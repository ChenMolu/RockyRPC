package com.rocky;

import com.rocky.config.ProtocolConfig;
import com.rocky.config.ReferenceConfig;
import com.rocky.config.RegistryConfig;
import com.rocky.config.ServiceConfig;
import com.rocky.discovery.Registry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RockyRPCBootstrap {

    // RockyRPCBootstrap 是个单例，我们希望每个应用程序只有一个实例
    private static RockyRPCBootstrap rockyRPCBootstrap = new RockyRPCBootstrap();
    private String appName = "default";
    private RegistryConfig registryConfig;
    private ProtocolConfig protocolConfig;
    private Registry registry;
    private static final int port = 8088;

    // 连接缓存，如果使用InetSocketAddress这样的类进行缓存，一定需要看它有没有重写equals和toString方法
    public final static Map<InetSocketAddress, Channel> CHANNEL_CACHE = new ConcurrentHashMap<>(16);

    // 维护已经发布并暴露的服务列表 key -> interface 的全限定名
    private final static Map<String, ServiceConfig<?>> SERVERS_LIST = new ConcurrentHashMap<>(16);

    // 定义全局的对外挂起的 completableFuture
    public final static Map<Long, CompletableFuture<Object>> PENDING_REQUEST = new ConcurrentHashMap<>(128);

    public RockyRPCBootstrap() {
        // 构造启动引导程序，需要做一些初始化的工作
    }

    public static RockyRPCBootstrap getInstance() {

        return rockyRPCBootstrap;
    }

    /**
     * 用来定义当前应用的名字
     *
     * @param appName
     * @return this 当前实例
     */
    public RockyRPCBootstrap application(String appName) {
        this.appName = appName;
        return this;
    }

    /**
     * 配置一个注册中心
     *
     * @return this当前实例
     */
    public RockyRPCBootstrap registry(RegistryConfig registryConfig) {
        this.registry = registryConfig.getRegistry();
        return this;
    }


    /**
     * 配置当前暴露的服务使用的协议
     *
     * @param protocolConfig
     * @return
     */
    public RockyRPCBootstrap protocol(ProtocolConfig protocolConfig) {
        this.protocolConfig = protocolConfig;
        if (log.isDebugEnabled()) {
            log.debug("当前工程使用了：{} 协议进行序列化", protocolConfig.toString());
        }
        return this;
    }

    /**
     * --------------------------------------服务提供方相关的API--------------------------------------
     */

    /**
     * 发布服务 将接口的实现，注册到服务中心
     *
     * @param service
     * @return this 当前实例
     */
    public RockyRPCBootstrap publish(ServiceConfig<?> service) {
        // 抽象了注册中心的概念，使用注册中心一个的实现完成注册
        registry.register(service);

        SERVERS_LIST.put(service.getInterfacer().getName(), service);
        return this;
    }

    /**
     * 批量发布
     *
     * @param service 封装的需要发布的服务集合
     * @return
     */
    public RockyRPCBootstrap publish(List<ServiceConfig> service) {
        return this;
    }

    /**
     * 启动Netty服务
     */
    public void start() {
        // Netty的Reactor线程池，初始化了一个NioEventLoop数组，用来处理I/O操作,如接受新的连接和读/写数据
        EventLoopGroup boss = new NioEventLoopGroup(2);
        EventLoopGroup worker = new NioEventLoopGroup(10);
        try {
            //用于启动NIO服务
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap = serverBootstrap.group(boss, worker)
                    // 通过工厂方法设计模式实例化一个channel
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        // ChannelInitializer 是一个特殊的处理类，他的目的是帮助使用者配置一个新的Channel,用于把许多自定义的处理类增加到pipline上来
                        @Override
                        // ChannelInitializer 是一个特殊的处理类，他的目的是帮助使用者配置一个新的 Channel。
                        public void initChannel(SocketChannel ch) throws Exception {
                            // 配置childHandler来通知一个关于消息处理的InfoServerHandler实例
                            ch.pipeline().addLast(new SimpleChannelInboundHandler<Object>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object message) throws Exception {
                                    ByteBuf byteBuf = (ByteBuf) message;
                                    log.info("byteBuf --> {}", byteBuf.toString(Charset.defaultCharset()));

                                    channelHandlerContext.writeAndFlush(Unpooled.copiedBuffer("hello rockyrpc".getBytes(StandardCharsets.UTF_8)));
                                }
                            });
                        }
                    });

            // 绑定服务器，该实例将提供有关IO操作的结果或状态的信息
            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();
//            System.out.println("在" + channelFuture.channel().localAddress() + "上开启监听");

            // 阻塞操作，closeFuture()开启了一个channel的监听器（这期间channel在进行各项工作），直到链路断开
            // closeFuture().sync()会阻塞当前线程，直到通道关闭操作完成。这可以用于确保在关闭通道之前，程序不会提前退出。
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                boss.shutdownGracefully().sync();
                worker.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * --------------------------------------服务调用方相关的API--------------------------------------
     */


    /**
     * @param reference
     * @return
     */
    public RockyRPCBootstrap reference(ReferenceConfig<?> reference) {

        // 在这个方法中我们是否能拿到相关的配置项-注册中心

        // 配置reference,将来调用get方法时，方便生成代理对象
        // 1、需要一个注册中心
        reference.setRegistry(registry);
        return this;
    }


}

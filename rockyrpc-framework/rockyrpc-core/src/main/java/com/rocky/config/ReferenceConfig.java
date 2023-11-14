package com.rocky.config;

import com.rocky.RockyRPCBootstrap;
import com.rocky.discovery.NettyBootStrapInitializer;
import com.rocky.discovery.Registry;
import com.rocky.exceptions.NetworkException;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


@Slf4j
public class ReferenceConfig<T> {

    private Class<T> interfaceRef;

    private Registry registry;

    public Class<T> getInterface() {
        return interfaceRef;
    }

    public void setInterface(Class<T> interfaceRef) {
        this.interfaceRef = interfaceRef;
    }

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public ReferenceConfig() {
    }

    public ReferenceConfig(Class<T> interfaceRef) {
        this.interfaceRef = interfaceRef;
    }

    /**
     * 代理设计模式，生成一个API接口的代理对象
     *
     * @return 返回一个代理对象
     */
    public T get() {
        // 此处一定是使用动态代理完成一些工作
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Class[] classes = new Class[]{interfaceRef};

        // 使用动态代理生成代理对象
        Object helloProxy = Proxy.newProxyInstance(classLoader, classes, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                //
                log.info("method -> {}", method);
//                log.info("proxy -> {}", proxy);
                log.info("args -> {}", args);
                // 1. 发现服务，从注册中心，寻找一个可用的服务
                // 传入服务名字，返回端口+ip
                InetSocketAddress address = registry.lookup(interfaceRef.getName());
                // 2. 使用netty连接服务器，发送 调用的 服务的名字+方法名字+参数列表，得到结果

                // 尝试从全局的缓存中获取一个通道
                Channel channel = RockyRPCBootstrap.CHANNEL_CACHE.get(address);
                if (channel == null) {
                    // await方法会阻塞，会等待连接成功再返回，netty还提供了异步处理的逻辑
                    // sync和await都是阻塞当前线程，获取返回值（连接的过程是异步的，发送数据的过程也是异步的）
                    // 如果发生了异常，sync会主动在主线程中抛出异常，await不会，异常在在子线程中处理需要future

                    // 同步方法：
//                    channel = NettyBootStrapInitializer.getBootstrap().connect(address).await().channel();

                    // 使用addListener执行的异步方式：
                    CompletableFuture<Channel> channelCompletableFuture = new CompletableFuture<>();
                    NettyBootStrapInitializer.getBootstrap().connect(address).addListener(
                            (ChannelFutureListener) promise -> {
                                if (promise.isDone()) {
                                    channelCompletableFuture.complete(promise.channel());
                                } else if (!promise.isSuccess()) {
                                    channelCompletableFuture.completeExceptionally(promise.cause());
                                }
                            });

                    channel = channelCompletableFuture.get(3, TimeUnit.SECONDS);

                    // 缓存channel
                    RockyRPCBootstrap.CHANNEL_CACHE.put(address, channel);
                }
                if (channel == null) {
                    throw new NetworkException("获取通道发生了异常");
                }

                // todo 封装报文

                CompletableFuture<Object> completeFuture = new CompletableFuture<>();
                channel.writeAndFlush(Unpooled.copiedBuffer("hello".getBytes())).addListener((ChannelFutureListener) promise -> {
//                    if (promise.isDone()) {
//                        completeFuture.complete(promise.getNow());
//                    } else
                    if (!promise.isSuccess()) {
                        completeFuture.completeExceptionally(promise.cause());
                    }
                });

//                return completeFuture.get(3, TimeUnit.SECONDS);
                return null;
            }
        });

        return (T) helloProxy;
    }
}

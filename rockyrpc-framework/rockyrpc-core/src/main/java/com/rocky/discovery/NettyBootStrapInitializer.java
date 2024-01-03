package com.rocky.discovery;

import com.rocky.RockyRPCBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;

/**
 * 提供bootStrap单例
 * TODO：这里会有什么问题？
 */
@Slf4j
public class NettyBootStrapInitializer {
    private static final Bootstrap bootstrap = new Bootstrap();

    static {
        NioEventLoopGroup group = new NioEventLoopGroup();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf msg) throws Exception {
                                log.info("msg-->{}", msg.toString(Charset.defaultCharset()));
                                // 服务提供方给予的结果
//                                String result = msg.toString(Charset.defaultCharset());
//
//                                // 从全局的挂起的请求中寻找与之匹配的待处理的cf
//                                CompletableFuture<Object> completableFuture = RockyRPCBootstrap.PENDING_REQUEST.get(1L);
//                                completableFuture.complete(result);
                            }
                        });
                    }
                });
    }


    private NettyBootStrapInitializer() {
    }

    public static Bootstrap getBootstrap() {
        return bootstrap;
    }
}

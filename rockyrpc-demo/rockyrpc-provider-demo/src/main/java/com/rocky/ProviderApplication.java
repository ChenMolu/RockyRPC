package com.rocky;

import com.rocky.config.ProtocolConfig;
import com.rocky.config.RegistryConfig;
import com.rocky.config.ServiceConfig;
import com.rocky.impl.HelloRpcImpl;

/**
 * Hello world!
 */
public class ProviderApplication {
    public static void main(String[] args) {
        // 服务提供方需要注册服务、启动服务
        // 1、封装要发布的服务
        ServiceConfig<HelloRpc> service = new ServiceConfig<>();
        service.setInterface(HelloRpc.class);
        service.setRef(new HelloRpcImpl());

        // 2、定义注册中心

        // 3、通过启动引导程序、启动服务提供方
        // （1） 配置 -- 应用的名称 -- 注册中心 -- 序列化协议 -- 压缩方式
        // （2） 发布服务
        RockyRPCBootstrap.getInstance()
                .application("first-RockyRPC-provider")
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                .protocol (new ProtocolConfig("jdk"))
                // 发布服务
                .publish(service)
                .start();


    }
}

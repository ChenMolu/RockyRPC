package com.rocky;

import com.rocky.config.ProtocolConfig;
import com.rocky.config.ReferenceConfig;
import com.rocky.config.RegistryConfig;
import com.rocky.config.ServiceConfig;
import com.rocky.discovery.Registry;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RockyRPCBootstrap {


    // RockyRPCBootstrap 是个单例，我们希望每个应用程序只有一个实例
    private static RockyRPCBootstrap rockyRPCBootstrap = new RockyRPCBootstrap();

    private String appName = "default";

    private RegistryConfig registryConfig;

    private ProtocolConfig protocolConfig;

    private Registry registry;

    // 维护已经发布并暴露的服务列表 key -> interface 的全限定名
    private static final Map<String, ServiceConfig<?>> SERVERS_LIST = new ConcurrentHashMap<>(16);

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
        try {
            Thread.sleep(1000000000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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

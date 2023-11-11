package com.rocky;

import com.rocky.config.ProtocolConfig;
import com.rocky.config.ReferenceConfig;
import com.rocky.config.RegistryConfig;
import com.rocky.config.ServiceConfig;
import com.rocky.utils.NetUtils;
import com.rocky.utils.zookeeper.ZookeeperNode;
import com.rocky.utils.zookeeper.ZookeeperUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;

import java.util.List;

@Slf4j
public class RockyRPCBootstrap {


    // RockyRPCBootstrap 是个单例，我们希望每个应用程序只有一个实例
    private static RockyRPCBootstrap rockyRPCBootstrap = new RockyRPCBootstrap();

    private String appName = "default";

    private RegistryConfig registryConfig;

    private ProtocolConfig protocolConfig;

    private int port = 8088;

    private ZooKeeper zooKeeper;

    public RockyRPCBootstrap() {
        // 构造启动引导程序，需要做一些初始化的工作
    }

    public static RockyRPCBootstrap getInstance() {

        return rockyRPCBootstrap;
    }

    /**
     * 用来定义房前应用的名字
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

        zooKeeper = ZookeeperUtils.createZookeeper();

        this.registryConfig = registryConfig;
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

        // 服务名称的节点
        String parentNode = Constant.BASE_PROVIDERS_PATH + "/" + service.getInterfacer().getName();

        //这个节点应该是一个临时节点
        if (!ZookeeperUtils.exists(zooKeeper, parentNode, null)) {
            ZookeeperNode zooKeeperNode = new ZookeeperNode(parentNode, null);
            ZookeeperUtils.createNode(zooKeeper, zooKeeperNode, null, CreateMode.PERSISTENT);
        }

        // 创建本机的临时节点，ip:port
        // 服务提供方的端口一般自己设定，我们还需要一个获取ip的方法
        String node = parentNode + "/" + NetUtils.getIp() + ":" + port;
        if (!ZookeeperUtils.exists(zooKeeper, node, null)) {
            ZookeeperNode zooKeeperNode = new ZookeeperNode(node, null);
            ZookeeperUtils.createNode(zooKeeper, zooKeeperNode, null, CreateMode.EPHEMERAL);
        }

        if (log.isDebugEnabled()) {
            log.debug("服务{}，已经被注册", service.toString());
        }
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
            Thread.sleep(10000);
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
        return this;
    }


}

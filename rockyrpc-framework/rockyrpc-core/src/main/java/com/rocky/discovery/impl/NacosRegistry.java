package com.rocky.discovery.impl;

import com.rocky.Constant;
import com.rocky.config.ServiceConfig;
import com.rocky.discovery.AbstractRegistry;
import com.rocky.utils.NetUtils;
import com.rocky.utils.zookeeper.ZookeeperNode;
import com.rocky.utils.zookeeper.ZookeeperUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;

import java.net.InetSocketAddress;

@Slf4j
public class NacosRegistry extends AbstractRegistry {

    private ZooKeeper zooKeeper;

    public NacosRegistry() {
        this.zooKeeper = ZookeeperUtils.createZookeeper();
    }

    public NacosRegistry(String connectString, int timeout) {
        this.zooKeeper = ZookeeperUtils.createZookeeper(connectString, timeout);
    }

    @Override
    public void register(ServiceConfig<?> service) {
        // 服务名称的节点
        String parentNode = Constant.BASE_PROVIDERS_PATH + "/" + service.getInterfacer().getName();

        //这个节点应该是一个临时节点
        if (!ZookeeperUtils.exists(zooKeeper, parentNode, null)) {
            ZookeeperNode zooKeeperNode = new ZookeeperNode(parentNode, null);
            ZookeeperUtils.createNode(zooKeeper, zooKeeperNode, null, CreateMode.PERSISTENT);
        }

        // 创建本机的临时节点，ip:port
        // 服务提供方的端口一般自己设定，我们还需要一个获取ip的方法

        // TODO 后续处理端口问题
        String node = parentNode + "/" + NetUtils.getIp() + ":" + 8088;
        if (!ZookeeperUtils.exists(zooKeeper, node, null)) {
            ZookeeperNode zooKeeperNode = new ZookeeperNode(node, null);
            ZookeeperUtils.createNode(zooKeeper, zooKeeperNode, null, CreateMode.EPHEMERAL);
        }

        if (log.isDebugEnabled()) {
            log.debug("服务{}，已经被注册", service.toString());
        }
    }

    @Override
    public InetSocketAddress lookup(String serviceName) {
        return null;
    }
}

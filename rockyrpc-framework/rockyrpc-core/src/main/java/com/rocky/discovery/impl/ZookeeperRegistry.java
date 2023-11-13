package com.rocky.discovery.impl;

import com.rocky.Constant;
import com.rocky.config.ServiceConfig;
import com.rocky.discovery.AbstractRegistry;
import com.rocky.exceptions.NetworkException;
import com.rocky.utils.NetUtils;
import com.rocky.utils.zookeeper.ZookeeperNode;
import com.rocky.utils.zookeeper.ZookeeperUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ZookeeperRegistry extends AbstractRegistry {

    private ZooKeeper zooKeeper;

    public ZookeeperRegistry() {
        this.zooKeeper = ZookeeperUtils.createZookeeper();
    }

    public ZookeeperRegistry(String connectString, int timeout) {
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
        // 1、找到服务对应的节点
        String serviceNode = Constant.BASE_PROVIDERS_PATH + "/" + serviceName;
        // 2、从zk中获取他的子节点
        List<String> children = ZookeeperUtils.getChildren(zooKeeper, serviceNode, null);
        // 获取了所有的可用服务列表
        List<InetSocketAddress> inetSocketAddresses = children.stream().map(ipString -> {
            String[] ipAndPort = ipString.split(":");
            String ip = ipAndPort[0];
            int port = Integer.valueOf(ipAndPort[1]);
            return new InetSocketAddress(ip, port);
        }).collect(Collectors.toList());

        if(inetSocketAddresses.size() == 0) {
            throw new NetworkException("未找到可用服务！");
        }
        // TODO ques: 我们每次调用相关方法的时候都需要去注册中心拉取服务列表嘛？ 本地缓存 + watcher
        //            我们如何合理的选择一个可用的服务，而不是只获取第一个
        return inetSocketAddresses.get(0);
    }
}

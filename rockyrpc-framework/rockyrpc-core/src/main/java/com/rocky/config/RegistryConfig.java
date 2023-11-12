package com.rocky.config;

import com.rocky.Constant;
import com.rocky.discovery.Register;
import com.rocky.discovery.impl.NacosRegistry;
import com.rocky.discovery.impl.ZookeeperRegistry;
import com.rocky.exceptions.DiscoveryException;

public class RegistryConfig {

    private final String connectString;

    public RegistryConfig(String connectString) {
        this.connectString = connectString;
    }

    /**
     * 使用简单工厂完成
     *
     * @return
     */
    public Register getRegister() {
        // 1、获取注册中心的类型
        String registerType = getRegisterType(connectString, true).toLowerCase().trim();
        if (registerType.equals("zookeeper")) {
            String host = getRegisterType(connectString, false);
            return new ZookeeperRegistry(host, Constant.TIME_OUT);
        }else if (registerType.equals("nacos")) {
            String host = getRegisterType(connectString, false);
            return new NacosRegistry(host, Constant.TIME_OUT);
        }
        throw new DiscoveryException("未发现合适的注册中心！");
    }

    private String getRegisterType(String connectString, boolean ifType) {
        String[] typeAndHost = connectString.split("://");
        if (typeAndHost.length != 2) {
            throw new RuntimeException("给定注册中心连接URL不合法");
        }
        if (ifType) {
            return typeAndHost[0];
        } else {
            return typeAndHost[1];
        }

    }
}

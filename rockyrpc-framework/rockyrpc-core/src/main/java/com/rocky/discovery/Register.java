package com.rocky.discovery;


import com.rocky.config.ServiceConfig;

/**
 * 注册中心
 */
public interface Register {

    /**
     * 注册服务
     * @param serviceConfig 服务的配置内容
     */
    public void register(ServiceConfig<?> serviceConfig);
}

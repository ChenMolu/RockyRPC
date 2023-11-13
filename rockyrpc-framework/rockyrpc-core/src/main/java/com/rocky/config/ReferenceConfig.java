package com.rocky.config;

import com.rocky.discovery.Registry;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;


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
                log.info("proxy -> {}", proxy);
                log.info("args -> {}", args);
                // 1. 发现服务，从注册中心，寻找一个可用的服务
                // 传入服务名字，返回端口+ip
                InetSocketAddress address = registry.lookup(interfaceRef.getName());
                // 2. 使用netty连接服务器，发送 调用的 服务的名字+方法名字+参数列表，得到结果
                if(log.isDebugEnabled()) {
                    log.debug("服务调用方，发现了服务【{}】的可用主机【{}】", interfaceRef.getName(), address);
                }
                return null;
            }
        });

        return (T) helloProxy;
    }
}

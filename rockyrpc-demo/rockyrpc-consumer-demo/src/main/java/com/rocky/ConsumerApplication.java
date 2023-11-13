package com.rocky;

import com.rocky.config.ReferenceConfig;
import com.rocky.config.RegistryConfig;

/**
 * Hello world!
 *
 */
public class ConsumerApplication
{
    public static void main( String[] args ) {
        // 想尽一切办法获取代理对象，使用ReferenceConfig进行封装
        // reference一定用生成代理的模版方法、get()
        ReferenceConfig<HelloRpc> reference = new ReferenceConfig<>();
        reference.setInterface(HelloRpc.class);

        // 代理做了些什么：
        // 1. 连接注册中心
        // 2。 拉取服务列表
        // 3. 选择一个服务并建立连接
        // 4. 发送请求，携带一些信息（接口名，参数列表，方法的名字），获得结果
        RockyRPCBootstrap.getInstance()
                .application("first-RockyRPC-consumer")
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                .reference(reference);

        // 获取一个代理对象
        HelloRpc service = reference.get();
        String message = service.sayHi("rockyrpc");
        System.out.println("Receive result ======> " + message);
    }
}

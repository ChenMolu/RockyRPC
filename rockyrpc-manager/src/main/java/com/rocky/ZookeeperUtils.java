//package com.rocky;
//
//import org.apache.zookeeper.ZooKeeper;
//
//import java.io.IOException;
//
//public class ZookeeperUtils {
//
//
//    public static ZooKeeper createZookeeper() {
//        ZooKeeper zooKeeper;
//        // 定义连接参数
//        String connectString = "8.134.113.47:2181";
//        // 定义超时时间
//        int timeout = 10000;
//        try {
//            // new MyWatcher() 默认的watcher
//            zooKeeper = new ZooKeeper(connectString,timeout,new MyWatcher());
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//}

package com.rocky.impl;

import com.rocky.HelloRpc;
import org.apache.commons.lang3.StringUtils;


public class HelloRpcImpl implements HelloRpc {
    @Override
    public String sayHi(String msg) {
        return "hi  consumer:" + msg;
    }
}

package com.netty.rpc.client.proxy;

import com.netty.rpc.client.handler.RpcFuture;

/**
 * Created by luxiaoxun on 2016/3/16.
 */
public interface RpcService {
    RpcFuture call(String funcName, Object... args);
}
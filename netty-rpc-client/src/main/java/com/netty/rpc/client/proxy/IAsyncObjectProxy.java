package com.netty.rpc.client.proxy;

import com.netty.rpc.client.handler.RPCFuture;

/**
 * Created by luxiaoxun on 2016/3/16.
 */
public interface IAsyncObjectProxy {
    RPCFuture call(String funcName, Object... args);
}
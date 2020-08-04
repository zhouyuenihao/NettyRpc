package com.netty.rpc.client.route.impl;

import com.google.common.hash.Hashing;
import com.netty.rpc.client.handler.RpcClientHandler;
import com.netty.rpc.client.route.RpcLoadBalance;
import com.netty.rpc.protocol.RpcProtocol;

import java.util.List;
import java.util.Map;

public class RpcLoadBalanceConsistentHash extends RpcLoadBalance {

    public RpcProtocol doRoute(String serviceName, List<RpcProtocol> addressList) {
        int index = Hashing.consistentHash(serviceName.hashCode(), addressList.size());
        return addressList.get(index);
    }

    @Override
    public RpcProtocol route(String serviceName, Map<RpcProtocol, RpcClientHandler> connectedServerNodes) throws Exception {
        Map<String, List<RpcProtocol>> serviceMap = getServiceMap(connectedServerNodes);
        List<RpcProtocol> addressList = serviceMap.get(serviceName);
        if (addressList != null && addressList.size() > 0) {
            return doRoute(serviceName, addressList);
        } else {
            throw new Exception("Can not find connection for service: " + serviceName);
        }
    }
}

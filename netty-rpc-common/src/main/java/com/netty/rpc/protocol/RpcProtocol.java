package com.netty.rpc.protocol;

import com.netty.rpc.util.JsonUtil;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class RpcProtocol implements Serializable {
    private static final long serialVersionUID = -1102180003395190700L;
    // service host
    private String host;
    // service port
    private int port;
    // service info list
    private List<RpcServiceProtocol> serviceProtocolList;

    public String toJson() {
        String json = JsonUtil.objectToJson(this);
        return json;
    }

    public static RpcProtocol fromJson(String json) {
        return JsonUtil.jsonToObject(json, RpcProtocol.class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RpcProtocol that = (RpcProtocol) o;
        return port == that.port &&
                Objects.equals(host, that.host) &&
                isListEquals(serviceProtocolList, that.getServiceProtocolList());
    }

    private boolean isListEquals(List<RpcServiceProtocol> thisList, List<RpcServiceProtocol> thatList) {
        if (thisList == null && thatList == null) {
            return true;
        }
        if ((thisList == null && thatList != null)
                || (thisList != null && thatList == null)
                || (thisList.size() != thatList.size())) {
            return false;
        }
        return thisList.containsAll(thatList) && thatList.containsAll(thisList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, serviceProtocolList.hashCode());
    }

    @Override
    public String toString() {
        return toJson();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public List<RpcServiceProtocol> getServiceProtocolList() {
        return serviceProtocolList;
    }

    public void setServiceProtocolList(List<RpcServiceProtocol> serviceProtocolList) {
        this.serviceProtocolList = serviceProtocolList;
    }
}

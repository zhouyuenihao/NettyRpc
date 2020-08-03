package com.netty.rpc.protocol;

import com.netty.rpc.util.JsonUtil;

import java.io.Serializable;
import java.util.Objects;

public class RpcProtocol implements Serializable {
    private static final long serialVersionUID = -1102180003395190700L;
    private String uuid;
    private String host;
    private int port;
    // interface name
    private String serviceName;

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
                uuid.equals(that.uuid) &&
                host.equals(that.host) &&
                serviceName.equals(that.serviceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, host, port, serviceName);
    }

    @Override
    public String toString() {
        return toJson();
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}

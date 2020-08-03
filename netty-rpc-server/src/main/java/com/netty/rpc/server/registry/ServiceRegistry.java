package com.netty.rpc.server.registry;

import cn.hutool.core.util.IdUtil;
import com.netty.rpc.config.Constant;
import com.netty.rpc.protocol.RpcProtocol;
import com.netty.rpc.zookeeper.CuratorClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 服务注册
 *
 * @author luxiaoxun
 */
public class ServiceRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);

    private CuratorClient curatorClient;
    private List<String> pathList = new ArrayList<>();

    public ServiceRegistry(String registryAddress) {
        this.curatorClient = new CuratorClient(registryAddress, 5000);
    }

    public void registerService(String host, int port, Map<String, Object> serviceMap) {
        //register service info, format uuid:ip:port
        if (serviceMap.size() > 0) {
            for (String key : serviceMap.keySet()) {
                try {
                    RpcProtocol rpcProtocol = new RpcProtocol();
                    //Add an uuid when register the service so we can distinguish the same ip:port service
                    String uuid = IdUtil.objectId();
                    rpcProtocol.setUuid(uuid);
                    rpcProtocol.setHost(host);
                    rpcProtocol.setPort(port);
                    rpcProtocol.setServiceName(key);
                    String serviceData = rpcProtocol.toJson();
                    byte[] bytes = serviceData.getBytes();
                    String path = Constant.ZK_DATA_PATH + "-" + uuid;
                    this.curatorClient.createPathData(path, bytes);
                    pathList.add(path);
                    logger.info("Registry new service:{}, host:{}, port:{}", key, host, port);
                } catch (Exception e) {
                    logger.error("Register service {} fail, exception:{}", key, e.getMessage());
                }
            }
        }
    }

    public void unregisterService() {
        logger.info("Unregister all service");
        for (String path : pathList) {
            try {
                this.curatorClient.deletePath(path);
            } catch (Exception ex) {
                logger.error("Delete service path error: " + ex.getMessage());
            }
        }
        this.curatorClient.close();
    }
}
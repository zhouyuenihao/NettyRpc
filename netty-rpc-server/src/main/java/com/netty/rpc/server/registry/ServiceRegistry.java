package com.netty.rpc.server.registry;

import com.netty.rpc.config.Constant;
import com.netty.rpc.protocol.RpcProtocol;
import com.netty.rpc.protocol.RpcServiceProtocol;
import com.netty.rpc.util.ServiceUtil;
import com.netty.rpc.zookeeper.CuratorClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
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
        // Register service info
        List<RpcServiceProtocol> serviceProtocolList = new ArrayList<>();
        for (String key : serviceMap.keySet()) {
            String[] serviceInfo = key.split(ServiceUtil.SERVICE_CONCAT_TOKEN);
            if (serviceInfo.length > 0) {
                RpcServiceProtocol rpcServiceProtocol = new RpcServiceProtocol();
                rpcServiceProtocol.setServiceName(serviceInfo[0]);
                if (serviceInfo.length == 2) {
                    rpcServiceProtocol.setVersion(serviceInfo[1]);
                } else {
                    rpcServiceProtocol.setVersion("");
                }
                logger.info("Register new service: {} ", key);
                serviceProtocolList.add(rpcServiceProtocol);
            } else {
                logger.warn("Can not get service name and version: {} ", key);
            }
        }
        try {
            RpcProtocol rpcProtocol = new RpcProtocol();
            rpcProtocol.setHost(host);
            rpcProtocol.setPort(port);
            rpcProtocol.setServiceProtocolList(serviceProtocolList);
            String serviceData = rpcProtocol.toJson();
            byte[] bytes = serviceData.getBytes();
            String path = Constant.ZK_DATA_PATH + "-" + rpcProtocol.hashCode();
            this.curatorClient.createPathData(path, bytes);
            pathList.add(path);
            logger.info("Register {} new service, host: {}, port: {}", serviceProtocolList.size(), host, port);
        } catch (Exception e) {
            logger.error("Register service fail, exception: {}", e.getMessage());
        }

        curatorClient.addConnectionStateListener(new ConnectionStateListener() {
            @Override
            public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
                if (connectionState == ConnectionState.RECONNECTED) {
                    logger.info("Connection state: {}, register service after reconnected", connectionState);
                    registerService(host, port, serviceMap);
                }
            }
        });
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
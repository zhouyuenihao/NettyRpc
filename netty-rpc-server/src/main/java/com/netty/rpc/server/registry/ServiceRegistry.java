package com.netty.rpc.server.registry;

import cn.hutool.core.util.IdUtil;
import com.netty.rpc.config.Constant;
import com.netty.rpc.zookeeper.CuratorClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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

    public void registerService(String data) {
        //register service info, format uuid:ip:port
        if (data != null) {
            //Add an uuid when register the service so we can distinguish the same ip:port service
            String uuid = IdUtil.objectId();
            String serviceData = uuid + ":" + data;
            byte[] bytes = serviceData.getBytes();
            try {
                String path = Constant.ZK_DATA_PATH + "-" + uuid;
                this.curatorClient.createPathData(path, bytes);
                pathList.add(path);
                logger.info("Registry new service: " + data);
            } catch (Exception e) {
                logger.error("Register service {} fail, exception: {}", data, e.getMessage());
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
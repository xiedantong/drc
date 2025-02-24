package com.ctrip.framework.drc.console.task;

import com.ctrip.framework.drc.console.enums.ActionEnum;
import com.ctrip.framework.drc.console.monitor.AbstractLeaderAwareMonitor;
import com.ctrip.framework.drc.console.pojo.MetaKey;
import com.ctrip.framework.drc.core.driver.command.netty.endpoint.MySqlEndpoint;
import com.ctrip.framework.drc.core.server.observer.endpoint.MasterMySQLEndpointObservable;
import com.ctrip.framework.drc.core.server.observer.endpoint.MasterMySQLEndpointObserver;
import com.ctrip.xpipe.api.endpoint.Endpoint;
import com.ctrip.xpipe.api.observer.Observable;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.springframework.util.CollectionUtils;
import org.unidal.tuple.Triple;


import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author: hbshen 
 * @Date: 2021/4/26
 */
public abstract class AbstractMasterMySQLEndpointObserver extends AbstractLeaderAwareMonitor implements MasterMySQLEndpointObserver {

    protected Map<MetaKey, MySqlEndpoint> masterMySQLEndpointMap = Maps.newConcurrentMap();
    protected Set<String> mhasRelated = Sets.newConcurrentHashSet();
    
    protected String regionName;
    
    protected Set<String> dcsInRegion;
    
    protected String localDcName; 
    
    protected boolean onlyCarePart;

    @Override
    public void initialize() {
        super.initialize();
        setObservationRange();
    }

    @Override
    public void update(Object args, Observable observable) {
        if (observable instanceof MasterMySQLEndpointObservable) {
            Triple<MetaKey, MySqlEndpoint, ActionEnum> message = (Triple<MetaKey, MySqlEndpoint, ActionEnum>) args;
            MetaKey metaKey = message.getFirst();
            MySqlEndpoint masterMySQLEndpoint = message.getMiddle();
            ActionEnum action = message.getLast();

            if (onlyCarePart && !isCare(metaKey)) {
                logger.info("[OBSERVE][dc={}] {} not interested in {}({})", 
                        getClass().getName(), localDcName, metaKey, masterMySQLEndpoint.getSocketAddress());
                return;
            }
            if(ActionEnum.ADD.equals(action) || ActionEnum.UPDATE.equals(action)) {
                logger.info("[OBSERVE][{}] {} {}({})", getClass().getName(), action.name(), metaKey, masterMySQLEndpoint.getSocketAddress());
                MySqlEndpoint oldEndpoint = masterMySQLEndpointMap.get(metaKey);
                if (oldEndpoint != null) {
                    logger.info("[OBSERVE][{}] {} clear old {}({})", getClass().getName(), action.name(), metaKey, oldEndpoint.getSocketAddress());
                    clearOldEndpointResource(oldEndpoint);
                }
                masterMySQLEndpointMap.put(metaKey, masterMySQLEndpoint);
                refreshMhasRelated();
            } else if (ActionEnum.DELETE.equals(action)) {
                logger.info("[OBSERVE][{}] {} {}", getClass().getName(), action.name(), metaKey);
                MySqlEndpoint oldEndpoint = masterMySQLEndpointMap.remove(metaKey);
                if (oldEndpoint != null) {
                    logger.info("[OBSERVE][{}] {} clear old {}({})", getClass().getName(), action.name(), metaKey, oldEndpoint.getSocketAddress());
                    clearOldEndpointResource(oldEndpoint);
                }
                refreshMhasRelated();
            }
        }
    }
    
    public Map<MetaKey, MySqlEndpoint> getMasterMySQLEndpointMap() {
        return masterMySQLEndpointMap;
    }

    public void setMasterMySQLEndpointMap(Map<MetaKey, MySqlEndpoint> masterMySQLEndpointMap) {
        this.masterMySQLEndpointMap = masterMySQLEndpointMap;
    }
    
    public Set<String> getMhasRelated() {
        if (CollectionUtils.isEmpty(mhasRelated)) {
            refreshMhasRelated();
        }
        return mhasRelated;
    }
    
    private void refreshMhasRelated() {
        mhasRelated = masterMySQLEndpointMap.keySet().stream().map(MetaKey::getMhaName).collect(Collectors.toSet());
    }

    public void setLocalDcName(String localDcName) {
        this.localDcName = localDcName;
    }

    public abstract void clearOldEndpointResource(Endpoint endpoint);

    public abstract void setLocalDcName();
    
    public abstract void setLocalRegionInfo();
    
    public abstract void setOnlyCarePart();
    
    public abstract boolean isCare(MetaKey metaKey);
    
    private void setObservationRange() {
        setOnlyCarePart();
        setLocalDcName();
        setLocalRegionInfo();
    }
    
}

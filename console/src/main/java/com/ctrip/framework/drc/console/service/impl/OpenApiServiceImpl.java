package com.ctrip.framework.drc.console.service.impl;


import com.ctrip.framework.drc.console.monitor.delay.config.v2.MetaProviderV2;
import com.ctrip.framework.drc.console.service.OpenApiService;
import com.ctrip.framework.drc.console.vo.api.DrcDbInfo;
import com.ctrip.framework.drc.console.vo.api.MessengerInfo;
import com.ctrip.framework.drc.core.entity.*;
import com.ctrip.framework.drc.core.meta.ColumnsFilterConfig;
import com.ctrip.framework.drc.core.meta.DataMediaConfig;
import com.ctrip.framework.drc.core.meta.RowsFilterConfig;
import com.ctrip.framework.drc.core.server.common.filter.table.aviator.AviatorRegexFilter;
import com.ctrip.xpipe.codec.JsonCodec;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static com.ctrip.framework.drc.core.service.utils.Constants.ESCAPE_CHARACTER_DOT_REGEX;

/**
 * @ClassName OpenApiServiceImpl
 * @Author haodongPan
 * @Date 2022/1/6 20:26
 * @Version: $
 */
@Service
public class OpenApiServiceImpl implements OpenApiService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private MetaProviderV2 metaProviderV2;

    @Override
    public List<MessengerInfo> getAllMessengersInfo() throws SQLException {
        List<MessengerInfo> res = Lists.newArrayList();
        Map<String, Dc> dcs = metaProviderV2.getDrc().getDcs();
        for (Dc dc : dcs.values()) {
            Map<String, DbCluster> dbClusters = dc.getDbClusters();
            for (DbCluster dbCluster : dbClusters.values()) {
                List<Messenger> messengers = dbCluster.getMessengers();
                if (!CollectionUtils.isEmpty(messengers)) {
                    for (Messenger messenger : messengers) {
                        MessengerInfo mInfo = new MessengerInfo();
                        mInfo.setMhaName(dbCluster.getMhaName());
                        mInfo.setNameFilter(messenger.getNameFilter());
                        res.add(mInfo);
                        break;
                    }
                }
            }
        }
        return res;
    }


    @Override
    public List<DrcDbInfo> getDrcDbInfos(String dbName) {
        List<DrcDbInfo> res = Lists.newArrayList();
        Drc drc = metaProviderV2.getDrc();
        for (Entry<String, Dc> dcInfo : drc.getDcs().entrySet()) {
            Dc dcMeta = dcInfo.getValue();
            String destRegion = dcMeta.getRegion();

            for (Entry<String, DbCluster> dbClusterInfo : dcMeta.getDbClusters().entrySet()) {
                DbCluster dbClusterMeta = dbClusterInfo.getValue();
                String destMha = dbClusterMeta.getMhaName();

                Set<String> srcMhaNames = Sets.newHashSet();
                List<Applier> appliers = dbClusterMeta.getAppliers();
                for (Applier applier : appliers) {
                    try {
                        String srcRegion = applier.getTargetRegion();
                        String srcMha = applier.getTargetMhaName();
                        if (srcMhaNames.contains(srcMha)) {
                            continue;
                        } else {
                            srcMhaNames.add(srcMha);
                        }

                        String nameFilter = applier.getNameFilter();
                        if (StringUtils.isNotBlank(nameFilter)) {
                            // generate by drcDbInfo by db
                            Map<String, DrcDbInfo> dbInfoMap = Maps.newHashMap();
                            for (String fullTableName : nameFilter.split(",")) {
                                String[] split = fullTableName.split(ESCAPE_CHARACTER_DOT_REGEX);
                                String dbRegex = split[0];
                                String tableRegex = split[1];
                                if ("drcmonitordb".equalsIgnoreCase(dbRegex)) {
                                    continue;
                                }
                                if (StringUtils.isNotBlank(dbName) && !new AviatorRegexFilter(dbRegex).filter(dbName)) {
                                    continue;
                                }
                                if (dbInfoMap.containsKey(dbRegex)) {
                                    DrcDbInfo drcDbInfo = dbInfoMap.get(dbRegex);
                                    drcDbInfo.addRegexTable(tableRegex);
                                } else {
                                    DrcDbInfo drcDbInfo = new DrcDbInfo(dbRegex, tableRegex, srcMha, destMha, srcRegion, destRegion);
                                    dbInfoMap.put(dbRegex, drcDbInfo);
                                    res.add(drcDbInfo);
                                }
                            }

                            processProperties(applier, dbInfoMap, destMha);
                        } else {
                            // all db
                            DrcDbInfo drcDbInfo = new DrcDbInfo(".*", ".*", srcMha, destMha, srcRegion, destRegion);
                            res.add(drcDbInfo);

                            processProperties(applier, drcDbInfo);
                        }
                    } catch (Exception e) {
                        logger.warn("getDrcDbInfos fail in applier which destMha is :{}", destMha, e);
                    }
                }
            }
        }
        return res;
    }

    private void processProperties(Applier applier, Map<String, DrcDbInfo> dbInfoMap, String destMha) {
        if (StringUtils.isNotBlank(applier.getProperties())) {
            String properties = applier.getProperties();
            DataMediaConfig dataMediaConfig = JsonCodec.INSTANCE.decode(properties, DataMediaConfig.class);

            List<RowsFilterConfig> rowsFilters = dataMediaConfig.getRowsFilters();
            if (!CollectionUtils.isEmpty(rowsFilters)) {
                for (RowsFilterConfig rowsFilter : rowsFilters) {
                    String fullTableName = rowsFilter.getTables();
                    String[] split = fullTableName.split(ESCAPE_CHARACTER_DOT_REGEX);
                    String db = split[0];
                    if (dbInfoMap.containsKey(db)) {
                        DrcDbInfo drcDbInfo = dbInfoMap.get(db);
                        drcDbInfo.addRowsFilterConfig(rowsFilter);
                    } else {
                        logger.warn("no db found in nameFilter,destMha:{},rowsFilter:{}", destMha, rowsFilter);
                    }
                }
            }

            List<ColumnsFilterConfig> columnsFilters = dataMediaConfig.getColumnsFilters();
            if (!CollectionUtils.isEmpty(columnsFilters)) {
                for (ColumnsFilterConfig columnsFilter : columnsFilters) {
                    String fullTableName = columnsFilter.getTables();
                    String[] split = fullTableName.split(ESCAPE_CHARACTER_DOT_REGEX);
                    String db = split[0];
                    if (dbInfoMap.containsKey(db)) {
                        DrcDbInfo drcDbInfo = dbInfoMap.get(db);
                        drcDbInfo.addColumnFilterConfig(columnsFilter);
                    } else {
                        logger.warn("no db found in nameFilter,destMha:{},columnsFilter:{}", destMha, columnsFilter);
                    }
                }
            }
        }

    }

    private void processProperties(Applier applier, DrcDbInfo drcDbInfo) {
        if (StringUtils.isNotBlank(applier.getProperties())) {
            String properties = applier.getProperties();
            DataMediaConfig dataMediaConfig = JsonCodec.INSTANCE.decode(properties, DataMediaConfig.class);

            List<RowsFilterConfig> rowsFilters = dataMediaConfig.getRowsFilters();
            if (!CollectionUtils.isEmpty(rowsFilters)) {
                for (RowsFilterConfig rowsFilter : rowsFilters) {
                    drcDbInfo.addRowsFilterConfig(rowsFilter);
                }
            }

            List<ColumnsFilterConfig> columnsFilters = dataMediaConfig.getColumnsFilters();
            if (!CollectionUtils.isEmpty(columnsFilters)) {
                for (ColumnsFilterConfig columnsFilter : columnsFilters) {
                    drcDbInfo.addColumnFilterConfig(columnsFilter);
                }
            }
        }

    }


}

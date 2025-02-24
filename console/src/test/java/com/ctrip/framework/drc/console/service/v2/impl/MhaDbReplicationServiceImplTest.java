package com.ctrip.framework.drc.console.service.v2.impl;

import com.ctrip.framework.drc.console.dao.entity.DbTbl;
import com.ctrip.framework.drc.console.dao.entity.v2.DbReplicationTbl;
import com.ctrip.framework.drc.console.dao.entity.v2.MhaDbMappingTbl;
import com.ctrip.framework.drc.console.dao.entity.v2.MhaTblV2;
import com.ctrip.framework.drc.console.dao.entity.v3.MhaDbReplicationTbl;
import com.ctrip.framework.drc.console.dto.v3.MhaDbReplicationDto;
import com.ctrip.framework.drc.console.enums.ReplicationTypeEnum;
import com.ctrip.framework.drc.console.exception.ConsoleException;
import com.ctrip.xpipe.tuple.Pair;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.ctrip.framework.drc.console.service.v2.impl.MessengerServiceV2ImplTest.VPC_MHA_NAME;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class MhaDbReplicationServiceImplTest extends CommonDataInit {

    @Before
    public void setUp() throws IOException, SQLException {
        MockitoAnnotations.openMocks(this);
        when(defaultConsoleConfig.getVpcMhaNames()).thenReturn(Lists.newArrayList(VPC_MHA_NAME));
        super.setUp();
    }

    @Test
    public void testQueryDbReplication() {
        List<MhaDbReplicationDto> replicationDtos = mhaDbReplicationService.queryByMha("mha1", "mha2", null);
        Assert.assertFalse(CollectionUtils.isEmpty(replicationDtos));
        Assert.assertEquals("mha1", replicationDtos.get(0).getSrc().getMhaName());
        Assert.assertEquals("mha2", replicationDtos.get(0).getDst().getMhaName());
        Assert.assertFalse(StringUtils.isBlank(replicationDtos.get(0).getDst().getDbName()));
        Assert.assertNotNull(replicationDtos.get(0).getDst().getMhaDbMappingId());
    }

    @Test
    public void testQueryMqReplication() {
        List<MhaDbReplicationDto> replicationDtos = mhaDbReplicationService.queryMqByMha("mha1", null);
        Assert.assertEquals(1, replicationDtos.size());
        Assert.assertEquals("mha1", replicationDtos.get(0).getSrc().getMhaName());
        Assert.assertEquals(MhaDbReplicationDto.MQ_DTO, replicationDtos.get(0).getDst());
    }

    @Test
    public void testQueryByDcName() {
        List<MhaDbReplicationDto> replicationDtos = mhaDbReplicationService.queryByDcName("shaoy", null);
        Assert.assertTrue(replicationDtos.stream().anyMatch(e -> ReplicationTypeEnum.DB_TO_DB.getType().equals(e.getReplicationType()) && Boolean.TRUE.equals(e.getDrcStatus())));
        Assert.assertTrue(replicationDtos.stream().anyMatch(e -> ReplicationTypeEnum.DB_TO_MQ.getType().equals(e.getReplicationType()) && Boolean.TRUE.equals(e.getDrcStatus())));
        Assert.assertTrue(replicationDtos.stream().anyMatch(e -> ReplicationTypeEnum.DB_TO_MQ.getType().equals(e.getReplicationType()) && !Boolean.TRUE.equals(e.getDrcStatus())));
        replicationDtos.forEach(System.out::println);

        replicationDtos = mhaDbReplicationService.queryByDcName("sinaws", null);
        replicationDtos.forEach(System.out::println);
    }

    @Test
    public void testDbReplication() throws SQLException {
        List<DbReplicationTbl> dbReplicationTbls = new ArrayList<>();

        // 1. no need to maintain
        dbReplicationTbls.add(getDbReplicationTbl(1L, 3L, 0));
        dbReplicationTbls.add(getDbReplicationTbl(2L, 4L, 0));

        Pair<List<MhaDbReplicationTbl>, List<MhaDbReplicationTbl>> insertsAndUpdates = mhaDbReplicationService.getInsertsAndUpdates(dbReplicationTbls);
        List<MhaDbReplicationTbl> inserts = insertsAndUpdates.getKey();
        List<MhaDbReplicationTbl> updates = insertsAndUpdates.getValue();
        Assert.assertEquals(0, inserts.size());
        Assert.assertEquals(updates.size(), 0);


        // 2. update and inserts
        // insert
        dbReplicationTbls.add(getDbReplicationTbl(3L, 6L, 0));
        // update
        dbReplicationTbls.add(getDbReplicationTbl(1L, -1L, 1));
        dbReplicationTbls.add(getDbReplicationTbl(2L, 5L, 0));
        // exist, do nothing
        dbReplicationTbls.add(getDbReplicationTbl(2L, -1L, 1));


        insertsAndUpdates = mhaDbReplicationService.getInsertsAndUpdates(dbReplicationTbls);
        inserts = insertsAndUpdates.getKey();
        updates = insertsAndUpdates.getValue();

        Assert.assertEquals(1, inserts.size());
        Assert.assertEquals(2, updates.size());
    }

    private static DbReplicationTbl getDbReplicationTbl(Long srcId, Long dstId, int type) {
        DbReplicationTbl replication1 = new DbReplicationTbl();
        replication1.setSrcMhaDbMappingId(srcId);
        replication1.setDstMhaDbMappingId(dstId);
        replication1.setReplicationType(type);
        return replication1;
    }

    @Test
    public void testGetReplicationRelatedMha() throws SQLException {
        List<MhaTblV2> replicationRelatedMha = mhaDbReplicationService.getReplicationRelatedMha("db1", "table1");
        Assert.assertEquals(2, replicationRelatedMha.size());
        replicationRelatedMha = mhaDbReplicationService.getReplicationRelatedMha("db1", "table3");
        Assert.assertEquals(0, replicationRelatedMha.size());
        replicationRelatedMha = mhaDbReplicationService.getReplicationRelatedMha("db4", "table1");
        Assert.assertEquals(0, replicationRelatedMha.size());
    }
}


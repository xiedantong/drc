package com.ctrip.framework.drc.console.service.v2.impl;

import static com.ctrip.framework.drc.console.utils.UTConstants.XML_FILE_META;

import com.ctrip.framework.drc.console.config.DefaultConsoleConfig;
import com.ctrip.framework.drc.console.monitor.delay.config.v2.MetaProviderV2;
import com.ctrip.framework.drc.console.pojo.MetaKey;
import com.ctrip.framework.drc.console.pojo.MonitorMetaInfo;
import com.ctrip.framework.drc.console.pojo.ReplicatorWrapper;
import com.ctrip.framework.drc.console.service.v2.MonitorServiceV2;
import com.ctrip.framework.drc.core.driver.command.netty.endpoint.MySqlEndpoint;
import com.ctrip.framework.drc.core.entity.Drc;
import com.ctrip.framework.drc.core.transform.DefaultSaxParser;
import com.ctrip.xpipe.api.endpoint.Endpoint;
import com.ctrip.xpipe.utils.FileUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.util.ClassUtils;


public class CacheMetaServiceImplTest {

    @InjectMocks private CacheMetaServiceImpl cacheMetaService;

    @Mock private MetaProviderV2 metaProviderV2;

    @Mock private DefaultConsoleConfig consoleConfig;

    @Mock private MonitorServiceV2 monitorServiceV2;
    

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        String file = ClassUtils.getDefaultClassLoader().getResource(XML_FILE_META).getPath();
        InputStream ins = FileUtils.getFileInputStream(file);
        Drc expectedDrc = DefaultSaxParser.parse(ins);
        String expectedDrcString = expectedDrc.toString();
        Mockito.when(metaProviderV2.getDrc()).thenReturn(expectedDrc);
    }

    @Test
    public void testGetAllReplicatorsInLocalRegion() {
        Mockito.when(consoleConfig.getDcsInLocalRegion()).thenReturn(new HashSet<>(){{
            add("dc1");
            add("dc2");
        }});

        Map<String, List<ReplicatorWrapper>> allReplicators = cacheMetaService.getAllReplicatorsInLocalRegion();
        Assert.assertEquals(6,allReplicators.size());
    }

    @Test
    public void testGetMasterReplicatorsToBeMonitored() {
        Mockito.when(consoleConfig.getDcsInLocalRegion()).thenReturn(new HashSet<>(){{
            add("dc1");
            add("dc2");
        }});
        List<String> mhas = Lists.newArrayList("mha1dc1","mha2dc1","mha3dc1","mha1dc2","mha2dc2","mha3dc2","mha3dc3");
        Map<String, ReplicatorWrapper> masterReplicatorsToBeMonitored = cacheMetaService.getMasterReplicatorsToBeMonitored(
                mhas);
        Assert.assertEquals(7,masterReplicatorsToBeMonitored.size());
        
        
        Mockito.when(consoleConfig.getDcsInLocalRegion()).thenReturn(new HashSet<>(){{
            add("dc2");
            add("dc3");
        }});
        masterReplicatorsToBeMonitored = cacheMetaService.getMasterReplicatorsToBeMonitored(mhas);
        Assert.assertEquals(5,masterReplicatorsToBeMonitored.size());
        
        
        Mockito.when(consoleConfig.getDcsInLocalRegion()).thenReturn(new HashSet<>(){{
            add("dc1");
            add("dc3");
        }});
        masterReplicatorsToBeMonitored = cacheMetaService.getMasterReplicatorsToBeMonitored(mhas);
        
    }

    @Test
    public void testGetMha2UuidsMap() {
        Set<String> dcs = Sets.newHashSet("dc1", "dc2");
        Map<String, Set<String>> mha2UuidsMap = cacheMetaService.getMha2UuidsMap(dcs);
        Assert.assertEquals(6,mha2UuidsMap.size());
        Assert.assertEquals(2,mha2UuidsMap.get("mha1dc1").size());
        Assert.assertEquals(2,mha2UuidsMap.get("mha2dc1").size());
        Assert.assertEquals(2,mha2UuidsMap.get("mha3dc1").size());
        Assert.assertEquals(2,mha2UuidsMap.get("mha1dc2").size());
        Assert.assertEquals(2,mha2UuidsMap.get("mha2dc2").size());
        Assert.assertEquals(4,mha2UuidsMap.get("mha3dc2").size());
    }

    @Test
    public void testGetMonitorMetaInfo() throws SQLException {
        List<String> mhas = Lists.newArrayList("mha1dc1","mha2dc1","mha3dc1","mha1dc2","mha2dc2","mha3dc2","mha3dc3");
        Mockito.when(monitorServiceV2.getMhaNamesToBeMonitored()).thenReturn(mhas);

        MonitorMetaInfo monitorMetaInfo = cacheMetaService.getMonitorMetaInfo();
        Map<MetaKey, MySqlEndpoint> masterMySQLEndpoint = monitorMetaInfo.getMasterMySQLEndpoint();
        Map<MetaKey, MySqlEndpoint> slaveMySQLEndpoint = monitorMetaInfo.getSlaveMySQLEndpoint();
        Map<MetaKey, Endpoint> masterReplicatorEndpoint = monitorMetaInfo.getMasterReplicatorEndpoint();
        Assert.assertEquals(7,masterMySQLEndpoint.size());
        Assert.assertEquals(7,slaveMySQLEndpoint.size());
        Assert.assertEquals(7,masterReplicatorEndpoint.size());

    }
}
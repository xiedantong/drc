package com.ctrip.framework.drc.applier.mq;

import com.ctrip.framework.drc.core.driver.binlog.gtid.GtidSet;
import com.ctrip.framework.drc.core.driver.binlog.manager.task.NamedCallable;
import com.ctrip.framework.drc.core.driver.binlog.manager.task.RetryTask;
import com.ctrip.framework.drc.core.server.utils.ThreadUtils;
import com.ctrip.framework.drc.core.utils.SpringUtils;
import com.ctrip.framework.drc.fetcher.resource.context.MqPosition;
import com.ctrip.framework.drc.fetcher.system.AbstractResource;
import com.ctrip.framework.drc.fetcher.system.InstanceConfig;
import com.ctrip.xpipe.utils.VisibleForTesting;
import com.ctrip.xpipe.zk.ZkClient;
import com.ctrip.xpipe.zk.impl.SpringZkClient;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.io.*;
import java.util.concurrent.*;

import static com.ctrip.framework.drc.core.driver.config.GlobalConfig.APPLIER_POSITIONS_PATH;
import static com.ctrip.framework.drc.core.server.config.SystemConfig.APPLIER_PATH;
import static com.ctrip.framework.drc.core.server.config.SystemConfig.isIntegrityTest;

/**
 * Created by jixinwang on 2022/10/19
 */
public class MqPositionResource extends AbstractResource implements MqPosition {

    private static final Logger loggerMsg = LoggerFactory.getLogger("MESSENGER");

    private static final int RETRY_TIME = 3;

    private static final int PERSIST_POSITION_PERIOD_TIME = 30;

    @InstanceConfig(path = "gtidExecuted")
    public String initialGtidExecuted = "";

    @InstanceConfig(path = "registryKey")
    public String registryKey;

    private GtidSet executedGtidSet = new GtidSet(StringUtils.EMPTY);

    private ZkClient zkClient;

    private File positionFile;

    private String zkPositionPath;

    private ExecutorService gtidService = ThreadUtils.newSingleThreadExecutor("MQ-Update-Position");

    private ScheduledExecutorService scheduledExecutorService = ThreadUtils.newSingleThreadScheduledExecutor("MQ-Persist-Position-Task");

    @Override
    protected void doInitialize() throws Exception {
        initZkClient();
        String filePositionPath = APPLIER_PATH + registryKey;
        positionFile = new File(filePositionPath);
        zkPositionPath = APPLIER_POSITIONS_PATH + "/" + registryKey;
        executedGtidSet = new GtidSet(get()).union(new GtidSet(initialGtidExecuted));
        updatePositionInFile();
        startUpdatePositionSchedule();
    }

    @Override
    public void add(String gtid) {
        gtidService.submit(() -> {
            executedGtidSet.add(gtid);
        });
    }

    @Override
    public void union(GtidSet gtidSet) {
        gtidService.submit(() -> {
            executedGtidSet = executedGtidSet.union(gtidSet);
        });
    }

    @Override
    public String get() {
        if (isIntegrityTest()) {
            return StringUtils.EMPTY;
        }
        GtidSet gtidSetFromZk = new GtidSet(getPositionFromZk());
        GtidSet gtidSetFromFile = new GtidSet(getPositionFromFile());
        return gtidSetFromZk.union(gtidSetFromFile).toString();
    }

    private void startUpdatePositionSchedule() {
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                persistPosition();
                loggerMsg.info("[MQ][{}] persist position schedule success", registryKey);
            } catch (Throwable t) {
                loggerMsg.error("[MQ][{}] persist position schedule error", registryKey, t);
            }
        }, PERSIST_POSITION_PERIOD_TIME, PERSIST_POSITION_PERIOD_TIME, TimeUnit.SECONDS);
    }

    private void persistPosition() {
        if (isIntegrityTest()) {
            return;
        }

        Boolean res = new RetryTask<>(new ZkPositionUpdateTask(), RETRY_TIME).call();
        if (res == null) {
            loggerMsg.error("[MQ][{}] persist position in zk error", registryKey);
            try {
                updatePositionInFile();
            } catch (IOException e) {
                loggerMsg.error("[MQ][{}] persist position error", registryKey);
            }
        }
    }

    @VisibleForTesting
    protected String getCurrentPosition() {
        return executedGtidSet.toString();
    }

    private String getPositionFromZk() {
        CuratorFramework curatorFramework = zkClient.get();
        try {
            if (!checkPath(curatorFramework, zkPositionPath)) {
                return StringUtils.EMPTY;
            }
            byte[] gtidSetByte = curatorFramework.getData().forPath(zkPositionPath);
            return new String(gtidSetByte);
        } catch (Exception e) {
            loggerMsg.error("[MQ][{}] get position from zk error", registryKey, e);
        }
        return StringUtils.EMPTY;
    }

    private String getPositionFromFile() {
        if (positionFile.exists()) {
            try {
                return FileUtils.readFileToString(positionFile);
            } catch (IOException e) {
                loggerMsg.error("[MQ][{}] get position from file error", registryKey, e);
            }
        }
        return StringUtils.EMPTY;
    }

    private void updatePositionInFile() throws IOException {
        String currentPosition = getCurrentPosition();
        loggerMsg.info("[MQ][{}] persist mq position to file with position: {}", registryKey, currentPosition);
        if (positionFile.exists()) {
            FileUtils.writeStringToFile(positionFile, currentPosition);
        } else {
            loggerMsg.error("[MQ][{}] persist mq position to file error, file: {} not exists, with position: {}", registryKey, positionFile.getPath(), currentPosition);
        }
    }

    private boolean checkPath(CuratorFramework curatorFramework, String registerPath) throws Exception {
        if (curatorFramework.checkExists().forPath(registerPath) == null) {
            curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(registerPath);
            curatorFramework.inTransaction().check().forPath(zkPositionPath).and().setData().forPath(zkPositionPath, StringUtils.EMPTY.getBytes()).and().commit();
            return false;
        }
        return true;
    }

    private void initZkClient() {
        try {
            ApplicationContext applicationContext = SpringUtils.getApplicationContext();
            if (applicationContext != null) {
                zkClient = applicationContext.getBean(SpringZkClient.class);
            }
        } catch (Exception e) {
            loggerMsg.error("[MQ][{}]  init zk client error", registryKey, e);
        }
    }

    class ZkPositionUpdateTask implements NamedCallable<Boolean> {

        @Override
        public Boolean call() throws Exception {
            String currentPosition = getCurrentPosition();
            loggerMsg.info("[MQ][{}] persist mq position to zk with: {}", registryKey, currentPosition);
            CuratorFramework curatorFramework = zkClient.get();
            checkPath(curatorFramework, zkPositionPath);
            curatorFramework.inTransaction().check().forPath(zkPositionPath).and().setData().forPath(zkPositionPath, currentPosition.getBytes()).and().commit();
            return true;
        }

        @Override
        public void afterSuccess(int retryTime) {
            loggerMsg.info("[MQ][{}] {} success with retryTime {}", registryKey, name(), retryTime);
        }
    }

    @Override
    protected void doDispose() throws Exception {
        new ZkPositionUpdateTask().call();
        updatePositionInFile();
        loggerMsg.info("[MQ][{}] persist mq position when mq position resource dispose", registryKey);

        if (gtidService != null) {
            gtidService.shutdown();
            gtidService = null;
        }
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
            scheduledExecutorService = null;
        }
    }

    @VisibleForTesting
    protected void setZkClient(ZkClient zkClient) {
        this.zkClient = zkClient;
    }
}

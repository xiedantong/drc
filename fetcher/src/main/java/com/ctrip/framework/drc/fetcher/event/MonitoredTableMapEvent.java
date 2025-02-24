package com.ctrip.framework.drc.fetcher.event;

import com.ctrip.framework.drc.core.driver.binlog.impl.TableMapLogEvent;
import com.ctrip.framework.drc.core.driver.schema.data.TableKey;
import com.ctrip.framework.drc.core.monitor.reporter.DefaultEventMonitorHolder;
import com.ctrip.framework.drc.fetcher.event.meta.MetaEvent;
import com.ctrip.framework.drc.fetcher.event.transaction.TransactionEvent;
import com.ctrip.framework.drc.fetcher.resource.condition.DirectMemory;
import com.ctrip.framework.drc.fetcher.resource.condition.DirectMemoryAware;
import com.ctrip.framework.drc.fetcher.resource.transformer.TransformerContext;
import com.ctrip.framework.drc.fetcher.resource.context.BaseTransactionContext;
import com.ctrip.framework.drc.fetcher.resource.context.LinkContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: Haibo Shen
 * @Date: 2021/3/17
 */
public class MonitoredTableMapEvent<T extends BaseTransactionContext> extends TableMapLogEvent implements MetaEvent.Write<LinkContext>, DirectMemoryAware, TransactionEvent<T> {

    protected static final Logger logger = LoggerFactory.getLogger(MonitoredTableMapEvent.class);

    private TableKey tableKey;

    private TableKey transformerTableKey;

    private String gtid;

    private int dataIndex;

    private DirectMemory directMemory;

    public MonitoredTableMapEvent() {
        logTableMapEvent();
    }

    private AtomicBoolean released = new AtomicBoolean(false);

    @Override
    public void release() {
        if (released.compareAndSet(false, true)) {
            directMemory.release(getLogEventHeader().getEventSize());
            super.release();
        }
    }

    @Override
    public void involve(LinkContext linkContext) {
        gtid = linkContext.fetchGtid();
        dataIndex = linkContext.increaseDataIndexByOne();
        tableKey = TableKey.from(getSchemaName(), getTableName());
        tableKey.setColumns(getColumns());
        linkContext.updateTableKeyMap(getTableId(), tableKey);
        transformerTableKey = tableKey;
        logEvent();
    }

    protected void logTableMapEvent() {
        DefaultEventMonitorHolder.getInstance().logBatchEvent("db.event", "table map", 1, 0);
    }

    protected void logEvent() {
        DefaultEventMonitorHolder.getInstance().logBatchEvent("db", getSchemaName(), 1, 0);
        DefaultEventMonitorHolder.getInstance().logBatchEvent("table", getSchemaName() + "." + getTableName(), 1, 0);
    }

    public String identifier() {
        try {
            return gtid + "-" + dataIndex + "-" + tableKey.toString();
        } catch (Throwable t) {
            return getClass().getSimpleName() + ":UNKNOWN";
        }
    }

    @Override
    public void transformer(TransformerContext transformerContext) {
        String transformerName = transformerContext.getNameMap().get(tableKey.toString());
        if (transformerName != null) {
            transformerTableKey = TableKey.from(transformerName);
        }
    }

    @Override
    public ApplyResult apply(T context) {
        release();
        context.updateTableKeyMap(getTableId(), transformerTableKey);
        return ApplyResult.SUCCESS;
    }

    @Override
    public void setDirectMemory(DirectMemory directMemory) {
        this.directMemory = directMemory;
    }

}

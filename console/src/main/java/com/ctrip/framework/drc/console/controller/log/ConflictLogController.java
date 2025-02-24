package com.ctrip.framework.drc.console.controller.log;

import com.ctrip.framework.drc.console.aop.log.LogRecord;
import com.ctrip.framework.drc.console.aop.permission.AccessToken;
import com.ctrip.framework.drc.console.enums.TokenType;
import com.ctrip.framework.drc.console.enums.log.LogBlackListType;
import com.ctrip.framework.drc.console.enums.operation.OperateAttrEnum;
import com.ctrip.framework.drc.console.enums.operation.OperateTypeEnum;
import com.ctrip.framework.drc.console.param.log.ConflictAutoHandleParam;
import com.ctrip.framework.drc.console.param.log.ConflictRowsLogQueryParam;
import com.ctrip.framework.drc.console.param.log.ConflictTrxLogQueryParam;
import com.ctrip.framework.drc.console.service.log.ConflictLogService;
import com.ctrip.framework.drc.console.vo.log.*;
import com.ctrip.framework.drc.core.http.ApiResult;
import com.ctrip.framework.drc.fetcher.conflict.ConflictTransactionLog;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Created by dengquanliang
 * 2023/9/27 14:19
 */
@RestController
@RequestMapping("/api/drc/v2/log/conflict/")
public class ConflictLogController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ConflictLogService conflictLogService;

    @GetMapping("trx")
    @LogRecord(type = OperateTypeEnum.CONFLICT_RESOLUTION, attr = OperateAttrEnum.QUERY,
            success = "getConflictTrxLogView with ConflictTrxLogQueryParam: {#param.db}")
    public ApiResult<List<ConflictTrxLogView>> getConflictTrxLogView(ConflictTrxLogQueryParam param) {
        try {
            ApiResult apiResult = ApiResult.getSuccessInstance(conflictLogService.getConflictTrxLogView(param));
            apiResult.setPageReq(param.getPageReq());
            return apiResult;
        } catch (Exception e) {
            logger.error("getConflictTrxLogView error", e);
            return ApiResult.getFailInstance(null, e.getMessage());
        }
    }

    @GetMapping("trx/count")
    public ApiResult<Integer> getTrxCount(ConflictTrxLogQueryParam param) {
        try {
            return ApiResult.getSuccessInstance(conflictLogService.getTrxLogCount(param));
        } catch (Exception e) {
            logger.error("getTrxCount fail: {}", e);
            return ApiResult.getFailInstance(null, e.getMessage());
        }
    }

    @GetMapping("rows")
    @LogRecord(type = OperateTypeEnum.CONFLICT_RESOLUTION, attr = OperateAttrEnum.QUERY,
            success = "getConflictRowsLogView with ConflictRowsLogQueryParam: {#param.toString()}")
    public ApiResult<List<ConflictRowsLogView>> getConflictRowsLogView(ConflictRowsLogQueryParam param) {
        try {
            ApiResult apiResult = ApiResult.getSuccessInstance(conflictLogService.getConflictRowsLogView(param));
            apiResult.setPageReq(param.getPageReq());
            return apiResult;
        } catch (Exception e) {
            logger.error("getConflictRowsLogView error, {}", e);
            return ApiResult.getFailInstance(null, e.getMessage());
        }
    }

    @GetMapping("rows/count")
    public ApiResult<Integer> getRowsCount(ConflictRowsLogQueryParam param) {
        try {
            return ApiResult.getSuccessInstance(conflictLogService.getRowsLogCount(param));
        } catch (Exception e) {
            logger.error("getRowsCount fail: {}", e);
            return ApiResult.getFailInstance(null, e.getMessage());
        }
    }

    @GetMapping("rows/rowLogIds")
    public ApiResult<List<ConflictRowsLogView>> getConflictRowsLogView(@RequestParam List<Long> rowLogIds) {
        try {
            ApiResult apiResult = ApiResult.getSuccessInstance(conflictLogService.getConflictRowsLogView(rowLogIds));
            return apiResult;
        } catch (Exception e) {
            logger.error("getConflictRowsLogView error, {}", e);
            return ApiResult.getFailInstance(null, e.getMessage());
        }
    }

    @GetMapping("detail")
    @LogRecord(type = OperateTypeEnum.CONFLICT_RESOLUTION, attr = OperateAttrEnum.QUERY,
            success = "getConflictTrxLogDetailView with conflictTrxLogId: {#conflictTrxLogId}")
    public ApiResult<ConflictTrxLogDetailView> getConflictTrxLogDetailView(@RequestParam long conflictTrxLogId) {
        try {
            ApiResult apiResult = ApiResult.getSuccessInstance(
                    conflictLogService.getConflictTrxLogDetailView(conflictTrxLogId));
            return apiResult;
        } catch (Exception e) {
            logger.error("getConflictTrxLogDetailView error, {}", e);
            return ApiResult.getFailInstance(null, e.getMessage());
        }
    }

    @GetMapping("records")
    public ApiResult<ConflictCurrentRecordView> getConflictCurrentRecordView(@RequestParam long conflictTrxLogId,
            @RequestParam int columnSize) {
        try {
            ApiResult apiResult = ApiResult.getSuccessInstance(
                    conflictLogService.getConflictCurrentRecordView(conflictTrxLogId, columnSize));
            return apiResult;
        } catch (Exception e) {
            logger.error("getConflictCurrentRecordView error, {}", e);
            return ApiResult.getFailInstance(null, e.getMessage());
        }
    }

    @GetMapping("/row/record")
    public ApiResult<ConflictCurrentRecordView> getConflictRowRecordView(@RequestParam long conflictRowLogId,
            @RequestParam int columnSize) {
        try {
            ApiResult apiResult = ApiResult.getSuccessInstance(
                    conflictLogService.getConflictRowRecordView(conflictRowLogId, columnSize));
            return apiResult;
        } catch (Exception e) {
            logger.error("getConflictRowRecordView error, {}", e);
            return ApiResult.getFailInstance(null, e.getMessage());
        }
    }

    @GetMapping("/records/compare")
    public ApiResult<ConflictRowsRecordCompareView> compareRowRecords(@RequestParam List<Long> conflictRowLogIds) {
        try {
            ApiResult apiResult = ApiResult.getSuccessInstance(conflictLogService.compareRowRecords(conflictRowLogIds));
            return apiResult;
        } catch (Exception e) {
            logger.error("compareRowRecords error, {}", e);
            return ApiResult.getFailInstance(null, e.getMessage());
        }
    }

    @GetMapping("/compare")
    public ApiResult<List<ConflictRowRecordCompareEqualView>> compareRowRecordsEqual(@RequestParam List<Long> conflictRowLogIds) {
        try {
            ApiResult apiResult = ApiResult.getSuccessInstance(conflictLogService.compareRowRecordsEqual(conflictRowLogIds));
            return apiResult;
        } catch (Exception e) {
            logger.error("compareRowRecordsEqual error, {}", e);
            return ApiResult.getFailInstance(null, e.getMessage());
        }
    }

    @PostMapping("")
    public ApiResult<Boolean> createConflictLog(@RequestBody List<ConflictTransactionLog> trxLogs) {
        try {
            conflictLogService.createConflictLog(trxLogs);
            return ApiResult.getSuccessInstance(true);
        } catch (Exception e) {
            logger.error("createConflictLog error, {}", e);
            return ApiResult.getFailInstance(null, e.getMessage());
        }
    }

    @DeleteMapping("/v2")
    @LogRecord(type = OperateTypeEnum.CONFLICT_RESOLUTION, attr = OperateAttrEnum.DELETE,
            success = "deleteTrxLogs from {#beginTime} to {#endTime}")
    public ApiResult<Long> deleteTrxLogs(@RequestParam long beginTime, @RequestParam long endTime) {
        try {
            long result = conflictLogService.deleteTrxLogs(beginTime, endTime);
            return ApiResult.getSuccessInstance(result);
        } catch (Exception e) {
            logger.error("deleteTrxLogs error, {}", e);
            return ApiResult.getFailInstance(null, e.getMessage());
        }
    }

    @DeleteMapping("")
    @LogRecord(type = OperateTypeEnum.CONFLICT_RESOLUTION, attr = OperateAttrEnum.DELETE,
            success = "deleteRowLogs from {#beginTime} to {#endTime}")
    public ApiResult<Map<String, Integer>> deleteRowLogs(@RequestParam long beginTime, @RequestParam long endTime) {
        try {
            return ApiResult.getSuccessInstance(conflictLogService.deleteTrxLogsByTime(beginTime, endTime));
        } catch (Exception e) {
            logger.error("deleteRowLogs error, {}", e);
            return ApiResult.getFailInstance(null, e.getMessage());
        }
    }

    @GetMapping("/rows/check")
    public ApiResult<Pair<String, String>> checkSameMhaReplication(@RequestParam List<Long> conflictRowLogIds) {
        try {
            ApiResult apiResult = ApiResult.getSuccessInstance(
                    conflictLogService.checkSameMhaReplication(conflictRowLogIds));
            return apiResult;
        } catch (Exception e) {
            logger.error("checkSameMhaReplication error, {}", e);
            return ApiResult.getFailInstance(null, e.getMessage());
        }
    }

    @GetMapping("/rows/detail")
    public ApiResult<ConflictTrxLogDetailView> getConflictRowLogDetailView(@RequestParam List<Long> conflictRowLogIds) {
        try {
            ApiResult apiResult = ApiResult.getSuccessInstance(
                    conflictLogService.getRowLogDetailView(conflictRowLogIds));
            return apiResult;
        } catch (Exception e) {
            logger.error("getConflictRowLogDetailView error, {}", e);
            return ApiResult.getFailInstance(null, e.getMessage());
        }
    }

    @GetMapping("/rows/records")
    public ApiResult<ConflictCurrentRecordView> getConflictRowRecordView(@RequestParam List<Long> conflictRowLogIds) {
        try {
            ApiResult apiResult = ApiResult.getSuccessInstance(
                    conflictLogService.getConflictRowRecordView(conflictRowLogIds));
            return apiResult;
        } catch (Exception e) {
            logger.error("getConflictRowRecordView error, {}", e);
            return ApiResult.getFailInstance(null, e.getMessage());
        }
    }

    @PostMapping("/rows/handleSql")
    @LogRecord(type = OperateTypeEnum.CONFLICT_RESOLUTION, attr = OperateAttrEnum.ADD,
            success = "createHandleSql with ConflictAutoHandleParam : {#param.toString()}")
    public ApiResult<ConflictAutoHandleView> createHandleSql(@RequestBody ConflictAutoHandleParam param) {
        try {
            ApiResult apiResult = ApiResult.getSuccessInstance(conflictLogService.createHandleSql(param));
            return apiResult;
        } catch (Exception e) {
            logger.error("createHandleSql error: {}", e);
            return ApiResult.getFailInstance(null, e.getMessage());
        }
    }

    @PostMapping("/db/blacklist")
    public ApiResult<Boolean> addDbBlacklist(@RequestParam String dbFilter) {
        try {
            conflictLogService.addDbBlacklist(dbFilter, LogBlackListType.USER);
            return ApiResult.getSuccessInstance(true);
        } catch (Exception e) {
            logger.error("addDbBlacklist error, {}", e);
            return ApiResult.getFailInstance(false, e.getMessage());
        }
    }

    @DeleteMapping("/db/blacklist")
    public ApiResult<Boolean> deleteBlacklist(@RequestParam String dbFilter) {
        try {
            conflictLogService.deleteBlacklist(dbFilter);
            return ApiResult.getSuccessInstance(true);
        } catch (Exception e) {
            logger.error("deleteBlacklist error, {}", e);
            return ApiResult.getFailInstance(false, e.getMessage());
        }
    }
    
    @AccessToken(type = TokenType.OPEN_API_4_DBA)
    @PostMapping("blacklist/dba/touchjob")
    @LogRecord(type = OperateTypeEnum.CONFLICT_RESOLUTION, attr = OperateAttrEnum.ADD, operator = "DBA",
            success = "addBlackListForTouchJob with db : {#db} and table : {#table}")
    public ApiResult<Boolean> addBlackListForTouchJob(@RequestParam String db, @RequestParam String table) {
        try {
            conflictLogService.addDbBlacklist(db + "\\." + table, LogBlackListType.DBA);
            return ApiResult.getSuccessInstance(true);
        } catch (Exception e) {
            logger.error("addBlackListForTouchJob error", e);
            return ApiResult.getFailInstance(false, e.getMessage());
        }
    }

    @AccessToken(type = TokenType.OPEN_API_4_DBA)
    @DeleteMapping("blacklist/dba/touchjob")
    @LogRecord(type = OperateTypeEnum.CONFLICT_RESOLUTION, attr = OperateAttrEnum.DELETE, operator = "DBA",
            success = "deleteBlackListForTouchJob with db : {#db} and table : {#table}")
    public ApiResult<Boolean> deleteBlackListForTouchJob(@RequestParam String db, @RequestParam String table) {
        try {
            conflictLogService.deleteBlacklist(db + "\\." + table);
            return ApiResult.getSuccessInstance(true);
        } catch (Exception e) {
            logger.error("deleteBlackListForTouchJob error", e);
            return ApiResult.getFailInstance(false, e.getMessage());
        }
    }
}

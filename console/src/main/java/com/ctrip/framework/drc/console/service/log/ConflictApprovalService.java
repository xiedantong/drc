package com.ctrip.framework.drc.console.service.log;

import com.ctrip.framework.drc.console.param.log.ConflictApprovalCreateParam;
import com.ctrip.framework.drc.console.param.log.ConflictApprovalQueryParam;
import com.ctrip.framework.drc.console.vo.log.*;

import java.util.List;

/**
 * Created by dengquanliang
 * 2023/10/31 11:19
 */
public interface ConflictApprovalService {

    List<ConflictApprovalView> getConflictApprovalViews(ConflictApprovalQueryParam param) throws Exception;

    ConflictCurrentRecordView getConflictRecordView(Long approvalId) throws Exception;

    ConflictTrxLogDetailView getConflictRowLogDetailView(Long approvalId) throws Exception;

    List<ConflictAutoHandleView> getConflictAutoHandleView(Long approvalId) throws Exception;

    void createConflictApproval(ConflictApprovalCreateParam param) throws Exception;

    void approvalCallBack(ConflictApprovalCallBackRequest request) throws Exception;

    void executeApproval(Long approvalId) throws Exception;
}

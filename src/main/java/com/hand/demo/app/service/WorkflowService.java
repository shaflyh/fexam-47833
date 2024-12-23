package com.hand.demo.app.service;

import com.hand.demo.domain.entity.InvCountHeader;
import org.hzero.boot.workflow.dto.RunTaskHistory;

import java.util.List;

public interface WorkflowService {

    void startWorkflow(Long tenantId, InvCountHeader header);

    List<RunTaskHistory> getApproveHistory(Long tenantId, String countNumber);
}
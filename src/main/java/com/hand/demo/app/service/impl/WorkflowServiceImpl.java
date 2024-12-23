package com.hand.demo.app.service.impl;

import com.hand.demo.api.dto.InvCountHeaderDTO;
import com.hand.demo.api.dto.WorkflowEventDTO;
import com.hand.demo.app.service.IamDepartmentService;
import com.hand.demo.app.service.WorkflowService;
import com.hand.demo.domain.entity.InvCountHeader;
import com.hand.demo.infra.util.Utils;
import io.choerodon.core.exception.CommonException;
import org.hzero.boot.workflow.WorkflowClient;
import org.hzero.boot.workflow.dto.RunTaskHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkflowServiceImpl implements WorkflowService {

    private final IamDepartmentService departmentService;
    private final WorkflowClient workflowClient;
    private final Utils utils;

    private static final String FLOW_KEY = "INV_COUNT33_RESULT_SUBMIT";

    @Autowired
    public WorkflowServiceImpl(IamDepartmentService departmentService, WorkflowClient workflowClient, Utils utils) {
        this.departmentService = departmentService;
        this.workflowClient = workflowClient;
        this.utils = utils;
    }

    /**
     * Starts a workflow instance for the given inventory counting order.
     *
     * @param tenantId Tenant ID for the workflow.
     * @param header   Inventory counting order header.
     */
    @Override
    public void startWorkflow(Long tenantId, InvCountHeader header) {
        String businessKey = header.getCountNumber(); // "INV-Counting-0-20241223-090"
        // TODO: Make sure for this parameter
        String dimension = "EMPLOYEE"; // "EMPLOYEE"
        String starter = String.valueOf(utils.getUserVO().getId()); // "47833"
        Map<String, Object> variableMap = new HashMap<>();

        // Set the department code to determine workflow
        String departmentCode = departmentService.getDepartmentCode(header.getDepartmentId());
        variableMap.put("departmentCode", departmentCode);

        // Call workflow client to start the workflow
        try {
            workflowClient.startInstanceByFlowKey(tenantId, FLOW_KEY, businessKey, dimension, starter, variableMap);
        } catch (Exception e) {
            throw new CommonException("Failed to start workflow: ", e);
        }
    }

    @Override
    public List<RunTaskHistory> getApproveHistory(Long tenantId, String countNumber) {
        return workflowClient.approveHistoryByFlowKey(tenantId, FLOW_KEY, countNumber);
    }
}




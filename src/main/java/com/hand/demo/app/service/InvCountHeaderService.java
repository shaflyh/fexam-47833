package com.hand.demo.app.service;

import com.hand.demo.api.dto.InvCountHeaderDTO;
import com.hand.demo.api.dto.InvCountInfoDTO;
import com.hand.demo.api.dto.WorkflowEventDTO;
import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import com.hand.demo.domain.entity.InvCountHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * (InvCountHeader)应用服务
 *
 * @author Shafly - 47833
 * @since 2024-12-17 10:42:53
 */
public interface InvCountHeaderService {


    /**
     * Manual Save Check
     *
     * @param invCountHeaders invCountHeaders
     */
    InvCountInfoDTO manualSaveCheck(List<InvCountHeaderDTO> invCountHeaders);

    /**
     * Manual Save
     *
     * @param invCountHeaders invCountHeaders
     */
    List<InvCountHeaderDTO> manualSave(List<InvCountHeaderDTO> invCountHeaders);

    /**
     * Execute Check
     *
     * @param invCountHeaders invCountHeaders
     */
    InvCountInfoDTO executeCheck(List<InvCountHeaderDTO> invCountHeaders);

    /**
     * Execute Check
     *
     * @param invCountHeaders invCountHeaders
     */
    List<InvCountHeaderDTO> execute(List<InvCountHeaderDTO> invCountHeaders);

    /**
     * Count Sync WMS
     *
     * @param invCountHeaders invCountHeaders
     */
    InvCountInfoDTO countSyncWms(List<InvCountHeaderDTO> invCountHeaders);

    /**
     * 1. Counting order save (orderSave)
     *
     * @param invCountHeaders invCountHeaders
     */
    InvCountInfoDTO orderSave(List<InvCountHeaderDTO> invCountHeaders);

    /**
     * 2. Counting order remove (orderRemove)
     *
     * @param invCountHeaders invCountHeaders
     */
    InvCountInfoDTO orderRemove(List<InvCountHeaderDTO> invCountHeaders);

    /**
     * 3.a. Counting order query (list)
     *
     * @param pageRequest    分页参数
     * @param invCountHeader 查询条件
     * @return 返回值
     */
    Page<InvCountHeaderDTO> selectList(PageRequest pageRequest, InvCountHeaderDTO invCountHeader);

    /**
     * 3.b. Counting order query (detail)
     *
     * @param countHeaderId countHeaderId
     * @return InvCountHeaderDTO
     */
    InvCountHeaderDTO selectDetail(Long countHeaderId);

    /**
     * 4. Counting order execution (orderExecution)
     *
     * @param invCountHeaders invCountHeaders
     */
    InvCountInfoDTO orderExecution(List<InvCountHeaderDTO> invCountHeaders);

    /**
     * 5. Submit counting results for approval (orderSubmit)
     *
     * @param invCountHeaders invCountHeaders
     */
    InvCountInfoDTO orderSubmit(List<InvCountHeaderDTO> invCountHeaders);

    /**
     * 6. Counting result synchronous (countResultSync)
     *
     * @param invCountHeader invCountHeader
     */
    InvCountHeaderDTO countResultSync(InvCountHeaderDTO invCountHeader);

    /**
     * 7. Counting order report dataset method (countingOrderReportDs)
     *
     * @param invCountHeader invCountHeader
     */
    List<InvCountHeaderDTO> countingOrderReportDs(InvCountHeaderDTO invCountHeader);

    InvCountHeaderDTO workflowCallback(Long tenantId, WorkflowEventDTO workflowEventDTO);
}


package com.hand.demo.app.service;

import com.hand.demo.api.dto.InvCountHeaderDTO;
import com.hand.demo.api.dto.InvCountInfoDTO;
import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import com.hand.demo.domain.entity.InvCountHeader;

import java.util.List;

/**
 * (InvCountHeader)应用服务
 *
 * @author Shafly - 47833
 * @since 2024-12-17 10:42:53
 */
public interface InvCountHeaderService {


    /**
     * 1. Counting order save (orderSave)
     *
     * @param invCountHeaders invCountHeaders
     */
    InvCountInfoDTO orderSave(List<InvCountHeader> invCountHeaders);

    /**
     * 2. Counting order remove (orderRemove)
     *
     * @param invCountHeaders invCountHeaders
     */
    InvCountInfoDTO orderRemove(List<InvCountHeader> invCountHeaders);

    /**
     * 3.a. Counting order query (list)
     *
     * @param pageRequest    分页参数
     * @param invCountHeader 查询条件
     * @return 返回值
     */
    Page<InvCountHeaderDTO> selectList(PageRequest pageRequest, InvCountHeader invCountHeader);

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
    InvCountInfoDTO orderExecution(List<InvCountHeader> invCountHeaders);

    /**
     * 5. Submit counting results for approval (orderSubmit)
     *
     * @param invCountHeaders invCountHeaders
     */
    InvCountInfoDTO orderSubmit(List<InvCountHeader> invCountHeaders);

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

}


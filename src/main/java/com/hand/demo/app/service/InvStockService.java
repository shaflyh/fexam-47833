package com.hand.demo.app.service;

import com.hand.demo.api.dto.InvCountHeaderDTO;
import com.hand.demo.api.dto.InvStockSummaryDTO;
import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import com.hand.demo.domain.entity.InvStock;

import java.util.List;

/**
 * (InvStock)应用服务
 *
 * @author Shafly - 47833
 * @since 2024-12-17 10:43:39
 */
public interface InvStockService {

    /**
     * 查询数据
     *
     * @param pageRequest 分页参数
     * @param invStocks   查询条件
     * @return 返回值
     */
    Page<InvStock> selectList(PageRequest pageRequest, InvStock invStocks);

    /**
     * 保存数据
     *
     * @param invStocks 数据
     */
    void saveData(List<InvStock> invStocks);

    List<InvStock> fetchValidStocks(InvCountHeaderDTO headerDTO);

    List<InvStockSummaryDTO> selectStockSummary(InvCountHeaderDTO header);
}


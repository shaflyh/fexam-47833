package com.hand.demo.app.service.impl;

import com.hand.demo.api.dto.InvCountHeaderDTO;
import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.hzero.mybatis.domian.Condition;
import org.springframework.beans.factory.annotation.Autowired;
import com.hand.demo.app.service.InvStockService;
import org.springframework.stereotype.Service;
import com.hand.demo.domain.entity.InvStock;
import com.hand.demo.domain.repository.InvStockRepository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * (InvStock)应用服务
 *
 * @author Shafly - 47833
 * @since 2024-12-17 10:43:40
 */
@Service
public class InvStockServiceImpl implements InvStockService {
    private final InvStockRepository invStockRepository;

    @Autowired
    public InvStockServiceImpl(InvStockRepository invStockRepository) {
        this.invStockRepository = invStockRepository;
    }

    @Override
    public Page<InvStock> selectList(PageRequest pageRequest, InvStock invStock) {
        return PageHelper.doPageAndSort(pageRequest, () -> invStockRepository.selectList(invStock));
    }

    @Override
    public void saveData(List<InvStock> invStocks) {
        List<InvStock> insertList =
                invStocks.stream().filter(line -> line.getStockId() == null).collect(Collectors.toList());
        List<InvStock> updateList =
                invStocks.stream().filter(line -> line.getStockId() != null).collect(Collectors.toList());
        invStockRepository.batchInsertSelective(insertList);
        invStockRepository.batchUpdateByPrimaryKeySelective(updateList);
    }

    @Override
    public List<InvStock> fetchValidStocks(InvCountHeaderDTO headerDTO) {
        // Create a condition for the InvStock entity
        Condition condition = new Condition(InvStock.class);

        // Build the condition
        // On hand quantity validation is not 0 and according to:
        // tenantId + companyId + departmentId + warehouseId + snapshotMaterialIds + snapshotBatchIds
        condition.createCriteria()
                .andEqualTo("tenantId", headerDTO.getTenantId())
                .andEqualTo("companyId", headerDTO.getCompanyId())
                .andEqualTo("departmentId", headerDTO.getDepartmentId())
                .andEqualTo("warehouseId", headerDTO.getWarehouseId())
                .andGreaterThan("unitQuantity", BigDecimal.ZERO); // Ensure on-hand quantity is greater than 0

        // Add dynamic list-based conditions for snapshotMaterialIds
        if (headerDTO.getSnapshotMaterialIds() != null && !headerDTO.getSnapshotMaterialIds().isEmpty()) {
            List<String> materialIds = Arrays.asList(headerDTO.getSnapshotMaterialIds().split(","));
            condition.and().andIn("materialId", materialIds);
        }

        if (headerDTO.getSnapshotBatchIds() != null && !headerDTO.getSnapshotBatchIds().isEmpty()) {
            List<String> batchIds = Arrays.asList(headerDTO.getSnapshotBatchIds().split(","));
            condition.and().andIn("batchId", batchIds);
        }

        // Query using the stock repository
        return invStockRepository.selectByCondition(condition);
    }
}


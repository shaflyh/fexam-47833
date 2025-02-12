package com.hand.demo.app.service.impl;

import com.hand.demo.api.dto.InvCountHeaderDTO;
import com.hand.demo.api.dto.InvStockSummaryDTO;
import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.apache.commons.lang.StringUtils;
import org.hzero.mybatis.domian.Condition;
import org.hzero.mybatis.util.Sqls;
import org.springframework.beans.factory.annotation.Autowired;
import com.hand.demo.app.service.InvStockService;
import org.springframework.stereotype.Service;
import com.hand.demo.domain.entity.InvStock;
import com.hand.demo.domain.repository.InvStockRepository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
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
        // Pre-process dynamic conditions
        List<String> materialIds = StringUtils.isNotBlank(headerDTO.getSnapshotMaterialIds())
                ? Arrays.asList(headerDTO.getSnapshotMaterialIds().split(","))
                : Collections.emptyList();
        List<String> batchIds = StringUtils.isNotBlank(headerDTO.getSnapshotBatchIds())
                ? Arrays.asList(headerDTO.getSnapshotBatchIds().split(","))
                : Collections.emptyList();

        // Build the condition dynamically according to:
        // tenantId + companyId + departmentId + warehouseId + snapshotMaterialIds + snapshotBatchIds
        Sqls sqls = Sqls.custom()
                .andEqualTo(InvStock.FIELD_TENANT_ID, headerDTO.getTenantId())
                .andEqualTo(InvStock.FIELD_COMPANY_ID, headerDTO.getCompanyId())
                .andEqualTo(InvStock.FIELD_DEPARTMENT_ID, headerDTO.getDepartmentId())
                .andEqualTo(InvStock.FIELD_WAREHOUSE_ID, headerDTO.getWarehouseId())
                .andGreaterThan(InvStock.FIELD_AVAILABLE_QUANTITY, BigDecimal.ZERO);

        if (!materialIds.isEmpty()) {
            sqls.andIn(InvStock.FIELD_MATERIAL_ID, materialIds);
        }

        if (!batchIds.isEmpty()) {
            sqls.andIn(InvStock.FIELD_BATCH_ID, batchIds);
        }

        // Query using the repository
        return invStockRepository.selectByCondition(
                Condition.builder(InvStock.class)
                        .andWhere(sqls)
                        .build()
        );
    }

    @Override
    public List<InvStockSummaryDTO> selectStockSummary(InvCountHeaderDTO header) {
        return invStockRepository.selectStockSummary(header);
    }


}


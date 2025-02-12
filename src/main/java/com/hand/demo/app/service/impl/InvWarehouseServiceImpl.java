package com.hand.demo.app.service.impl;

import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import com.hand.demo.app.service.InvWarehouseService;
import org.springframework.stereotype.Service;
import com.hand.demo.domain.entity.InvWarehouse;
import com.hand.demo.domain.repository.InvWarehouseRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * (InvWarehouse)应用服务
 *
 * @author Shafly - 47833
 * @since 2024-12-17 10:43:49
 */
@Service
public class InvWarehouseServiceImpl implements InvWarehouseService {
    private final InvWarehouseRepository invWarehouseRepository;

    @Autowired
    public InvWarehouseServiceImpl(InvWarehouseRepository invWarehouseRepository) {
        this.invWarehouseRepository = invWarehouseRepository;
    }

    @Override
    public Page<InvWarehouse> selectList(PageRequest pageRequest, InvWarehouse invWarehouse) {
        return PageHelper.doPageAndSort(pageRequest, () -> invWarehouseRepository.selectList(invWarehouse));
    }

    @Override
    public void saveData(List<InvWarehouse> invWarehouses) {
        List<InvWarehouse> insertList =
                invWarehouses.stream().filter(line -> line.getWarehouseId() == null).collect(Collectors.toList());
        List<InvWarehouse> updateList =
                invWarehouses.stream().filter(line -> line.getWarehouseId() != null).collect(Collectors.toList());
        invWarehouseRepository.batchInsertSelective(insertList);
        invWarehouseRepository.batchUpdateByPrimaryKeySelective(updateList);
    }

    @Override
    public boolean isWmsWarehouse(Long warehouseId) {
        InvWarehouse invWarehouse = invWarehouseRepository.selectByPrimaryKey(warehouseId);
        return invWarehouse.getIsWmsWarehouse().equals(1);
    }

    @Override
    public InvWarehouse validateWarehouse(Long tenantId, Long warehouseId) {
        InvWarehouse warehouse = new InvWarehouse();
        warehouse.setTenantId(tenantId);
        warehouse.setWarehouseId(warehouseId);
        List<InvWarehouse> fetchedWarehouse = invWarehouseRepository.select(warehouse);
        if (fetchedWarehouse.isEmpty()) {
            throw new CommonException("Warehouse does not exist for tenantId: " + tenantId + " and warehouseId: " + warehouseId);
        }
        return fetchedWarehouse.get(0);
    }

    @Override
    public Long getIdByWarehouseCode(String warehouseCode) {
        InvWarehouse warehouse = invWarehouseRepository.selectOne(new InvWarehouse().setWarehouseCode(warehouseCode));
        if (warehouse == null) {
            throw new CommonException("Warehouse not found for code: " + warehouseCode);
        }
        return warehouse.getWarehouseId();
    }

    @Override
    public InvWarehouse getById(Long id) {
        return invWarehouseRepository.selectByPrimary(id);
    }
}


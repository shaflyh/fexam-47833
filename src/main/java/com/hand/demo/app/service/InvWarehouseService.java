package com.hand.demo.app.service;

import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import com.hand.demo.domain.entity.InvWarehouse;

import java.util.List;

/**
 * (InvWarehouse)应用服务
 *
 * @author Shafly - 47833
 * @since 2024-12-17 10:43:48
 */
public interface InvWarehouseService {

    /**
     * 查询数据
     *
     * @param pageRequest   分页参数
     * @param invWarehouses 查询条件
     * @return 返回值
     */
    Page<InvWarehouse> selectList(PageRequest pageRequest, InvWarehouse invWarehouses);

    /**
     * 保存数据
     *
     * @param invWarehouses 数据
     */
    void saveData(List<InvWarehouse> invWarehouses);

    /**
     * Check is WMS Warehouse
     *
     * @param warehouseId warehouseId
     */
    boolean isWmsWarehouse(Long warehouseId);

    /**
     * Validates the existence of a warehouse based on tenant and warehouse ID.
     *
     * @param tenantId    The ID of the tenant.
     * @param warehouseId The ID of the warehouse.
     * @return The validated InvWarehouse object.
     */
    InvWarehouse validateWarehouse(Long tenantId, Long warehouseId);

    Long getIdByWarehouseCode(String warehouseCode);
}


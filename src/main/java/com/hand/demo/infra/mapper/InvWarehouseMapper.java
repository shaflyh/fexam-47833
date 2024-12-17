package com.hand.demo.infra.mapper;

import io.choerodon.mybatis.common.BaseMapper;
import com.hand.demo.domain.entity.InvWarehouse;

import java.util.List;

/**
 * (InvWarehouse)应用服务
 *
 * @author Shafly - 47833
 * @since 2024-12-17 10:43:48
 */
public interface InvWarehouseMapper extends BaseMapper<InvWarehouse> {
    /**
     * 基础查询
     *
     * @param invWarehouse 查询条件
     * @return 返回值
     */
    List<InvWarehouse> selectList(InvWarehouse invWarehouse);
}


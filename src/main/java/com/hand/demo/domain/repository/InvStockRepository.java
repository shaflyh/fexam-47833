package com.hand.demo.domain.repository;

import org.hzero.mybatis.base.BaseRepository;
import com.hand.demo.domain.entity.InvStock;

import java.util.List;

/**
 * (InvStock)资源库
 *
 * @author Shafly - 47833
 * @since 2024-12-17 10:43:39
 */
public interface InvStockRepository extends BaseRepository<InvStock> {
    /**
     * 查询
     *
     * @param invStock 查询条件
     * @return 返回值
     */
    List<InvStock> selectList(InvStock invStock);

    /**
     * 根据主键查询（可关联表）
     *
     * @param stockId 主键
     * @return 返回值
     */
    InvStock selectByPrimary(Long stockId);
}

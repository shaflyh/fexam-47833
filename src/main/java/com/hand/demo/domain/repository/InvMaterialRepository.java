package com.hand.demo.domain.repository;

import org.hzero.mybatis.base.BaseRepository;
import com.hand.demo.domain.entity.InvMaterial;

import java.util.List;

/**
 * (InvMaterial)资源库
 *
 * @author Shafly - 47833
 * @since 2024-12-17 10:43:22
 */
public interface InvMaterialRepository extends BaseRepository<InvMaterial> {
    /**
     * 查询
     *
     * @param invMaterial 查询条件
     * @return 返回值
     */
    List<InvMaterial> selectList(InvMaterial invMaterial);

    /**
     * 根据主键查询（可关联表）
     *
     * @param materialId 主键
     * @return 返回值
     */
    InvMaterial selectByPrimary(Long materialId);
}

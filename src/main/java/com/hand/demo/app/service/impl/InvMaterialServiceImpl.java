package com.hand.demo.app.service.impl;

import com.hand.demo.api.dto.MaterialDTO;
import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import com.hand.demo.app.service.InvMaterialService;
import org.springframework.stereotype.Service;
import com.hand.demo.domain.entity.InvMaterial;
import com.hand.demo.domain.repository.InvMaterialRepository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * (InvMaterial)应用服务
 *
 * @author Shafly - 47833
 * @since 2024-12-17 10:43:23
 */
@Service
public class InvMaterialServiceImpl implements InvMaterialService {

    private final InvMaterialRepository invMaterialRepository;

    @Autowired
    public InvMaterialServiceImpl(InvMaterialRepository invMaterialRepository) {
        this.invMaterialRepository = invMaterialRepository;
    }

    @Override
    public Page<InvMaterial> selectList(PageRequest pageRequest, InvMaterial invMaterial) {
        return PageHelper.doPageAndSort(pageRequest, () -> invMaterialRepository.selectList(invMaterial));
    }

    @Override
    public void saveData(List<InvMaterial> invMaterials) {
        List<InvMaterial> insertList =
                invMaterials.stream().filter(line -> line.getMaterialId() == null).collect(Collectors.toList());
        List<InvMaterial> updateList =
                invMaterials.stream().filter(line -> line.getMaterialId() != null).collect(Collectors.toList());
        invMaterialRepository.batchInsertSelective(insertList);
        invMaterialRepository.batchUpdateByPrimaryKeySelective(updateList);
    }

    @Override
    public InvMaterial selectById(Long materialId) {
        return invMaterialRepository.selectByPrimaryKey(materialId);
    }

    /**
     * Converts a comma-separated string of material IDs to a list of MaterialDTOs.
     */
    @Override
    public List<MaterialDTO> convertToMaterialDTOs(String materialIds) {
        List<InvMaterial> materials = invMaterialRepository.selectByIds(materialIds);
        return materials.stream().map(this::mapToMaterialDTO).collect(Collectors.toList());
    }

    /**
     * Maps an InvMaterial entity to a MaterialDTO.
     *
     * @param material the InvMaterial entity
     * @return the corresponding MaterialDTO
     */
    private MaterialDTO mapToMaterialDTO(InvMaterial material) {
        MaterialDTO dto = new MaterialDTO();
        dto.setMaterialId(material.getMaterialId());
        dto.setMaterialCode(material.getMaterialCode());
        return dto;
    }
}


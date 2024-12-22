package com.hand.demo.app.service.impl;

import com.hand.demo.api.dto.InvCountLineDTO;
import com.hand.demo.infra.util.Utils;
import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.hzero.mybatis.domian.Condition;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import com.hand.demo.app.service.InvCountLineService;
import org.springframework.stereotype.Service;
import com.hand.demo.domain.entity.InvCountLine;
import com.hand.demo.domain.repository.InvCountLineRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * (InvCountLine)应用服务
 *
 * @author Shafly - 47833
 * @since 2024-12-17 10:43:06
 */
@Service
public class InvCountLineServiceImpl implements InvCountLineService {

    private final InvCountLineRepository invCountLineRepository;
    private final Utils utils;

    @Autowired
    public InvCountLineServiceImpl(InvCountLineRepository invCountLineRepository, Utils utils) {
        this.invCountLineRepository = invCountLineRepository;
        this.utils = utils;
    }

    @Override
    public Page<InvCountLineDTO> selectList(PageRequest pageRequest, InvCountLine invCountLine) {

        // Convert to DTO
        InvCountLineDTO invCountLineDTO = new InvCountLineDTO();
        BeanUtils.copyProperties(invCountLine, invCountLineDTO);

        // Check if the user admin or not
        invCountLineDTO.setTenantAdminFlag(utils.getUserVO().getTenantAdminFlag() != null);
        return PageHelper.doPageAndSort(pageRequest, () -> invCountLineRepository.selectList(invCountLine));
    }

    @Override
    public void saveData(List<InvCountLine> invCountLines) {
        List<InvCountLine> insertList =
                invCountLines.stream().filter(line -> line.getCountLineId() == null).collect(Collectors.toList());
        List<InvCountLine> updateList =
                invCountLines.stream().filter(line -> line.getCountLineId() != null).collect(Collectors.toList());
        invCountLineRepository.batchInsertSelective(insertList);
        invCountLineRepository.batchUpdateByPrimaryKeySelective(updateList);
    }

    @Override
    public List<InvCountLineDTO> selectListByHeaderId(Long headerId) {
        Condition condition = new Condition(InvCountLine.class);
        // Define the criteria to filter by count_header_id
        condition.createCriteria().andEqualTo("countHeaderId", headerId);
        // Fetch the filtered data
        List<InvCountLine> invCountLines = invCountLineRepository.selectByCondition(condition);
        // return the DTOs
        List<InvCountLineDTO> invCountLineDTOS = new ArrayList<>();
        for (InvCountLine invCountLine : invCountLines) {
            InvCountLineDTO invCountLineDTO = new InvCountLineDTO();
            BeanUtils.copyProperties(invCountLine, invCountLineDTO);
            invCountLineDTOS.add(invCountLineDTO);
        }
        return invCountLineDTOS;
    }

    @Override
    public List<InvCountLine> fetchExistingLines(List<InvCountLine> lineList) {
        // Extract the set of IDs from the update list
        Set<Long> lineIds = lineList.stream().map(InvCountLine::getCountLineId).collect(Collectors.toSet());

        // Convert the set of IDs to a comma-separated string for the repository query
        String idString = lineIds.stream().map(String::valueOf).collect(Collectors.joining(","));

        // Fetch the existing lines from the repository using the comma-separated IDs
        return invCountLineRepository.selectByIds(idString);
    }
}


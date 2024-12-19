package com.hand.demo.app.service.impl;

import com.hand.demo.api.dto.InvCountLineDTO;
import com.hand.demo.infra.util.mapper.InvCountLineDTOMapper;
import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.hzero.mybatis.domian.Condition;
import org.springframework.beans.factory.annotation.Autowired;
import com.hand.demo.app.service.InvCountLineService;
import org.springframework.stereotype.Service;
import com.hand.demo.domain.entity.InvCountLine;
import com.hand.demo.domain.repository.InvCountLineRepository;

import java.util.List;
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
    private final InvCountLineDTOMapper invCountLineDTOMapper;

    @Autowired
    public InvCountLineServiceImpl(InvCountLineRepository invCountLineRepository,
                                   InvCountLineDTOMapper invCountLineDTOMapper) {
        this.invCountLineRepository = invCountLineRepository;
        this.invCountLineDTOMapper = invCountLineDTOMapper;
    }

    @Override
    public Page<InvCountLineDTO> selectList(PageRequest pageRequest, InvCountLine invCountLine) {
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
        return invCountLineDTOMapper.toDtoList(invCountLines);
    }
}


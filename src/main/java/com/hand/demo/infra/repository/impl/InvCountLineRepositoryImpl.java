package com.hand.demo.infra.repository.impl;

import com.hand.demo.api.dto.InvCountLineDTO;
import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.springframework.stereotype.Component;
import com.hand.demo.domain.entity.InvCountLine;
import com.hand.demo.domain.repository.InvCountLineRepository;
import com.hand.demo.infra.mapper.InvCountLineMapper;

import javax.annotation.Resource;
import java.util.List;

/**
 * (InvCountLine)资源库
 *
 * @author Shafly - 47833
 * @since 2024-12-17 10:43:06
 */
@Component
public class InvCountLineRepositoryImpl extends BaseRepositoryImpl<InvCountLine> implements InvCountLineRepository {
    @Resource
    private InvCountLineMapper invCountLineMapper;

    @Override
    public List<InvCountLineDTO> selectList(InvCountLine invCountLine) {
        return invCountLineMapper.selectList(invCountLine);
    }

    @Override
    public InvCountLine selectByPrimary(Long countLineId) {
        InvCountLine invCountLine = new InvCountLine();
        invCountLine.setCountLineId(countLineId);
        List<InvCountLineDTO> invCountLines = invCountLineMapper.selectList(invCountLine);
        if (invCountLines.size() == 0) {
            return null;
        }
        return invCountLines.get(0);
    }

}


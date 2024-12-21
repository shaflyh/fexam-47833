package com.hand.demo.app.service.impl;

import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import com.hand.demo.app.service.InvCountExtraService;
import org.springframework.stereotype.Service;
import com.hand.demo.domain.entity.InvCountExtra;
import com.hand.demo.domain.repository.InvCountExtraRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * (InvCountExtra)应用服务
 *
 * @author Shafly - 47833
 * @since 2024-12-17 10:42:19
 */
@Service
public class InvCountExtraServiceImpl implements InvCountExtraService {
    private final InvCountExtraRepository invCountExtraRepository;

    @Autowired
    public InvCountExtraServiceImpl(InvCountExtraRepository invCountExtraRepository) {
        this.invCountExtraRepository = invCountExtraRepository;
    }

    @Override
    public Page<InvCountExtra> selectList(PageRequest pageRequest, InvCountExtra invCountExtra) {
        return PageHelper.doPageAndSort(pageRequest, () -> invCountExtraRepository.selectList(invCountExtra));
    }

    @Override
    public void saveData(List<InvCountExtra> invCountExtras) {
        List<InvCountExtra> insertList =
                invCountExtras.stream().filter(line -> line.getExtraInfoId() == null).collect(Collectors.toList());
        List<InvCountExtra> updateList =
                invCountExtras.stream().filter(line -> line.getExtraInfoId() != null).collect(Collectors.toList());
        invCountExtraRepository.batchInsertSelective(insertList);
        invCountExtraRepository.batchUpdateByPrimaryKeySelective(updateList);
    }

    @Override
    public InvCountExtra createExtra(Long tenantId, Long sourceId, String programKey) {
        InvCountExtra extra = new InvCountExtra();
        extra.setTenantId(tenantId);
        extra.setSourceId(sourceId);
        extra.setEnabledFlag(1);
        extra.setProgramKey(programKey);
        return extra;
    }

    @Override
    public List<InvCountExtra> fetchExtrasByHeaderId(Long countHeaderId) {
        InvCountExtra query = new InvCountExtra();
        query.setEnabledFlag(1);
        query.setSourceId(countHeaderId);
        return invCountExtraRepository.select(query);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveExtras(InvCountExtra... extras) {
        for (InvCountExtra extra : extras) {
            invCountExtraRepository.insertSelective(extra);
        }
    }

}


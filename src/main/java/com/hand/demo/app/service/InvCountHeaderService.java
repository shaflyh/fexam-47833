package com.hand.demo.app.service;

import com.hand.demo.api.dto.InvCountHeaderDTO;
import com.hand.demo.api.dto.InvCountInfoDTO;
import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import com.hand.demo.domain.entity.InvCountHeader;

import java.util.List;

/**
 * (InvCountHeader)应用服务
 *
 * @author Shafly - 47833
 * @since 2024-12-17 10:42:53
 */
public interface InvCountHeaderService {

    /**
     * 查询数据
     *
     * @param pageRequest     分页参数
     * @param invCountHeaders 查询条件
     * @return 返回值
     */
    Page<InvCountHeaderDTO> selectList(PageRequest pageRequest, InvCountHeader invCountHeaders);

    /**
     * Select detail Invoice Count Header
     *
     * @param countHeaderId countHeaderId
     * @return InvCountHeaderDTO
     */
    InvCountHeaderDTO selectDetail(Long countHeaderId);

    /**
     * Create order save
     *
     * @param invCountHeaders invCountHeaders
     */
    InvCountInfoDTO orderSave(List<InvCountHeader> invCountHeaders);

    /**
     * Create order save
     *
     * @param invCountHeaders invCountHeaders
     */
    InvCountInfoDTO orderRemove(List<InvCountHeader> invCountHeaders);

    /**
     * Execute check
     *
     * @param invCountHeaders invCountHeaders
     */
    InvCountInfoDTO executeCheck(List<InvCountHeader> invCountHeaders);


    //    /**
    //     * 保存数据
    //     *
    //     * @param invCountHeaders 数据
    //     */
    //    void saveData(List<InvCountHeader> invCountHeaders);
}


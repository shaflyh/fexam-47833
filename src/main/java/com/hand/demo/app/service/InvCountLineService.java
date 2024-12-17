package com.hand.demo.app.service;

import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import com.hand.demo.domain.entity.InvCountLine;

import java.util.List;

/**
 * (InvCountLine)应用服务
 *
 * @author Shafly - 47833
 * @since 2024-12-17 10:43:06
 */
public interface InvCountLineService {

    /**
     * 查询数据
     *
     * @param pageRequest   分页参数
     * @param invCountLines 查询条件
     * @return 返回值
     */
    Page<InvCountLine> selectList(PageRequest pageRequest, InvCountLine invCountLines);

    /**
     * 保存数据
     *
     * @param invCountLines 数据
     */
    void saveData(List<InvCountLine> invCountLines);

}


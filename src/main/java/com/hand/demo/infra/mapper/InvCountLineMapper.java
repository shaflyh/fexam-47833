package com.hand.demo.infra.mapper;

import com.hand.demo.api.dto.InvCountLineDTO;
import io.choerodon.mybatis.common.BaseMapper;
import com.hand.demo.domain.entity.InvCountLine;

import java.util.List;

/**
 * (InvCountLine)应用服务
 *
 * @author Shafly - 47833
 * @since 2024-12-17 10:43:06
 */
public interface InvCountLineMapper extends BaseMapper<InvCountLine> {
    /**
     * 基础查询
     *
     * @param invCountLine 查询条件
     * @return 返回值
     */
    List<InvCountLineDTO> selectList(InvCountLine invCountLine);
}


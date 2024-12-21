package com.hand.demo.app.service;

import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import com.hand.demo.domain.entity.InvCountExtra;

import java.util.List;

/**
 * (InvCountExtra)应用服务
 *
 * @author Shafly - 47833
 * @since 2024-12-17 10:42:19
 */
public interface InvCountExtraService {

    /**
     * 查询数据
     *
     * @param pageRequest    分页参数
     * @param invCountExtras 查询条件
     * @return 返回值
     */
    Page<InvCountExtra> selectList(PageRequest pageRequest, InvCountExtra invCountExtras);

    /**
     * 保存数据
     *
     * @param invCountExtras 数据
     */
    void saveData(List<InvCountExtra> invCountExtras);


    InvCountExtra createExtra(Long tenantId, Long sourceId, String programKey);

    /**
     * Fetches synchronization extras for a given count header ID.
     *
     * @param countHeaderId The counting header ID.
     * @return List of InvCountExtra objects.
     */
    List<InvCountExtra> fetchExtrasByHeaderId(Long countHeaderId);

    /**
     * Saves synchronization extras to the database.
     * Keep the saved data in database even on transaction rollback
     *
     * @param extras Varargs of InvCountExtra objects to save.
     */
     void saveExtras(InvCountExtra... extras);
}


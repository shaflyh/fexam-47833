package com.hand.demo.app.service;

import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import com.hand.demo.domain.entity.IamDepartment;

import java.util.List;

/**
 * (IamDepartment)应用服务
 *
 * @author Shafly - 47833
 * @since 2024-12-17 10:41:37
 */
public interface IamDepartmentService {

    /**
     * 查询数据
     *
     * @param pageRequest    分页参数
     * @param iamDepartments 查询条件
     * @return 返回值
     */
    Page<IamDepartment> selectList(PageRequest pageRequest, IamDepartment iamDepartments);

    /**
     * 保存数据
     *
     * @param iamDepartments 数据
     */
    void saveData(List<IamDepartment> iamDepartments);

    String getDepartmentName(Long departmentId);

    String getDepartmentCode(Long departmentId);

    IamDepartment getDepartmentByCode(String departmentCode);

    IamDepartment getById(Long id);
}


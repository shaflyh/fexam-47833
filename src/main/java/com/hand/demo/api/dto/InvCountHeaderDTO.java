package com.hand.demo.api.dto;

import com.hand.demo.domain.entity.InvCountHeader;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hzero.boot.workflow.dto.RunTaskHistory;
import org.hzero.common.HZeroCacheKey;
import org.hzero.core.cache.CacheValue;
import org.hzero.core.cache.Cacheable;

import java.util.List;

/**
 * @author Shafly - 47833
 * @since 2024-12-17 13:05
 */

@Getter
@Setter
public class InvCountHeaderDTO extends InvCountHeader implements Cacheable {
    @ApiModelProperty(value = "Error Message")
    private String errorMsg;

    @ApiModelProperty(value = "Status")
    private String status;

    private String countStatusMeaning;

    private String countDimensionMeaning;

    private String countModeMeaning;

    private String countTypeMeaning;

    private List<InvCountLineDTO> countOrderLineList;

    // For invoice detail
    private List<UserDTO> counterList;

    private List<UserDTO> supervisorList;

    private List<MaterialDTO> snapshotMaterialList;

    private List<BatchDTO> snapshotBatchList;

    private Boolean isWMSWarehouse;

    // For data permission rule
    private Boolean tenantAdminFlag;

    // For countSyncWms
    private String employeeNumber;

    // Additional field for reporting
    private String companyCode;

    private String departmentCode;

    private String warehouseCode;

    private String departmentName;

    private String materialCodeList;

    private String batchCodeList;

    private List<RunTaskHistory> approvalHistory;

    @CacheValue(key = HZeroCacheKey.USER, primaryKey = "createdBy", searchKey = "tenantNum",
            structure = CacheValue.DataStructure.MAP_OBJECT)
    private String tenantCode;

    @CacheValue(key = HZeroCacheKey.USER, primaryKey = "createdBy", searchKey = "realName",
            structure = CacheValue.DataStructure.MAP_OBJECT)
    private String creatorName;
}

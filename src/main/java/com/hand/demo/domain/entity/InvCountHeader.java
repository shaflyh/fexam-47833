package com.hand.demo.domain.entity;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hand.demo.infra.constant.ErrorCodeConst;
import com.hand.demo.infra.constant.LovConst;
import io.choerodon.mybatis.annotation.ModifyAudit;
import io.choerodon.mybatis.annotation.VersionAudit;
import io.choerodon.mybatis.domain.AuditDomain;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import org.hzero.boot.platform.lov.annotation.LovValue;

/**
 * (InvCountHeader)实体类
 *
 * @author Shafly - 47833
 * @since 2024-12-17 10:42:52
 */

@Getter
@Setter
@ApiModel("")
@VersionAudit
@ModifyAudit
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Table(name = "fexam_inv_count_header")
public class InvCountHeader extends AuditDomain {
    private static final long serialVersionUID = 136455575279002850L;

    public static final String FIELD_COUNT_HEADER_ID = "countHeaderId";
    public static final String FIELD_APPROVED_TIME = "approvedTime";
    public static final String FIELD_ATTRIBUTE1 = "attribute1";
    public static final String FIELD_ATTRIBUTE10 = "attribute10";
    public static final String FIELD_ATTRIBUTE11 = "attribute11";
    public static final String FIELD_ATTRIBUTE12 = "attribute12";
    public static final String FIELD_ATTRIBUTE13 = "attribute13";
    public static final String FIELD_ATTRIBUTE14 = "attribute14";
    public static final String FIELD_ATTRIBUTE15 = "attribute15";
    public static final String FIELD_ATTRIBUTE2 = "attribute2";
    public static final String FIELD_ATTRIBUTE3 = "attribute3";
    public static final String FIELD_ATTRIBUTE4 = "attribute4";
    public static final String FIELD_ATTRIBUTE5 = "attribute5";
    public static final String FIELD_ATTRIBUTE6 = "attribute6";
    public static final String FIELD_ATTRIBUTE7 = "attribute7";
    public static final String FIELD_ATTRIBUTE8 = "attribute8";
    public static final String FIELD_ATTRIBUTE9 = "attribute9";
    public static final String FIELD_ATTRIBUTE_CATEGORY = "attributeCategory";
    public static final String FIELD_COMPANY_ID = "companyId";
    public static final String FIELD_COUNT_DIMENSION = "countDimension";
    public static final String FIELD_COUNT_MODE = "countMode";
    public static final String FIELD_COUNT_NUMBER = "countNumber";
    public static final String FIELD_COUNT_STATUS = "countStatus";
    public static final String FIELD_COUNT_TIME_STR = "countTimeStr";
    public static final String FIELD_COUNT_TYPE = "countType";
    public static final String FIELD_COUNTER_IDS = "counterIds";
    public static final String FIELD_DEL_FLAG = "delFlag";
    public static final String FIELD_DEPARTMENT_ID = "departmentId";
    public static final String FIELD_REASON = "reason";
    public static final String FIELD_RELATED_WMS_ORDER_CODE = "relatedWmsOrderCode";
    public static final String FIELD_REMARK = "remark";
    public static final String FIELD_SNAPSHOT_BATCH_IDS = "snapshotBatchIds";
    public static final String FIELD_SNAPSHOT_MATERIAL_IDS = "snapshotMaterialIds";
    public static final String FIELD_SOURCE_CODE = "sourceCode";
    public static final String FIELD_SOURCE_ID = "sourceId";
    public static final String FIELD_SOURCE_SYSTEM = "sourceSystem";
    public static final String FIELD_SUPERVISOR_IDS = "supervisorIds";
    public static final String FIELD_TENANT_ID = "tenantId";
    public static final String FIELD_WAREHOUSE_ID = "warehouseId";
    public static final String FIELD_WORKFLOW_ID = "workflowId";

    @Id
    @GeneratedValue
    private Long countHeaderId;

    @ApiModelProperty(value = "", required = true)
    @NotNull(message = ErrorCodeConst.INPUT_NULL)
    private Long tenantId;

    @ApiModelProperty(value = "", required = true)
    @NotBlank(message = ErrorCodeConst.INPUT_BLANK)
    @LovValue(lovCode = LovConst.INV_COUNT_STATUS, message = ErrorCodeConst.INPUT_INVALID)
    private String countStatus = "DRAFT"; // Default value: DRAFT

    @NotNull(message = ErrorCodeConst.INPUT_NULL)
    private Long companyId;

    @NotNull(message = ErrorCodeConst.INPUT_NULL)
    private Long warehouseId;

    @NotBlank(message = ErrorCodeConst.INPUT_BLANK)
    private String supervisorIds;

    @NotBlank(message = ErrorCodeConst.INPUT_BLANK)
    private String counterIds;

    private Date approvedTime;

    private String attribute1;

    private String attribute10;

    private String attribute11;

    private String attribute12;

    private String attribute13;

    private String attribute14;

    private String attribute15;

    private String attribute2;

    private String attribute3;

    private String attribute4;

    private String attribute5;

    private String attribute6;

    private String attribute7;

    private String attribute8;

    private String attribute9;

    private String attributeCategory;

    private String countDimension;

    private String countMode;

    @ApiModelProperty(value = "", required = true)
    private String countNumber;

    private String countTimeStr;

    private String countType;

    private Integer delFlag = 0; // Default value 0

    private Long departmentId;

    private String reason;

    private String relatedWmsOrderCode;

    private String remark;

    private String snapshotBatchIds;

    private String snapshotMaterialIds;

    private String sourceCode;

    private Long sourceId;

    private String sourceSystem;

    private Long workflowId;

    public interface Save {
    }

    public interface Update {
    }
}




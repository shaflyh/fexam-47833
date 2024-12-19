package com.hand.demo.api.dto;

import com.hand.demo.domain.entity.InvCountHeader;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hzero.core.cache.Cacheable;

import javax.persistence.Transient;
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

    @Transient
    private String countStatusMeaning;

    private List<InvCountLineDTO> invCountLineDTOList;

    // For invoice detail
    private List<UserDTO> counterList;

    private List<UserDTO> supervisorList;

    private List<MaterialDTO> snapshotMaterialList;

    private List<BatchDTO> snapshotBatchList;

    private Boolean isWMSwarehouse;

    // For data permission rule
    private boolean tenantAdminFlag;
}

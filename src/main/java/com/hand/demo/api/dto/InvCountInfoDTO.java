package com.hand.demo.api.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author Shafly - 47833
 * @since 2024-12-17 13:06
 */

@Getter
@Setter
public class InvCountInfoDTO {
    private String totalErrorMsg;

    @ApiModelProperty(value = "Verification passed list")
    private List<InvCountHeaderDTO> successList;

    @ApiModelProperty(value = "Verification failed list")
    private List<InvCountHeaderDTO> errorList;
}

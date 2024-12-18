package com.hand.demo.api.dto;

import com.hand.demo.domain.entity.InvCountHeader;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hzero.common.HZeroCacheKey;
import org.hzero.core.cache.CacheValue;

import javax.persistence.Transient;
import java.util.List;

/**
 * @author Shafly - 47833
 * @since 2024-12-17 13:05
 */

@Getter
@Setter
public class InvCountHeaderDTO extends InvCountHeader {
    @ApiModelProperty(value = "Error Message")
    private String errorMsg;

    private List<InvCountLineDTO> invCountLineDTOList;

    @Transient
    private String countStatusMeaning;

    //    @Transient
    //    @CacheValue(key = HZeroCacheKey.USER, primaryKey = "counterIds", searchKey = "realName",
    //            structure = CacheValue.DataStructure.LIST_OBJECT)
    //    private List<String> counterIdsCache;
    //
    //    @CacheValue(key = HZeroCacheKey.USER, primaryKey = "supervisorIds", searchKey = "realName",
    //            structure = CacheValue.DataStructure.LIST_OBJECT)
    //    private List<String> supervisorIdsCache;
}

package com.hand.demo.api.dto;

import com.hand.demo.domain.entity.InvCountLine;
import lombok.Getter;
import lombok.Setter;
import org.hzero.core.cache.Cacheable;

/**
 * @author Shafly - 47833
 * @since 2024-12-17 13:06
 */

@Getter
@Setter
public class InvCountLineDTO extends InvCountLine implements Cacheable {

    // For data permission rule
    private boolean tenantAdminFlag;

    // Additional field for reporting
    private String itemCode;

    private String itemName;

    private String batchCode;

    private UserDTO counter;
}

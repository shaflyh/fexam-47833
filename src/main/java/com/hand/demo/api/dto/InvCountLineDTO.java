package com.hand.demo.api.dto;

import com.hand.demo.domain.entity.InvCountLine;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Shafly - 47833
 * @since 2024-12-17 13:06
 */

@Getter
@Setter
public class InvCountLineDTO extends InvCountLine {

    // For data permission rule
    private boolean tenantAdminFlag;
}

package com.hand.demo.api.dto;

import com.hand.demo.domain.entity.InvStock;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InvStockSummaryDTO extends InvStock {
    private BigDecimal totalQuantity;
}

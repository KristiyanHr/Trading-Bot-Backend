package com.example.Auto_Trading_Bot.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioHolding {
    private Long id;
    private Long accountId;
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal averageBuyPrice;

    public PortfolioHolding(Long accountId, String symbol) {
        this.accountId = accountId;
        this.symbol = symbol;
    }
}

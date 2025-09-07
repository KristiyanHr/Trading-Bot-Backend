package com.example.Auto_Trading_Bot.models;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AccountStatus {
    private BigDecimal balance;
    private BigDecimal portfolioValue;
    private BigDecimal totalNetWorth;
    private List<Trade> trades;
    private List<PortfolioHolding> portfolioHoldings;
}

package com.example.Auto_Trading_Bot.models;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Trade {
    private Long id;
    private Long accountId;
    private String symbol;
    private TradeType tradeType;
    private BigDecimal quantity;
    private BigDecimal price;
    private LocalDateTime timestamp;
    private BigDecimal pnl;
}

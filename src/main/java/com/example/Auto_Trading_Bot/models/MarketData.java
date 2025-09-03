package com.example.Auto_Trading_Bot.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketData {
    private String symbol;
    private BigDecimal price;
    private LocalDateTime timestamp;

}

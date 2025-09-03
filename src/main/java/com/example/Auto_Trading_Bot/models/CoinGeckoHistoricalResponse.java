package com.example.Auto_Trading_Bot.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoinGeckoHistoricalResponse {
    private List<List<BigDecimal>> prices;
}

package com.example.Auto_Trading_Bot.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TickerPrice {
    private String symbol;
    private String price;



}

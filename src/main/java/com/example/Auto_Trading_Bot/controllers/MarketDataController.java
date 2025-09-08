package com.example.Auto_Trading_Bot.controllers;

import com.example.Auto_Trading_Bot.dao.MarketDataDAO;
import com.example.Auto_Trading_Bot.models.MarketData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/market-data")
@CrossOrigin(origins = "http://localhost:5173")
public class MarketDataController {

    @Autowired
    private MarketDataDAO marketDataDAO;

    @GetMapping("/{symbol}")
    public List<MarketData> getHistoricalData(
            @PathVariable String symbol,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
            ){
        return marketDataDAO.findBySymbolAndTimestampBetween(symbol, from, to);
    }
}

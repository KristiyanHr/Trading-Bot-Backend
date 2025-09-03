package com.example.Auto_Trading_Bot.controllers;

import com.example.Auto_Trading_Bot.dao.MarketDataDAO;
import com.example.Auto_Trading_Bot.models.CoinGeckoHistoricalResponse;
import com.example.Auto_Trading_Bot.models.MarketData;
import com.example.Auto_Trading_Bot.models.TickerPrice;
import com.example.Auto_Trading_Bot.services.CryptoAPIService;
import com.example.Auto_Trading_Bot.services.TradingBotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final CryptoAPIService cryptoAPIService;
    private final TradingBotService tradingBotService;
    private final MarketDataDAO marketDataDAO;

    @Autowired
    public TestController(CryptoAPIService cryptoAPIService, TradingBotService tradingBotService, MarketDataDAO marketDataDAO) {
        this.cryptoAPIService = cryptoAPIService;
        this.tradingBotService = tradingBotService;
        this.marketDataDAO = marketDataDAO;
    }

    @GetMapping("/price/{symbol}")
    public TickerPrice getCryptoPrice(@PathVariable String symbol){
        return cryptoAPIService.getPrice(symbol);
    }

    @GetMapping("/setup-data/{coinId}/{symbol}/{days}")
    public String setupHistoricalData(@PathVariable String coinId, @PathVariable String symbol, @PathVariable int days) {
        // Fetch historical data from CoinGecko
        CoinGeckoHistoricalResponse response = cryptoAPIService.getCoinGeckoHistoricalData(coinId, "usd", days);
        if (response == null || response.getPrices() == null) {
            return "Failed to fetch data from CoinGecko API.";
        }

        System.out.println("Fetched " + response.getPrices().size() + " data points from CoinGecko.");

        // Convert and save to our database
        response.getPrices().forEach(priceData -> {
            // priceData is a list: [timestamp, price]
            long timestampMillis = priceData.get(0).longValue();
            BigDecimal price = priceData.get(1);

            LocalDateTime timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestampMillis), ZoneOffset.UTC);

            marketDataDAO.save(new MarketData(symbol, price, timestamp));
        });

        return "Successfully saved " + response.getPrices().size() + " data points for " + symbol;
    }

    @GetMapping("run-backtest/{symbol}")
    public String runBacktest(@PathVariable String symbol){
        tradingBotService.runBacktest(1L, symbol);
        return "Backtest for " + symbol + " initiated. Check the application console logs and the 'trades' table in your database.";
    }

}

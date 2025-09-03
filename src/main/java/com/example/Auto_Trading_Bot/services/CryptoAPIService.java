package com.example.Auto_Trading_Bot.services;

import com.example.Auto_Trading_Bot.models.CoinGeckoHistoricalResponse;
import com.example.Auto_Trading_Bot.models.TickerPrice;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CryptoAPIService {
    private static final String BINANCE_API_URL = "https://api.binance.com/api/v3/ticker/price?symbol=";

    private final RestTemplate restTemplate = new RestTemplate();

    public TickerPrice getPrice(String symbol){
        String url = BINANCE_API_URL + symbol;

        try{
            return restTemplate.getForObject(url, TickerPrice.class);
        }catch(Exception e){
            System.err.println("Error fetching price for" + symbol + ": " + e.getMessage());
            return null;
        }
    }

    private static final String COINGECKO_API_URL = "https://api.coingecko.com/api/v3/coins/%s/market_chart?vs_currency=%s&days=%d&interval=daily";

    public CoinGeckoHistoricalResponse getCoinGeckoHistoricalData(String coinId, String vsCurrency, int days){
        String url = String.format(COINGECKO_API_URL, coinId, vsCurrency, days);

        try{
            return restTemplate.getForObject(url, CoinGeckoHistoricalResponse.class);
        }catch(Exception e){
            System.err.println("Error fetching historical data from CoinGecko for " + coinId + ": " + e.getMessage());
            return null;
        }
    }

}

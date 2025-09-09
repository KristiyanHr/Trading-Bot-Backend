package com.example.Auto_Trading_Bot.services;

import com.example.Auto_Trading_Bot.models.MarketData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TradingBotServiceTest {

    @Test
    public void testCalculateRSI_PerfectUptrend(){
        TradingBotService service = new TradingBotService(null,null,null,null,null);

        List<MarketData> data = new ArrayList<>();

        for(int i= 0; i < 14; i++){
            data.add(new MarketData("BTCUSDT", new BigDecimal(100 + (i * 10)), LocalDateTime.now()));
        }

        BigDecimal RSI = service.calculateRSI(data);

        assertEquals( 0 , new BigDecimal("100.00").compareTo(RSI), "RSI should be 100 in a perfect uptrend" );
    }
    @Test
    public void testCalculateRSI_PerfectDowntrend() {
        TradingBotService service = new TradingBotService(null, null, null, null, null);
        List<MarketData> data = new ArrayList<>();
        // Create a perfect downtrend: price goes down by 10 each day
        for (int i = 0; i < 14; i++) {
            data.add(new MarketData("BTCUSDT", new BigDecimal(200 - (i * 10)), LocalDateTime.now()));
        }

        BigDecimal rsi = service.calculateRSI(data);

        // In a perfect downtrend with no gains, RSI should be 0.
        assertEquals( 0 , BigDecimal.ZERO.compareTo(rsi), "RSI should be 0 in a perfect downtrend");
    }
}

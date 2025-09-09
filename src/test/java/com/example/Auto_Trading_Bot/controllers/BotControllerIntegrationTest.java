package com.example.Auto_Trading_Bot.controllers;

import com.example.Auto_Trading_Bot.dao.AccountDAO;
import com.example.Auto_Trading_Bot.dao.MarketDataDAO;
import com.example.Auto_Trading_Bot.models.MarketData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test; // <-- много важно
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class BotControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountDAO accountDAO;

    @Autowired
    private MarketDataDAO marketDataDAO;

    @BeforeEach
    void setUp(){
        accountDAO.updateBalance(1L, new BigDecimal("10000.00"));

        for (int i = 0; i < 50; i++) {
            BigDecimal price = ( i < 25 ) ? new BigDecimal("50000").subtract(new BigDecimal(i * 100)) : new BigDecimal("50000").add(new BigDecimal(i * 100));
            marketDataDAO.save(new MarketData("BTCUSDT", price, LocalDateTime.now().minusDays(50 - i)));
        }
    }

    @Test
    void startBacktest_executesTradesAndChecksBalance() throws Exception {
        BigDecimal balanceBefore = accountDAO.getBalance(1L);
        assertEquals(0, new BigDecimal("10000.00").compareTo(balanceBefore));

        mockMvc.perform(post("/api/bot/start-backtest/BTCUSDT"))
                .andExpect(status().isOk());

        Thread.sleep(5000);

        BigDecimal balanceAfter = accountDAO.getBalance(1L);

        assertNotEquals(0, balanceBefore.compareTo(balanceAfter), "Balance should have changed after the backtest!");
    }
}

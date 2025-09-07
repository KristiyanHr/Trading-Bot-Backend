package com.example.Auto_Trading_Bot.controllers;

import com.example.Auto_Trading_Bot.services.TradingBotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bot")
@CrossOrigin(origins = "http://localhost:5173")
public class BotController {
    private final TradingBotService tradingBotService;

    @Autowired
    public BotController(TradingBotService tradingBotService) {
        this.tradingBotService = tradingBotService;
    }

    @PostMapping("/start-backtest/{symbol}")
    public ResponseEntity<String> startBacktest(@PathVariable String symbol){
        //Pretty cool idea. Starting a new thread so we can run the test asynchronically and the api call to return immediately
        new Thread(() -> tradingBotService.runBacktest(1L, symbol)).start();
        return ResponseEntity.ok("Backtest started for symbol : " + symbol);
    }

    @PostMapping("/reset")
    public ResponseEntity<String> resetAccount(){
        tradingBotService.resetAccount(1L);
        return ResponseEntity.ok("Account has been reset to its initial state.");
    }

    @PostMapping("/start-live/{symbol}")
    public ResponseEntity<String> startLiveTrading(@PathVariable String symbol){
        new Thread(() -> tradingBotService.runLiveSimulation(1L, symbol)).start();
        return ResponseEntity.ok("Live trading simulation started for symbol: " + symbol);
    }

    @PostMapping("/stop-live")
    public ResponseEntity<String> stopLiveTrading(){
        tradingBotService.stopLiveSimulation();
        return ResponseEntity.ok("Live simulation stopping ....");
    }


}

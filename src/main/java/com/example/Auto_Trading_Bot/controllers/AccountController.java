package com.example.Auto_Trading_Bot.controllers;

import com.example.Auto_Trading_Bot.dao.AccountDAO;
import com.example.Auto_Trading_Bot.dao.PortfolioDAO;
import com.example.Auto_Trading_Bot.dao.TradeDAO;
import com.example.Auto_Trading_Bot.models.*;
import com.example.Auto_Trading_Bot.services.CryptoAPIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/account")
@CrossOrigin(origins =  "http://localhost:5173")
public class AccountController {
    private final AccountDAO accountDAO;
    private final TradeDAO tradeDAO;
    private final PortfolioDAO portfolioDAO;
    private final CryptoAPIService cryptoAPIService;

    @Autowired
    public AccountController(AccountDAO accountDAO, TradeDAO tradeDAO, PortfolioDAO portfolioDAO, CryptoAPIService cryptoAPIService) {
        this.accountDAO = accountDAO;
        this.tradeDAO = tradeDAO;
        this.portfolioDAO = portfolioDAO;
        this.cryptoAPIService = cryptoAPIService;
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountStatus> getAccountStatus(@PathVariable Long accountId){
        Optional<Account> optionalAccount = accountDAO.findBYId(accountId);
        if (optionalAccount.isEmpty()){
            return ResponseEntity.notFound().build();
        }

        Account account = optionalAccount.get();
        List<PortfolioHolding> holdings = portfolioDAO.findByAccountId(accountId);

        BigDecimal portfolioValue = BigDecimal.ZERO;

        for(PortfolioHolding holding : holdings){
            TickerPrice tickerPrice = cryptoAPIService.getPrice(holding.getSymbol());

            if (tickerPrice != null && tickerPrice.getPrice() != null){
                BigDecimal currentPrice = new BigDecimal(tickerPrice.getPrice());
                BigDecimal valueOfHolding = holding.getQuantity().multiply(currentPrice);
                portfolioValue = portfolioValue.add(valueOfHolding);
            }
        }

        AccountStatus status = new AccountStatus();
        status.setBalance(account.getBalance());
        status.setPortfolioHoldings(holdings);
        status.setTrades(tradeDAO.findByAccountId(accountId));
        status.setPortfolioValue(portfolioValue);
        status.setTotalNetWorth(portfolioValue.add(account.getBalance()));

        return ResponseEntity.ok(status);
    }

    @GetMapping("/{accountId}/trades")
    public List<Trade> getTradesByType(
            @PathVariable Long accountId,
            @RequestParam(name = "type") String simulationType
    ) {
        return tradeDAO.findByAccountIdAndType(accountId, simulationType.toUpperCase());
    }
}

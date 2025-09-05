package com.example.Auto_Trading_Bot.services;

import com.example.Auto_Trading_Bot.dao.AccountDAO;
import com.example.Auto_Trading_Bot.dao.MarketDataDAO;
import com.example.Auto_Trading_Bot.dao.PortfolioDAO;
import com.example.Auto_Trading_Bot.dao.TradeDAO;
import com.example.Auto_Trading_Bot.models.MarketData;
import com.example.Auto_Trading_Bot.models.PortfolioHolding;
import com.example.Auto_Trading_Bot.models.Trade;
import com.example.Auto_Trading_Bot.models.TradeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.error.Mark;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Service
public class TradingBotService {
    private final MarketDataDAO marketDataDAO;
    private final TradeDAO tradeDAO;
    private final AccountDAO accountDAO;
    private final PortfolioDAO portfolioDAO;


    private static final int SHORT_SMA_PERIOD = 10;
    private static final int LONG_SMA_PERIOD = 25;

    @Autowired
    public TradingBotService(AccountDAO accountDAO, MarketDataDAO marketDataDAO, TradeDAO tradeDAO, PortfolioDAO portfolioDAO) {
        this.accountDAO = accountDAO;
        this.marketDataDAO = marketDataDAO;
        this.tradeDAO = tradeDAO;
        this.portfolioDAO = portfolioDAO;
    }

    private static final BigDecimal TRADE_AMOUNT_USD = new BigDecimal("1000.00");


    public void runBacktest(Long accountId, String symbol){
        System.out.print("Starting backtest for symbol: " + symbol);
        List<MarketData> historicalData = marketDataDAO.findBySymbolOrderByTimestampAsc(symbol);

        if(historicalData.size() < LONG_SMA_PERIOD){
            System.err.println("Not enough historical data to run the backtest");
            return;
        }
        BigDecimal accountBalance = accountDAO.getBalance(accountId);
        Optional<PortfolioHolding> initialHolding = portfolioDAO.findByAccountIdAndSymbol(accountId, symbol);

        BigDecimal cryptoHoldings = initialHolding.map(PortfolioHolding::getQuantity).orElse(BigDecimal.ZERO);

        BigDecimal previousShortSma = null;
        BigDecimal previousLongSma = null;

        for (int i = LONG_SMA_PERIOD; i < historicalData.size(); i++) {
            BigDecimal currentShortSma = calculateSma(historicalData.subList(i - SHORT_SMA_PERIOD, i));
            BigDecimal currentLongSma = calculateSma(historicalData.subList(i - LONG_SMA_PERIOD, i));

            MarketData currentMarketData = historicalData.get(i);
            if (previousShortSma != null) {
                if (previousShortSma.compareTo(previousLongSma) <= 0 && currentShortSma.compareTo(currentLongSma) > 0){
                    if (accountBalance.compareTo(TRADE_AMOUNT_USD) >= 0){

                        BigDecimal quantityToBuy = TRADE_AMOUNT_USD.divide(currentMarketData.getPrice(), 8, RoundingMode.DOWN);

                        accountBalance = accountBalance.subtract(TRADE_AMOUNT_USD);
                        cryptoHoldings = cryptoHoldings.add(quantityToBuy);

                        accountDAO.updateBalance(accountId, accountBalance);
                        PortfolioHolding holding = initialHolding.orElseGet(() -> new PortfolioHolding(accountId, symbol));
                        holding.setQuantity(cryptoHoldings);
                        holding.setAverageBuyPrice(currentMarketData.getPrice());

                        portfolioDAO.saveOrUpdate(holding);

                        tradeDAO.save(createTrade(accountId, symbol, TradeType.BUY, quantityToBuy, currentMarketData));
                        System.out.println("BUY " + quantityToBuy + " " + symbol + " at " + currentMarketData.getPrice());
                    }
                } else if (previousShortSma.compareTo(previousLongSma) >=0 && currentShortSma.compareTo(currentLongSma) < 0) {
                    if (cryptoHoldings.compareTo(BigDecimal.ZERO) > 0){
                        BigDecimal amountSoldUsd = cryptoHoldings.multiply(currentMarketData.getPrice());

                        accountBalance = accountBalance.add(amountSoldUsd);

                        accountDAO.updateBalance(accountId, accountBalance);
                        portfolioDAO.findByAccountIdAndSymbol(accountId, symbol).ifPresent(h -> portfolioDAO.delete(h.getId()));

                        tradeDAO.save(createTrade(accountId, symbol, TradeType.SELL, cryptoHoldings, currentMarketData));
                        System.out.println("SELL " + cryptoHoldings + " " + symbol + " at " + currentMarketData.getPrice());

                        cryptoHoldings = BigDecimal.ZERO;
                    }
                }
            }
            previousShortSma = currentShortSma;
            previousLongSma = currentLongSma;
        }
        System.out.println("Backtest finished. Final Balance: " + accountBalance);
    }

    private BigDecimal calculateSma(List<MarketData> data){
        BigDecimal sum = data.stream()
                .map(MarketData::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(new BigDecimal(data.size()),8,RoundingMode.HALF_UP);
    }

    private Trade createTrade(Long accountId, String symbol, TradeType type, BigDecimal quantity, MarketData data){
        Trade trade = new Trade();
        trade.setAccountId(accountId);
        trade.setSymbol(symbol);
        trade.setTradeType(type);
        trade.setQuantity(quantity);
        trade.setPrice(data.getPrice());
        trade.setTimestamp(data.getTimestamp());

        return trade;
    }

    public void resetAccount(Long accountId){
        System.out.println("Resetting account: " + accountId);

        accountDAO.updateBalance(accountId, new BigDecimal("10000.00"));

        tradeDAO.deleteByAccountId(accountId);
        portfolioDAO.deleteByAccountId(accountId);
        System.out.println("Account has been reset successfully!");
    }

}

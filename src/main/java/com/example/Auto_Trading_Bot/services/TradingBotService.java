package com.example.Auto_Trading_Bot.services;

import com.example.Auto_Trading_Bot.dao.AccountDAO;
import com.example.Auto_Trading_Bot.dao.MarketDataDAO;
import com.example.Auto_Trading_Bot.dao.PortfolioDAO;
import com.example.Auto_Trading_Bot.dao.TradeDAO;
import com.example.Auto_Trading_Bot.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.error.Mark;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TradingBotService {
    private final MarketDataDAO marketDataDAO;
    private final TradeDAO tradeDAO;
    private final AccountDAO accountDAO;
    private final PortfolioDAO portfolioDAO;
    private final CryptoAPIService cryptoAPIService;
    private volatile boolean isLiveTradingRunning = false;


    private static final int SHORT_SMA_PERIOD = 10;
    private static final int LONG_SMA_PERIOD = 25;

    @Autowired
    public TradingBotService(AccountDAO accountDAO, MarketDataDAO marketDataDAO, TradeDAO tradeDAO, PortfolioDAO portfolioDAO, CryptoAPIService cryptoAPIService) {
        this.accountDAO = accountDAO;
        this.marketDataDAO = marketDataDAO;
        this.tradeDAO = tradeDAO;
        this.portfolioDAO = portfolioDAO;
        this.cryptoAPIService = cryptoAPIService;
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


    public void runLiveSimulation(Long accountId, String symbol){
        if (isLiveTradingRunning){
            System.out.println("Live Trading is already running!");
            return;
        }

        isLiveTradingRunning = true;
        System.out.println("Starting Live simulation for symbol: " + symbol);

        List<MarketData> historicalData = new ArrayList<>(marketDataDAO.findBySymbolOrderByTimestampAsc(symbol));

        if (historicalData.size() < LONG_SMA_PERIOD){
            System.err.println("Not enough historical data to calculate indicators for live trading, please run setup first.");
            stopLiveSimulation();
            return;
        }

        while (isLiveTradingRunning){
            try{
                TickerPrice liveTicker = cryptoAPIService.getPrice(symbol);
                if (liveTicker == null || liveTicker.getPrice() == null){
                    System.err.println("Could not fetch live price. Skipping this iteration");
                    Thread.sleep(10000);
                    continue;
                }

                MarketData liveMarketData = new MarketData(symbol, new BigDecimal(liveTicker.getPrice()), LocalDateTime.now());

                // 3. Calculate SMAs using historical data

                List<MarketData> shortSmaData = historicalData.subList(historicalData.size() - SHORT_SMA_PERIOD, historicalData.size());
                List<MarketData> longSmaData = historicalData.subList(historicalData.size() - LONG_SMA_PERIOD, historicalData.size());
                BigDecimal previousShortSma = calculateSma(shortSmaData);
                BigDecimal previousLongSma = calculateSma(longSmaData);

                List<MarketData> newHistoricalData = new ArrayList<>(historicalData);
                newHistoricalData.add(liveMarketData);

                // 4. Calculate what the *new* SMAs would be if we included the live price

                List<MarketData> newShortSmaData = newHistoricalData.subList(newHistoricalData.size() - SHORT_SMA_PERIOD, newHistoricalData.size());
                List<MarketData> newLongSmaData = newHistoricalData.subList(newHistoricalData.size() - LONG_SMA_PERIOD, newHistoricalData.size());

                BigDecimal currentShortSma = calculateSma(newShortSmaData);
                BigDecimal currentLongSma = calculateSma(newLongSmaData);

                // 5. Apply the exact same Crossover Logic as the backtest

                BigDecimal currentBalance = accountDAO.getBalance(accountId);
                Optional<PortfolioHolding> holdingOpt = portfolioDAO.findByAccountIdAndSymbol(accountId, symbol);

                //BUY SIGNALS
                if (previousShortSma.compareTo(previousLongSma) <=0 && currentShortSma.compareTo(currentLongSma) > 0){
                    if (currentBalance.compareTo(TRADE_AMOUNT_USD) >= 0){

                        System.out.println("live BUY signal detected!");
                        BigDecimal quantityToBuy = TRADE_AMOUNT_USD.divide(liveMarketData.getPrice(), 8, RoundingMode.DOWN);

                        BigDecimal newBalance = currentBalance.subtract(TRADE_AMOUNT_USD);
                        BigDecimal newHoldings = holdingOpt.map(PortfolioHolding::getQuantity).orElse(BigDecimal.ZERO).add(quantityToBuy);

                        accountDAO.updateBalance(accountId, newBalance);
                        PortfolioHolding holdingToSave = holdingOpt.orElseGet(() -> new PortfolioHolding(accountId, symbol));
                        holdingToSave.setQuantity(newHoldings);
                        holdingToSave.setAverageBuyPrice(liveMarketData.getPrice());

                        portfolioDAO.saveOrUpdate(holdingToSave);

                        tradeDAO.save(createTrade(accountId, symbol, TradeType.BUY, quantityToBuy, liveMarketData));
                        System.out.println("BUY " + quantityToBuy + " " + symbol + " at " + liveMarketData.getPrice());
                    }
                } else if (previousShortSma.compareTo(previousLongSma) >= 0 && currentShortSma.compareTo(currentLongSma) < 0) {
                    if (holdingOpt.isPresent() && holdingOpt.get().getQuantity().compareTo(BigDecimal.ZERO) > 0){

                        System.out.println("LIVE SELL SIGNAL DETECTED!");
                        PortfolioHolding currentHolding = holdingOpt.get();
                        BigDecimal quantityToSell = currentHolding.getQuantity();
                        BigDecimal amountSoldUsd = quantityToSell.multiply(liveMarketData.getPrice());

                        java.math.BigDecimal newBalance = currentBalance.add(amountSoldUsd);

                        accountDAO.updateBalance(accountId, newBalance);
                        portfolioDAO.delete(currentHolding.getId());

                        tradeDAO.save(createTrade(accountId, symbol, TradeType.SELL, quantityToSell, liveMarketData));
                        System.out.println("SELL " + quantityToSell + " " + symbol + " at " + liveMarketData.getPrice());

                    }
                }

                historicalData.add(liveMarketData);
                historicalData.remove(0);

                System.out.println("Live check complete. Current price: "
                        + liveMarketData.getPrice() + ". Waiting for next interval...");
                Thread.sleep(15000);

            }catch (InterruptedException e){
                Thread.currentThread().interrupt();
                isLiveTradingRunning = false;
                System.out.println("Live trading thread interrupted");
            }catch (Exception e){
                System.err.println("An error has occurred during live simulation! " + e.getMessage());
                e.printStackTrace();
                try{
                    Thread.sleep(30000);
                }catch (InterruptedException ie){
                    isLiveTradingRunning = false;
                }
            }
        }
        System.out.println("Live simulation STOPPED for symbol: " + symbol);
    }

    public void stopLiveSimulation(){
        this.isLiveTradingRunning = false;
    }

}

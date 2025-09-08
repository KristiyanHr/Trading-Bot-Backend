package com.example.Auto_Trading_Bot.services;

import com.example.Auto_Trading_Bot.dao.AccountDAO;
import com.example.Auto_Trading_Bot.dao.MarketDataDAO;
import com.example.Auto_Trading_Bot.dao.PortfolioDAO;
import com.example.Auto_Trading_Bot.dao.TradeDAO;
import com.example.Auto_Trading_Bot.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private static final int RSI_PERIOD = 14;


    private static final int SHORT_SMA_PERIOD = 2;
    private static final int LONG_SMA_PERIOD = 3;

    @Autowired
    public TradingBotService(AccountDAO accountDAO, MarketDataDAO marketDataDAO, TradeDAO tradeDAO, PortfolioDAO portfolioDAO, CryptoAPIService cryptoAPIService) {
        this.accountDAO = accountDAO;
        this.marketDataDAO = marketDataDAO;
        this.tradeDAO = tradeDAO;
        this.portfolioDAO = portfolioDAO;
        this.cryptoAPIService = cryptoAPIService;
    }

    private static final BigDecimal TRADE_PERCENTAGE = new BigDecimal("0.50");


    public void runBacktest(Long accountId, String symbol) {
        System.out.println("Starting final backtest with dynamic sizing and PnL for symbol: " + symbol);
        tradeDAO.deleteByAccountIdAndType(accountId, "BACKTEST");

        List<MarketData> historicalData = marketDataDAO.findBySymbolOrderByTimestampAsc(symbol);

        tradeDAO.deleteByAccountIdAndType(accountId, "BACKTEST");

        int requiredDataPoints = Math.max(LONG_SMA_PERIOD, RSI_PERIOD);
        if (historicalData.size() < requiredDataPoints) {
            System.err.println("Not enough historical data to run the backtest.");
            return;
        }

        BigDecimal accountBalance = accountDAO.getBalance(accountId);
        Optional<PortfolioHolding> holdingOpt = portfolioDAO.findByAccountIdAndSymbol(accountId, symbol);

        BigDecimal cryptoHoldings = holdingOpt.map(PortfolioHolding::getQuantity).orElse(BigDecimal.ZERO);
        BigDecimal averageBuyPrice = holdingOpt.map(PortfolioHolding::getAverageBuyPrice).orElse(BigDecimal.ZERO);

        BigDecimal previousShortSma = null;
        BigDecimal previousLongSma = null;

        // Start loop at the first point where we have enough data for all indicators
        for (int i = requiredDataPoints; i < historicalData.size(); i++) {
            MarketData currentMarketData = historicalData.get(i);
            BigDecimal currentShortSma = calculateSma(historicalData.subList(i - SHORT_SMA_PERIOD, i));
            BigDecimal currentLongSma = calculateSma(historicalData.subList(i - LONG_SMA_PERIOD, i));
            BigDecimal currentRsi = calculateRSI(historicalData.subList(i - RSI_PERIOD, i));

            if (previousShortSma != null) {
                // --- BUY SIGNAL ---
                if (previousShortSma.compareTo(previousLongSma) <= 0 && currentShortSma.compareTo(currentLongSma) > 0) {
                    if (currentRsi.compareTo(new BigDecimal("70")) < 0) { // RSI confirmation

                        // <<< FIX: Calculate trade size dynamically as a percentage of balance
                        BigDecimal tradeAmountUsd = accountBalance.multiply(TRADE_PERCENTAGE);

                        // <<< FIX: Check if balance is sufficient and greater than a minimum amount (e.g., $10)
                        if (accountBalance.compareTo(tradeAmountUsd) >= 0 && accountBalance.compareTo(BigDecimal.TEN) > 0) {
                            System.out.println("RSI Confirmation OK for BUY: " + currentRsi);
                            BigDecimal quantityToBuy = tradeAmountUsd.divide(currentMarketData.getPrice(), 8, RoundingMode.DOWN);

                            // <<< FIX: Correctly calculate the new average buy price
                            BigDecimal totalQuantity = cryptoHoldings.add(quantityToBuy);
                            BigDecimal totalCost = averageBuyPrice.multiply(cryptoHoldings).add(tradeAmountUsd);
                            averageBuyPrice = totalCost.divide(totalQuantity, 8, RoundingMode.HALF_UP);

                            accountBalance = accountBalance.subtract(tradeAmountUsd);
                            cryptoHoldings = totalQuantity;

                            accountDAO.updateBalance(accountId, accountBalance);
                            PortfolioHolding holdingToSave = holdingOpt.orElseGet(() -> new PortfolioHolding(accountId, symbol));
                            holdingToSave.setQuantity(cryptoHoldings);
                            holdingToSave.setAverageBuyPrice(averageBuyPrice); // <<< FIX: Save the new calculated average price
                            portfolioDAO.saveOrUpdate(holdingToSave);

                            Trade buyTrade = createTrade(accountId, symbol, TradeType.BUY, quantityToBuy, currentMarketData);
                            buyTrade.setSimulationType("BACKTEST");
                            tradeDAO.save(buyTrade);

                            System.out.println("BUY: " + quantityToBuy + " " + symbol + " at " + currentMarketData.getPrice());
                        }
                    } else {
                        System.out.println("SMA Buy Signal IGNORED. RSI is overbought: " + currentRsi);
                    }
                }
                // --- SELL SIGNAL ---
                else if (previousShortSma.compareTo(previousLongSma) >= 0 && currentShortSma.compareTo(currentLongSma) < 0) {
                    if (currentRsi.compareTo(new BigDecimal("30")) > 0) { // RSI confirmation
                        if (cryptoHoldings.compareTo(BigDecimal.ZERO) > 0) {
                            System.out.println("RSI Confirmation OK for SELL: " + currentRsi);
                            BigDecimal amountSoldUsd = cryptoHoldings.multiply(currentMarketData.getPrice());

                            BigDecimal costOfGoodsSold = averageBuyPrice.multiply(cryptoHoldings);
                            BigDecimal pnl = amountSoldUsd.subtract(costOfGoodsSold);

                            accountBalance = accountBalance.add(amountSoldUsd);

                            accountDAO.updateBalance(accountId, accountBalance);
                            portfolioDAO.findByAccountIdAndSymbol(accountId, symbol).ifPresent(h -> portfolioDAO.delete(h.getId()));

                            Trade sellTrade = createTrade(accountId, symbol, TradeType.SELL, cryptoHoldings, currentMarketData);
                            sellTrade.setPnl(pnl);
                            sellTrade.setSimulationType("BACKTEST");
                            tradeDAO.save(sellTrade);

                            // <<< FIX: Removed the duplicate tradeDAO.save() call
                            System.out.println("SELL: " + cryptoHoldings + " " + symbol + " at " + currentMarketData.getPrice() + " | PnL: " + pnl);

                            // <<< FIX: Reset crypto holdings and average buy price after selling
                            cryptoHoldings = BigDecimal.ZERO;
                            averageBuyPrice = BigDecimal.ZERO;
                        }
                    } else {
                        System.out.println("SMA Sell Signal IGNORED. RSI is oversold: " + currentRsi);
                    }
                }
            }
            previousShortSma = currentShortSma;
            previousLongSma = currentLongSma;
        }
        System.out.println("Backtest finished. Final Balance: " + accountBalance);

    }

    private BigDecimal calculateSma(List<MarketData> data) {
        BigDecimal sum = data.stream()
                .map(MarketData::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(new BigDecimal(data.size()), 8, RoundingMode.HALF_UP);
    }

    private Trade createTrade(Long accountId, String symbol, TradeType type, BigDecimal quantity, MarketData data) {
        Trade trade = new Trade();
        trade.setAccountId(accountId);
        trade.setSymbol(symbol);
        trade.setTradeType(type);
        trade.setQuantity(quantity);
        trade.setPrice(data.getPrice());
        trade.setTimestamp(data.getTimestamp());

        return trade;
    }

    public void resetAccount(Long accountId) {
        System.out.println("Resetting account: " + accountId);

        accountDAO.updateBalance(accountId, new BigDecimal("10000.00"));

        tradeDAO.deleteByAccountId(accountId);
        portfolioDAO.deleteByAccountId(accountId);
        System.out.println("Account has been reset successfully!");
    }


    public void runLiveSimulation(Long accountId, String symbol) {
        if (isLiveTradingRunning) {
            System.out.println("Live Trading is already running!");
            return;
        }
        isLiveTradingRunning = true;
        System.out.println("Starting final live simulation with dynamic sizing and PnL for symbol: " + symbol);

        List<MarketData> historicalData = new ArrayList<>(marketDataDAO.findBySymbolOrderByTimestampAsc(symbol));

        while (isLiveTradingRunning) {
            try {
                int requiredDataPoints = Math.max(LONG_SMA_PERIOD, RSI_PERIOD) + 1;
                if (historicalData.size() < requiredDataPoints) {
                    System.err.println("Not enough historical data for live trading. Stopping simulation.");
                    stopLiveSimulation();
                    return;
                }

                // 1. Calculate PREVIOUS state
                BigDecimal previousShortSma = calculateSma(historicalData.subList(historicalData.size() - SHORT_SMA_PERIOD, historicalData.size()));
                BigDecimal previousLongSma = calculateSma(historicalData.subList(historicalData.size() - LONG_SMA_PERIOD, historicalData.size()));
                BigDecimal previousRsi = calculateRSI(historicalData.subList(historicalData.size() - RSI_PERIOD, historicalData.size()));

                // 2. Fetch LIVE price
                TickerPrice liveTicker = cryptoAPIService.getPrice(symbol);
                if (liveTicker == null || liveTicker.getPrice() == null) {
                    System.err.println("Could not fetch live price. Skipping iteration.");
                    Thread.sleep(10000);
                    continue;
                }
                MarketData liveMarketData = new MarketData(symbol, new BigDecimal(liveTicker.getPrice()), LocalDateTime.now());

                // 3. Update sliding window
                historicalData.add(liveMarketData);
                historicalData.remove(0);

                // 4. Calculate CURRENT state
                BigDecimal currentShortSma = calculateSma(historicalData.subList(historicalData.size() - SHORT_SMA_PERIOD, historicalData.size()));
                BigDecimal currentLongSma = calculateSma(historicalData.subList(historicalData.size() - LONG_SMA_PERIOD, historicalData.size()));

                // 5. Get latest account info from DB for this iteration
                BigDecimal currentBalance = accountDAO.getBalance(accountId);
                Optional<PortfolioHolding> holdingOpt = portfolioDAO.findByAccountIdAndSymbol(accountId, symbol);

                // --- BUY SIGNAL ---
                if (previousShortSma.compareTo(previousLongSma) <= 0 && currentShortSma.compareTo(currentLongSma) > 0) {
                    if (previousRsi.compareTo(new BigDecimal("70")) < 0) {

                        BigDecimal tradeAmountUsd = currentBalance.multiply(TRADE_PERCENTAGE);

                        if (currentBalance.compareTo(tradeAmountUsd) >= 0 && currentBalance.compareTo(BigDecimal.TEN) > 0) {
                            System.out.println("LIVE RSI Confirmation OK for BUY: " + previousRsi);
                            BigDecimal quantityToBuy = tradeAmountUsd.divide(liveMarketData.getPrice(), 8, RoundingMode.DOWN);

                            BigDecimal currentHoldings = holdingOpt.map(PortfolioHolding::getQuantity).orElse(BigDecimal.ZERO);
                            BigDecimal currentAvgPrice = holdingOpt.map(PortfolioHolding::getAverageBuyPrice).orElse(BigDecimal.ZERO);

                            BigDecimal totalQuantity = currentHoldings.add(quantityToBuy);
                            BigDecimal totalCost = currentAvgPrice.multiply(currentHoldings).add(tradeAmountUsd);
                            BigDecimal newAverageBuyPrice = totalCost.divide(totalQuantity, 8, RoundingMode.HALF_UP);

                            BigDecimal newBalance = currentBalance.subtract(tradeAmountUsd);

                            accountDAO.updateBalance(accountId, newBalance);
                            PortfolioHolding holdingToSave = holdingOpt.orElseGet(() -> new PortfolioHolding(accountId, symbol));
                            holdingToSave.setQuantity(totalQuantity);
                            holdingToSave.setAverageBuyPrice(newAverageBuyPrice);
                            portfolioDAO.saveOrUpdate(holdingToSave);

                            Trade buyTrade = createTrade(accountId, symbol, TradeType.BUY, quantityToBuy, liveMarketData);
                            buyTrade.setSimulationType("LIVE");
                            tradeDAO.save(buyTrade);
                            System.out.println("EXECUTED LIVE BUY: " + quantityToBuy + " " + symbol + " at " + liveMarketData.getPrice());
                        }
                    }
                }
                // --- SELL SIGNAL ---
                else if (previousShortSma.compareTo(previousLongSma) >= 0 && currentShortSma.compareTo(currentLongSma) < 0) {
                    if (previousRsi.compareTo(new BigDecimal("30")) > 0) {
                        if (holdingOpt.isPresent() && holdingOpt.get().getQuantity().compareTo(BigDecimal.ZERO) > 0) {
                            System.out.println("LIVE RSI Confirmation OK for SELL: " + previousRsi);
                            PortfolioHolding currentHolding = holdingOpt.get();
                            BigDecimal quantityToSell = currentHolding.getQuantity();
                            BigDecimal amountSoldUsd = quantityToSell.multiply(liveMarketData.getPrice());

                            BigDecimal costOfGoodsSold = currentHolding.getAverageBuyPrice().multiply(quantityToSell);
                            BigDecimal pnl = amountSoldUsd.subtract(costOfGoodsSold);

                            BigDecimal newBalance = currentBalance.add(amountSoldUsd);

                            accountDAO.updateBalance(accountId, newBalance);
                            portfolioDAO.delete(currentHolding.getId());

                            Trade sellTrade = createTrade(accountId, symbol, TradeType.SELL, quantityToSell, liveMarketData);
                            sellTrade.setPnl(pnl);
                            sellTrade.setSimulationType("LIVE");
                            tradeDAO.save(sellTrade);

                            System.out.println("EXECUTED LIVE SELL: " + quantityToSell + " " + symbol + " at " + liveMarketData.getPrice() + " | PnL: " + pnl);
                        }
                    } else {
                        System.out.println("SMA Live Sell Signal IGNORED. RSI is oversold: " + previousRsi);
                    }
                }

                // 6. Wait for the next cycle
                System.out.println("Live check complete. Current price: " + liveMarketData.getPrice() + ". RSI: " + previousRsi + ". Waiting...");
                Thread.sleep(15000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                isLiveTradingRunning = false;
                System.out.println("Live trading thread interrupted");
            } catch (Exception e) {
                System.err.println("An error occurred during live simulation: " + e.getMessage());
                e.printStackTrace();
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException ie) {
                    isLiveTradingRunning = false;
                }
            }
        }
        System.out.println("Live simulation STOPPED for symbol: " + symbol);
    }

    public void stopLiveSimulation() {
        this.isLiveTradingRunning = false;
    }

    public BigDecimal calculateRSI(List<MarketData> data) {

        if (data == null || data.size() < RSI_PERIOD) {
            return new BigDecimal("50");
        }
        BigDecimal sumOfGains = BigDecimal.ZERO;
        BigDecimal sumOfLosses = BigDecimal.ZERO;

        for (int i = 1; i < data.size(); i++) {
            BigDecimal previousPrice = data.get(i - 1).getPrice();
            BigDecimal currentPrice = data.get(i).getPrice();
            BigDecimal difference = data.get(i).getPrice().subtract(data.get(i - 1).getPrice());

            if (difference.compareTo(BigDecimal.ZERO) > 0) {
                sumOfGains = sumOfGains.add(difference);
            } else {
                sumOfLosses = sumOfLosses.add(difference.abs());
            }
        }

        if (sumOfLosses.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("100");
        }

        BigDecimal averageGain = sumOfGains.divide(new BigDecimal(RSI_PERIOD), 8, RoundingMode.HALF_UP);
        BigDecimal averageLoss = sumOfLosses.divide(new BigDecimal(RSI_PERIOD), 8, RoundingMode.HALF_UP);

        BigDecimal relativeStrength = averageGain.divide(averageLoss, 8, RoundingMode.HALF_UP);

        BigDecimal RSI = new BigDecimal("100").subtract(
                new BigDecimal("100").divide(BigDecimal.ONE.add(relativeStrength), 8, RoundingMode.HALF_UP));

        return RSI;
    }

}

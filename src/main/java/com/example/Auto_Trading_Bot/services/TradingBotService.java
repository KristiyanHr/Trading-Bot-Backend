package com.example.Auto_Trading_Bot.services;

import com.example.Auto_Trading_Bot.dao.MarketDataDAO;
import com.example.Auto_Trading_Bot.dao.TradeDAO;
import com.example.Auto_Trading_Bot.models.MarketData;
import com.example.Auto_Trading_Bot.models.Trade;
import com.example.Auto_Trading_Bot.models.TradeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.error.Mark;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.List;

@Service
public class TradingBotService {
    private final MarketDataDAO marketDataDAO;
    private final TradeDAO tradeDAO;

    private static final int SHORT_SMA_PERIOD = 10;
    private static final int LONG_SMA_PERIOD = 25;
    private static final BigDecimal TRADE_AMOUNT_USD = new BigDecimal("1000.00");

    @Autowired
    public TradingBotService(MarketDataDAO marketDataDAO, TradeDAO tradeDAO) {
        this.marketDataDAO = marketDataDAO;
        this.tradeDAO = tradeDAO;
    }

    public void runBacktest(Long accountId, String symbol){
        System.out.print("Starting backtest for symbol: " + symbol);
        List<MarketData> historicalData = marketDataDAO.findBySymbolOrderByTimestampAsc(symbol);

        if(historicalData.size() < LONG_SMA_PERIOD){
            System.err.println("Not enough historical data to run the backtest");
            return;
        }
        BigDecimal cryptoHoldings = BigDecimal.ZERO;
        BigDecimal previousShortSma = null;
        BigDecimal previousLongSma = null;

        for (int i = LONG_SMA_PERIOD; i < historicalData.size(); i++) {
            List<MarketData>shortSmaData = historicalData.subList(i - SHORT_SMA_PERIOD, i);
            List<MarketData>longSmaData = historicalData.subList(i - LONG_SMA_PERIOD, i);

            BigDecimal currentShortSma = calculateSma(shortSmaData);
            BigDecimal currentLongSma = calculateSma(longSmaData);
            MarketData currentMarketData = historicalData.get(i);

            if (previousShortSma != null){
                if(previousShortSma.compareTo(previousLongSma) <= 0 && currentShortSma.compareTo(currentLongSma) > 0){
                    if (cryptoHoldings.compareTo(BigDecimal.ZERO) == 0){
                        BigDecimal quantityToBuy = TRADE_AMOUNT_USD.divide(currentMarketData.getPrice(),8, RoundingMode.DOWN);
                        cryptoHoldings = cryptoHoldings.add(quantityToBuy);

                        //Create and save the trade

                        Trade buyTrade = createTrade(accountId, symbol, TradeType.BUY, quantityToBuy, currentMarketData);
                        tradeDAO.save(buyTrade);
                        System.out.println("Buy signal at " + currentMarketData.getTimestamp() +
                                " Price: " + currentMarketData.getPrice());
                    }
                } else if (previousShortSma.compareTo(previousLongSma) >= 0 && currentShortSma.compareTo(currentLongSma) < 0){
                    if (cryptoHoldings.compareTo(BigDecimal.ZERO) > 0){

                        Trade sellTrade = createTrade(accountId,symbol, TradeType.SELL, cryptoHoldings, currentMarketData);
                        tradeDAO.save(sellTrade);

                        System.out.println("Sell signal at " + currentMarketData.getTimestamp() +
                                " Price: " + currentMarketData.getPrice());

                        cryptoHoldings = BigDecimal.ZERO;
                    }
                }
            }
            previousShortSma = currentShortSma;
            previousLongSma = currentLongSma;
        }
        System.out.println("Backtest finished!");
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

}

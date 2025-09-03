package com.example.Auto_Trading_Bot.dao;

import com.example.Auto_Trading_Bot.models.MarketData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class MarketDataDAO {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public MarketDataDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(MarketData marketData){
        String sql = "INSERT INTO market_data (symbol, price, timestamp) VALUES (?,?,?)";
        jdbcTemplate.update(sql,marketData.getSymbol(), marketData.getPrice(), Timestamp.valueOf(marketData.getTimestamp()));
    }

    public List<MarketData> findBySymbolOrderByTimestampAsc(String symbol){
        String sql = "SELECT symbol, price, timestamp FROM market_data WHERE symbol = ? ORDER BY timestamp ASC";

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new MarketData(
                        rs.getString("symbol"),
                        rs.getBigDecimal("price"),
                        rs.getTimestamp("timestamp").toLocalDateTime()
                ),symbol);
    }
}

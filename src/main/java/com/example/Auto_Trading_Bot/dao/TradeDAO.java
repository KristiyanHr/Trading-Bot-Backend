package com.example.Auto_Trading_Bot.dao;

import com.example.Auto_Trading_Bot.models.Trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
public class TradeDAO {
    private final JdbcTemplate jdbcTemplate;


    @Autowired
    public TradeDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(Trade trade){
        String sql = "INSERT INTO trades (account_id, symbol, trade_type, quantity, price, timestamp, pnl) VALUES (?,?,?,?,?,?,?) ";

        jdbcTemplate.update(
                sql,
                trade.getAccountId(),
                trade.getSymbol(),
                trade.getTradeType().name(),
                trade.getQuantity(),
                trade.getPrice(),
                Timestamp.valueOf(trade.getTimestamp()),
                trade.getPnl()
        );
    }

    public void deleteByAccountId(Long accountId){
        String sql = "DELETE FROM trades WHERE account_id = ?";
        jdbcTemplate.update(sql, accountId);
    }
}

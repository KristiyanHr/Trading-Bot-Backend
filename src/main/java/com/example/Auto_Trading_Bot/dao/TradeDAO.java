package com.example.Auto_Trading_Bot.dao;

import com.example.Auto_Trading_Bot.models.Trade;
import com.example.Auto_Trading_Bot.models.TradeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class TradeDAO {
    private final JdbcTemplate jdbcTemplate;


    @Autowired
    public TradeDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private Trade mapRowToTrade(ResultSet rs, int rowNum) throws SQLException {
        Trade trade = new Trade();
        trade.setId(rs.getLong("id"));
        trade.setAccountId(rs.getLong("account_id"));
        trade.setSymbol(rs.getString("symbol"));
        trade.setTradeType(TradeType.valueOf(rs.getString("trade_type")));
        trade.setQuantity(rs.getBigDecimal("quantity"));
        trade.setPrice(rs.getBigDecimal("price"));
        trade.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());

        BigDecimal pnl = rs.getBigDecimal("pnl");
        trade.setPnl(pnl);

        return trade;
    }

    public void save(Trade trade){
        String sql = "INSERT INTO trades (account_id, symbol, trade_type, simulation_type, quantity, price, timestamp, pnl) VALUES (?,?,?,?,?,?,?,?) ";

        jdbcTemplate.update(
                sql,
                trade.getAccountId(),
                trade.getSymbol(),
                trade.getTradeType().name(),
                trade.getSimulationType(),
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

    public List<Trade> findByAccountId(Long accountId) {
        String sql = "SELECT * FROM trades WHERE account_id = ? ORDER BY timestamp DESC";

        return jdbcTemplate.query(sql, this::mapRowToTrade, accountId);
    }

    public void deleteByAccountIdAndType(Long accountId, String simulationType) {
        String sql = "DELETE FROM trades WHERE account_id = ? AND simulation_type = ?";
        jdbcTemplate.update(sql, accountId, simulationType);
    }

    public List<Trade> findByAccountIdAndType(Long accountId, String simulationType) {
        String sql = "SELECT * FROM trades WHERE account_id = ? AND simulation_type = ? ORDER BY timestamp DESC";
        return jdbcTemplate.query(sql, this::mapRowToTrade, accountId, simulationType);
    }
}

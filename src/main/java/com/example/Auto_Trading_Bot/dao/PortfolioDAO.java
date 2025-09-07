package com.example.Auto_Trading_Bot.dao;

import com.example.Auto_Trading_Bot.models.PortfolioHolding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class PortfolioDAO {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public PortfolioDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<PortfolioHolding> findByAccountIdAndSymbol(Long accountId, String symbol){
        String sql = "SELECT * FROM portfolio_holdings WHERE account_id = ? AND symbol = ?";

        try{
            PortfolioHolding holding = jdbcTemplate.queryForObject(sql, this::mapRowToHolding, accountId, symbol);
            return Optional.ofNullable(holding);
        }catch(EmptyResultDataAccessException e){
            return Optional.empty();
        }
    }

    private PortfolioHolding mapRowToHolding(ResultSet rs, int rowNum) throws SQLException {
        PortfolioHolding holding = new PortfolioHolding();
        holding.setId(rs.getLong("id"));
        holding.setAccountId(rs.getLong("account_id"));
        holding.setSymbol(rs.getString("symbol"));
        holding.setQuantity(rs.getBigDecimal("quantity"));
        holding.setAverageBuyPrice(rs.getBigDecimal("average_buy_price"));
        return holding;
    }

    public void saveOrUpdate(PortfolioHolding holding){
        String sql = "INSERT INTO portfolio_holdings(account_id, symbol, quantity, average_buy_price) VALUES (?,?,?,?)"
                + "ON DUPLICATE KEY UPDATE quantity = ?, average_buy_price = ?";

        jdbcTemplate.update(sql,
                holding.getAccountId(),
                holding.getSymbol(),
                holding.getQuantity(),
                holding.getAverageBuyPrice(),
                //VALUES for ON DUPLICATE KEY UPDATE
                holding.getQuantity(),
                holding.getAverageBuyPrice());
    }

    public void delete(Long holdingId){
        String sql = "Delete FROM portfolio_holdings WHERE id = ?";
        jdbcTemplate.update(sql, holdingId);
    }

    public void deleteByAccountId(Long accountId){
        String sql = "DELETE FROM portfolio_holdings WHERE account_id = ?";
        jdbcTemplate.update(sql, accountId);
    }

    public List<PortfolioHolding> findByAccountId(Long accountId){
        String sql = "SELECT * FROM portfolio_holdings WHERE account_id = ?";
        return jdbcTemplate.query(sql, this::mapRowToHolding, accountId);
    }
}

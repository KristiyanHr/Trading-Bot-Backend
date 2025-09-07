package com.example.Auto_Trading_Bot.dao;

import com.example.Auto_Trading_Bot.models.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class AccountDAO {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public AccountDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private Account mapRowToAccount(ResultSet rs, int rowNum) throws SQLException {
        Account account = new Account();
        account.setId(rs.getLong("id"));
        account.setUserId(rs.getLong("user_id"));
        account.setBalance(rs.getBigDecimal("balance"));
        account.setCurrency(rs.getString("currency"));
        account.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        account.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return account;
    }

    public Optional<Account> findBYId(Long accountId){
        String sql = "SELECT * FROM accounts WHERE id = ?";

        try{
            Account account = jdbcTemplate.queryForObject(sql, this::mapRowToAccount, accountId);
            return Optional.ofNullable(account);
        }catch (EmptyResultDataAccessException e){
            return Optional.empty();
        }
    }

    public BigDecimal getBalance(Long accountId){
        String sql = "SELECT balance FROM accounts where id = ?";

        return jdbcTemplate.queryForObject(sql, BigDecimal.class, accountId);
    }

    public void updateBalance(Long accountId, BigDecimal newBalance){
        String sql = "UPDATE accounts SET balance = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? ";
        jdbcTemplate.update(sql, newBalance, accountId);
    }

}

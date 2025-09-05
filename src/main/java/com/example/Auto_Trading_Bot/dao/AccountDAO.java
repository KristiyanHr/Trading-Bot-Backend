package com.example.Auto_Trading_Bot.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public class AccountDAO {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public AccountDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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

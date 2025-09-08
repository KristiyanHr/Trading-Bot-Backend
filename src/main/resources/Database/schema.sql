DROP TABLE IF EXISTS portfolio_holdings;
DROP TABLE IF EXISTS trades;
DROP TABLE IF EXISTS market_data;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE accounts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    balance DECIMAL(19, 8) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE market_data (
    id INT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    timestamp DATETIME NOT NULL,
    price DECIMAL(19, 8) NOT NULL,
    UNIQUE KEY unique_market_data_per_time_symbol (symbol, timestamp)
);

CREATE TABLE trades (
    id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    trade_type VARCHAR(10) NOT NULL,
    simulation_type VARCHAR(10) NOT NULL,-- 'BUY' or 'SELL'
    quantity DECIMAL(19, 8) NOT NULL,
    price DECIMAL(19, 8) NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    pnl DECIMAL(19, 8), -- Profit/Loss
    FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE TABLE portfolio_holdings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    quantity DECIMAL(19, 8) NOT NULL,
    average_buy_price DECIMAL(19, 8),
    UNIQUE KEY unique_holding_per_account_symbol (account_id, symbol),
    FOREIGN KEY (account_id) REFERENCES accounts(id)
);

This project is the backend service for a simulated automated cryptocurrency trading bot. It's a full-stack application with a Java Spring Boot backend and a React frontend. This repository contains the backend code.

The backend is responsible for all core business logic, including fetching market data, executing trading strategies, managing account state, and serving data to the frontend via a REST API.

**Frontend Repository:** https://github.com/KristiyanHr/Trading-Bot-Frontend

## Features

*   **Dual-Mode Operation:**
    *   **Training Mode:** Runs the trading strategy against historical market data to perform a backtest.
    *   **Live Simulation Mode:** Runs the bot against live market prices in a continuous loop to simulate real-time trading.
*   **Advanced Trading Strategy:** Implements a two-factor trading algorithm combining a trend indicator (Simple Moving Average Crossover) with a momentum indicator (Relative Strength Index - RSI) for more robust signals.
*   **Stateful Account Management:** Accurately tracks account balance (USD) and crypto portfolio holdings, persisting all changes to a relational database.
*   **Dynamic Trade Sizing:** Trades are sized as a percentage of the current account balance, a realistic risk management technique.
*   **Profit/Loss Calculation:** Accurately calculates and records the profit or loss for every completed trade (round trip).
*   **Multi-Asset Ready:** The system is designed to handle multiple cryptocurrency pairs (e.g., BTCUSDT, ETHUSDT).
*   **REST API:** A clean and professional API for the frontend to consume, covering bot controls and data retrieval.
*   **Unit & Integration Tests:** Includes JUnit tests to verify the correctness of business logic and the integrity of the API endpoints.

## Technical Requirements & Stack

*   **Language/Framework:** Java 17+ with Spring Boot 3
*   **Data Persistence:**
    *   **Database:** MySQL
    *   **Data Access:** Raw SQL queries via Spring's `JdbcTemplate` (as per project requirements, no ORM).
*   **Build Tool:** Maven
*   **Testing:** JUnit 5, MockMvc for integration tests.

## Setup & Running the Application

### Prerequisites
*   Java JDK 17 or newer
*   Apache Maven
*   MySQL Server

### 1. Database Setup

1.  **Start your MySQL server.**
2.  Connect to MySQL using a client (like MySQL Workbench or the command line) and create the user and database by running the following script. (Remember to replace `'your_password'` with a secure password).
 
    DROP USER IF EXISTS 'trading_bot_user'@'localhost';
    CREATE USER 'trading_bot_user'@'localhost' IDENTIFIED BY 'your_password';
    DROP DATABASE IF EXISTS trading_bot_db;
    CREATE DATABASE trading_bot_db;
    GRANT ALL PRIVILEGES ON trading_bot_db.* TO 'trading_bot_user'@'localhost';
    FLUSH PRIVILEGES;


### 2. Application Configuration

1.  Open the `src/main/resources/application.properties` file.
2.  Update the `spring.datasource` properties with your MySQL username and password.
   
    spring.datasource.url=jdbc:mysql://localhost:3306/trading_bot_db
    spring.datasource.username=trading_bot_user
    spring.datasource.password=your_password
 

### 3. Running the Backend

1.  Navigate to the root directory of the `trading-bot-backend` project.
2.  Run the application using the Maven Spring Boot plugin:

    mvn spring-boot:run

3.  The backend server will start on `http://localhost:8080`.

### 4. Populating Historical Data

For the backtesting and live simulation features to work, the database must be populated with historical market data.

1.  After the server is running, use a tool like Postman or your browser to make a GET request to the following endpoint. This will fetch the last year of daily data for Bitcoin and save it to the database. This only needs to be done once.

    http://localhost:8080/api/test/setup-data/bitcoin/BTCUSDT/365


## API Endpoints

An overview of the main API endpoints.

*   `POST /api/bot/start-backtest/{symbol}`: Starts a new backtest simulation.
*   `POST /api/bot/start-live/{symbol}`: Starts the live trading simulation.
*   `POST /api/bot/stop-live`: Stops the live trading simulation.
*   `POST /api/bot/reset`: Resets the account balance and clears all trades and holdings.
*   `GET /api/account/{id}`: Retrieves the full status of an account (balance, holdings, P/L).
*   `GET /api/account/{id}/trades?type={simulationType}`: Retrieves a list of trades for a specific simulation type (`BACKTEST` or `LIVE`).
*   `GET /api/market-data/{symbol}`: Retrieves historical market data for a given date range.

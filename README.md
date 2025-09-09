#  Automated Crypto Trading Bot

This is a full-stack web application that simulates an automated cryptocurrency trading bot. It is built with a Java Spring Boot backend and a React frontend, providing both the core trading logic and a user-friendly dashboard to control and visualize its performance.

---

## Overview

The project is composed of two distinct parts that work together to simulate real-world trading conditions.

###  Backend (Java, Spring Boot, MySQL)
The backend is the brain of the operation. It handles all market data integration, trading strategy execution, stateful account management, and exposes a comprehensive REST API.

###  Frontend (React, Vite, Recharts)
The frontend provides a modern, interactive dashboard to control the bot, monitor performance in real-time, and analyze both trade history and broader market data.

### Core Features
Together, the two parts provide a complete simulation experience:
    **Dual-Mode Operation: Run the bot in Training Mode (backtesting against historical data) or Trading Mode (simulating trades against live market prices).
    **Advanced Two-Factor Strategy:  Implements a robust algorithm combining a Simple Moving Average (SMA) Crossover for trend direction with a  Relative Strength Index (RSI) for momentum confirmation.
    **Stateful Account Management: Accurately tracks account balance and portfolio, using dynamic trade sizing (percentage of capital) for realistic risk management.
    **Rich Data Visualization: Real-time charting of portfolio performance, detailed trade history tables, and comparative price analysis charts.


##  Project Structure

This project is organized as a multi-repository setup, which is standard for modern full-stack applications.

---

root/

│
├── trading-bot-backend/   # The Backend service (Spring Boot, MySQL, REST API)
│
└── trading-bot-frontend/  # The Frontend dashboard (React + Vite)

---

##  Setup & Running the Project

Each part of the project has its own detailed setup guide. You must start the backend first.

### 1. Backend Setup
Navigate to the backend directory for instructions on setting up the database and running the server.
[View Backend README](./Trading-Bot-Backend/README.md)

> The backend runs at: `http://localhost:8080`

### 2. Frontend Setup
Once the backend is running, navigate to the frontend directory for instructions on installing dependencies and starting the UI.
[View Frontend README](./Trading-Bot-Frontend/README.md)

> The frontend runs at: `http://localhost:5173`

---

## Repositories

*   🔗 Backend Repository: https://github.com/KristiyanHr/Trading-Bot-Backend
*   🔗 Frontend Repository: https://github.com/KristiyanHr/Trading-Bot-Frontend

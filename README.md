# Stock-Trading-Engine

A high-performance stock trading engine implementation using lock-free algorithms for real-time order matching.

## Features

- Real-time order matching with price-time priority
- Lock-free design using atomic operations
- Supports 1,024 concurrent tickers
- Multi-threaded order simulation
- Partial order fulfillment handling
- Thread-safe order book management

## Usage
addOrder(OrderType.BUY, 42, 100, 150);  // Ticker 42, 100 shares @ $150
addOrder(OrderType.SELL, 42, 50, 155);   // Ticker 42, 50 shares @ $155


# Technical Implementation
## Core Components

Order Books: Array-based storage for 1024 tickers
Atomic References: For lock-free linked list operations
CAS Operations: Compare-and-swap for thread-safe updates

## Order Priorities
- Buy Orders: Highest price first
- Sell Orders: Lowest price first
- Time priority for same-priced orders


## Matching Logic
- Check if best bid ≥ best ask
-Execute trade at ask price
- Update quantities atomically
- Remove filled orders from book

## Performance

- Order Insertion	O(n) 
- Order Matching	O(1) per match which is O(n) for n matches

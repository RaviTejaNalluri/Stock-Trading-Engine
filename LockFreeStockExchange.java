import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class LockFreeStockExchange {
    public enum OrderType { BUY, SELL } 

    private static final int NUM_TICKERS = 1024;
    
    private static class Order {
        OrderType type;
        int ticker;
        AtomicInteger quantity;
        int price;
        AtomicReference<Order> next = new AtomicReference<>();

        Order(OrderType type, int ticker, int quantity, int price) {
            this.type = type;
            this.ticker = ticker;
            this.quantity = new AtomicInteger(quantity);
            this.price = price;
        }
    }

    private static class TickerOrderBook {
        final AtomicReference<Order> buyHead = new AtomicReference<>();
        final AtomicReference<Order> sellHead = new AtomicReference<>();
    }

    private static final TickerOrderBook[] orderBooks = new TickerOrderBook[NUM_TICKERS];

    static {
        for (int i = 0; i < NUM_TICKERS; i++) {
            orderBooks[i] = new TickerOrderBook();
        }
    }

    public static void addOrder(OrderType type, int ticker, int quantity, int price) {
        Order newOrder = new Order(type, ticker, quantity, price);
        TickerOrderBook book = orderBooks[ticker];
        AtomicReference<Order> headRef = (type == OrderType.BUY) ? book.buyHead : book.sellHead;

        while (true) {
            Order prev = null;
            Order curr = headRef.get();
            
            while (curr != null && shouldInsertBefore(type, curr, newOrder)) {
                prev = curr;
                curr = curr.next.get();
            }

            newOrder.next.set(curr);
            
            if (prev == null) {
                if (headRef.compareAndSet(curr, newOrder)) break;
            } else {
                if (prev.next.compareAndSet(curr, newOrder)) break;
            }
        }

        matchOrders(ticker);
    }

    private static boolean shouldInsertBefore(OrderType type, Order existing, Order newOrder) {
        if (type == OrderType.BUY) {
            return existing.price > newOrder.price; 
        } else {
            return existing.price < newOrder.price;
        }
    }

    private static void matchOrders(int ticker) {
        TickerOrderBook book = orderBooks[ticker];
        
        while (true) {
            Order buy = book.buyHead.get();
            Order sell = book.sellHead.get();
            
            if (buy == null || sell == null || buy.price < sell.price) break;
            
            int minQty = Math.min(buy.quantity.get(), sell.quantity.get());
            
            int buyRemaining = buy.quantity.addAndGet(-minQty);
            int sellRemaining = sell.quantity.addAndGet(-minQty);
            
            if (buyRemaining == 0) {
                if (!book.buyHead.compareAndSet(buy, buy.next.get())) {
                    break; 
                }
            }
            if (sellRemaining == 0) {
                if (!book.sellHead.compareAndSet(sell, sell.next.get())) {
                    break;
                }
            }
            
            if (buyRemaining > 0 || sellRemaining > 0) break;
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < 8; i++) {
            new Thread(() -> {
                ThreadLocalRandom rand = ThreadLocalRandom.current();
                while (true) {
                    int ticker = rand.nextInt(NUM_TICKERS);
                    OrderType type = rand.nextBoolean() ? OrderType.BUY : OrderType.SELL;
                    int quantity = rand.nextInt(100) + 1;
                    int price = rand.nextInt(1000) + 1;
                    
                    addOrder(type, ticker, quantity, price);
                    
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }).start();
        }
    }

}

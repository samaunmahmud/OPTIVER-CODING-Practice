import java.util.*;

public class OrderBook1 {

    // 1. Core Data Models
    public enum Side { BUY, SELL }

    public static class Order {
        String id;
        Side side;
        long price;
        long quantity;

        public Order(String id, Side side, long price, long quantity) {
            this.id = id;
            this.side = side;
            this.price = price;
            this.quantity = quantity;
        }
    }

    public record Trade(String buyOrderId, String sellOrderId, long price, long quantity) {}

    // 2. State & Storage
    // Bids: Sorted High -> Low | Asks: Sorted Low -> High
    private final TreeMap<Long, List<Order>> bids = new TreeMap<>(Collections.reverseOrder());
    private final TreeMap<Long, List<Order>> asks = new TreeMap<>();
    private final Map<String, Order> ordersById = new HashMap<>();
    private final List<Trade> trades = new ArrayList<>();

    // 3. Main Action: Add an Order
    public List<Trade> addOrder(String id, Side side, long price, long quantity) {
        if (ordersById.containsKey(id) || quantity <= 0) {
            throw new IllegalArgumentException("Invalid order submission");
        }

        Order incoming = new Order(id, side, price, quantity);

        // Step 1: Try to match with opposite orders
        List<Trade> newTrades = match(incoming);
        trades.addAll(newTrades);

        // Step 2: If quantity is left over, put it on the book to wait
        if (incoming.quantity > 0) {
            restOnBook(incoming);
        }

        return newTrades;
    }

    // 4. Matching Logic
    private List<Trade> match(Order incoming) {
        List<Trade> newTrades = new ArrayList<>();

        // Pick the opposite side of the book to trade against
        TreeMap<Long, List<Order>> oppositeBook;
        if (incoming.side == Side.BUY) {
            oppositeBook = asks;
        } else {
            oppositeBook = bids;
        }

        // Loop as long as incoming order needs filling and opposite orders exist
        while (incoming.quantity > 0 && !oppositeBook.isEmpty()) {

            // Get the best price level on the opposite side
            Map.Entry<Long, List<Order>> bestLevel = oppositeBook.firstEntry();
            long levelPrice = bestLevel.getKey();

            // Check if prices cross
            boolean pricesCross;
            if (incoming.side == Side.BUY) {
                pricesCross = (incoming.price >= levelPrice);
            } else {
                pricesCross = (incoming.price <= levelPrice);
            }

            if (!pricesCross) {
                break; // Prices don't match, stop trying to trade
            }

            // Get the oldest resting order at this price (FIFO)
            List<Order> queue = bestLevel.getValue();
            Order resting = queue.get(0);

            // Calculate fill amount
            long fillQuantity = Math.min(incoming.quantity, resting.quantity);

            // Determine buyer and seller IDs
            String buyId, sellId;
            if (incoming.side == Side.BUY) {
                buyId = incoming.id;
                sellId = resting.id;
            } else {
                buyId = resting.id;
                sellId = incoming.id;
            }

            // Record the trade execution
            Trade trade = new Trade(buyId, sellId, levelPrice, fillQuantity);
            newTrades.add(trade);

            // Update remaining quantities
            incoming.quantity -= fillQuantity;
            resting.quantity -= fillQuantity;

            // Clean up fully filled resting order
            if (resting.quantity == 0) {
                queue.remove(0); // Remove from list
                ordersById.remove(resting.id); // Remove from map

                // Remove price level entirely if no orders remain at this price
                if (queue.isEmpty()) {
                    oppositeBook.remove(levelPrice);
                }
            }
        }

        return newTrades;
    }

    // 5. Helper: Save leftover order to book
    private void restOnBook(Order order) {
        TreeMap<Long, List<Order>> targetBook;
        if (order.side == Side.BUY) {
            targetBook = bids;
        } else {
            targetBook = asks;
        }

        // Add price level if it doesn't exist, then add order to the back of list
        if (!targetBook.containsKey(order.price)) {
            targetBook.put(order.price, new ArrayList<>());
        }
        targetBook.get(order.price).add(order);

        // Save to fast lookup map
        ordersById.put(order.id, order);
    }

    // 6. Cancel an Order
    public boolean cancelOrder(String id) {
        Order order = ordersById.remove(id);
        if (order == null) {
            return false; // Order wasn't found on the book
        }

        TreeMap<Long, List<Order>> targetBook;
        if (order.side == Side.BUY) {
            targetBook = bids;
        } else {
            targetBook = asks;
        }

        List<Order> level = targetBook.get(order.price);
        if (level != null) {
            level.remove(order);
            if (level.isEmpty()) {
                targetBook.remove(order.price);
            }
        }

        return true;
    }

    // 7. Market Queries
    public Long bestBid() {
        if (bids.isEmpty()) return null;
        return bids.firstKey();
    }

    public Long bestAsk() {
        if (asks.isEmpty()) return null;
        return asks.firstKey();
    }

    public List<Trade> getTrades() {
        return trades;
    }
}
import java.util.*;

public class OrderBook1 {

    // =========================================================================
    // STAGE 1: DATA MODELS
    // Objects representing an Order, Side, and completed Trade.
    // =========================================================================

    // Defines the directional intent of the trader: either buying or selling.
    public enum Side { BUY, SELL }

    // Represents a single order submitted by a trader.
    public static class Order {
        String id;       // Unique identifier (e.g., "ord-101")
        Side side;       // BUY or SELL
        long price;      // Target price (e.g., 100)
        long quantity;   // Number of units left to trade (mutates as trades execute)

        public Order(String id, Side side, long price, long quantity) {
            this.id = id;
            this.side = side;
            this.price = price;
            this.quantity = quantity;
        }
    }

    // An immutable record logging a successfully executed match between two orders.
    public record Trade(String buyOrderId, String sellOrderId, long price, long quantity) {}


    // =========================================================================
    // STAGE 2: STORAGE & STATE MANAGEMENT
    // How orders and trades are organized in memory for fast operations.
    // =========================================================================

    // BUY ORDERS (Bids): TreeMap sorted HIGH to LOW (Collections.reverseOrder()).
    // Key = Price level | Value = List of orders waiting at that price in arrival order (FIFO).
    private final TreeMap<Long, List<Order>> bids = new TreeMap<>(Collections.reverseOrder());

    // SELL ORDERS (Asks): TreeMap sorted LOW to HIGH (Default natural ordering).
    // Key = Price level | Value = List of orders waiting at that price in arrival order (FIFO).
    private final TreeMap<Long, List<Order>> asks = new TreeMap<>();

    // ID DIRECTORY: Maps Order ID -> Order Object.
    // Allows us to find any active order instantly in O(1) time without searching through price levels.
    private final Map<String, Order> ordersById = new HashMap<>();

    // TRADE HISTORY: Audit trail of all completed trades executed by this system.
    private final List<Trade> trades = new ArrayList<>();


    // =========================================================================
    // STAGE 3: MAIN ACTION - ADDING AN ORDER
    // Entry point when a trader submits a new order to buy or sell.
    // =========================================================================

    public List<Trade> addOrder(String id, Side side, long price, long quantity) {

        // Safety Check: Reject duplicate IDs or orders with zero/negative quantities
        if (ordersById.containsKey(id) || quantity <= 0) {
            throw new IllegalArgumentException("Invalid order submission");
        }

        // Wrap raw inputs into a new Order object
        Order incoming = new Order(id, side, price, quantity);

        // STEP A: Try to immediately execute trades against existing resting orders
        List<Trade> newTrades = match(incoming);

        // Save executed trades to global trade list
        trades.addAll(newTrades);

        // STEP B: If the incoming order wasn't fully filled, place leftover quantity on book
        if (incoming.quantity > 0) {
            restOnBook(incoming);
        }

        // Return trades generated specifically by this new order
        return newTrades;
    }


    // =========================================================================
    // STAGE 4: MATCHING ENGINE
    // Core logic that matches incoming orders against existing orders.
    // =========================================================================

    private List<Trade> match(Order incoming) {
        List<Trade> newTrades = new ArrayList<>();

        // Pick the OPPOSITE book to search for matches.
        // If incoming is BUY -> Look for sellers in 'asks'
        // If incoming is SELL -> Look for buyers in 'bids'
        TreeMap<Long, List<Order>> oppositeBook;
        if (incoming.side == Side.BUY) {
            oppositeBook = asks;
        } else {
            oppositeBook = bids;
        }

        // Loop as long as incoming order has remaining quantity AND opposing orders exist
        while (incoming.quantity > 0 && !oppositeBook.isEmpty()) {

            // Grab the BEST available price level on the opposite side (the top of the TreeMap)
            Map.Entry<Long, List<Order>> bestLevel = oppositeBook.firstEntry();
            long levelPrice = bestLevel.getKey();

            // Check if prices "cross" (Can a deal happen?):
            // - BUY incoming: Wants to buy at 'price' or cheaper -> incoming.price >= levelPrice
            // - SELL incoming: Wants to sell at 'price' or higher -> incoming.price <= levelPrice
            boolean pricesCross;
            if (incoming.side == Side.BUY) {
                pricesCross = (incoming.price >= levelPrice);
            } else {
                pricesCross = (incoming.price <= levelPrice);
            }

            // If prices do NOT cross, no trade can happen. Stop matching immediately.
            if (!pricesCross) {
                break;
            }

            // Price match confirmed! Grab the waiting line at this price level.
            List<Order> queue = bestLevel.getValue();

            // Get the OLDEST resting order at the front of the list (Time Priority / FIFO)
            Order resting = queue.get(0);

            // Trade quantity is limited by whichever order has fewer shares/units left
            long fillQuantity = Math.min(incoming.quantity, resting.quantity);

            // Determine who was buyer and who was seller for the trade log
            String buyId, sellId;
            if (incoming.side == Side.BUY) {
                buyId = incoming.id;
                sellId = resting.id;
            } else {
                buyId = resting.id;
                sellId = incoming.id;
            }

            // Create and record the trade event at the resting order's price level
            Trade trade = new Trade(buyId, sellId, levelPrice, fillQuantity);
            newTrades.add(trade);

            // Deduct traded amount from both orders' quantities
            incoming.quantity -= fillQuantity;
            resting.quantity -= fillQuantity;

            // CLEANUP: If the resting order is now completely filled (0 quantity left)
            if (resting.quantity == 0) {
                queue.remove(0);               // Remove resting order from the price level line
                ordersById.remove(resting.id); // Remove resting order from global fast-lookup map

                // If no more orders remain at this price level, remove the price entry entirely
                if (queue.isEmpty()) {
                    oppositeBook.remove(levelPrice);
                }
            }
        }

        return newTrades;
    }

    // Helper Method: Places leftover/unfilled order onto the board to wait for future trades
    private void restOnBook(Order order) {

        // Select target book based on side
        TreeMap<Long, List<Order>> targetBook;
        if (order.side == Side.BUY) {
            targetBook = bids;
        } else {
            targetBook = asks;
        }

        // If this price level doesn't exist yet, create a new list for it
        if (!targetBook.containsKey(order.price)) {
            targetBook.put(order.price, new ArrayList<>());
        }

        // Add order to the BACK of the list (maintains arrival sequence for FIFO)
        targetBook.get(order.price).add(order);

        // Register order in global HashMap for fast lookup
        ordersById.put(order.id, order);
    }


    // =========================================================================
    // STAGE 5: MANAGEMENT & MARKET QUERIES
    // Fast O(1) order cancellations and top-of-book market data lookups.
    // =========================================================================

    // Cancels an active order by ID
    public boolean cancelOrder(String id) {
        // Step 1: Remove from HashMap instantly. If it wasn't in HashMap, it doesn't exist.
        Order order = ordersById.remove(id);
        if (order == null) {
            return false; // Order was already filled or cancelled
        }

        // Step 2: Determine which book it resides in
        TreeMap<Long, List<Order>> targetBook;
        if (order.side == Side.BUY) {
            targetBook = bids;
        } else {
            targetBook = asks;
        }

        // Step 3: Locate the list at that price level and remove the order from the list
        List<Order> level = targetBook.get(order.price);
        if (level != null) {
            level.remove(order);

            // Clean up price level if no orders are left
            if (level.isEmpty()) {
                targetBook.remove(order.price);
            }
        }

        return true; // Successfully cancelled
    }

    // Returns the highest active BUY price in the market (or null if empty)
    public Long bestBid() {
        if (bids.isEmpty()) return null;
        return bids.firstKey(); // O(1) access to top key
    }

    // Returns the lowest active SELL price in the market (or null if empty)
    public Long bestAsk() {
        if (asks.isEmpty()) return null;
        return asks.firstKey(); // O(1) access to top key
    }

    // Returns all trades completed since system startup
    public List<Trade> getTrades() {
        return trades;
    }
}
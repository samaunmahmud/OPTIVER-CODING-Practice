


import java.util.*;

public class OrderBook {

    public enum Side { BUY, SELL }

    public static class Order {
        final String id;
        final Side side;
        final long price;
        long quantity;

        Order(String id, Side side, long price, long quantity) {
            this.id = id; this.side = side; this.price = price; this.quantity = quantity;
        }
    }

    public record Trade(String buyOrderId, String sellOrderId, long price, long quantity) {}

    private final TreeMap<Long, LinkedHashSet<Order>> bids = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<Long, LinkedHashSet<Order>> asks = new TreeMap<>();
    private final Map<String, Order> ordersById = new HashMap<>();
    private final List<Trade> trades = new ArrayList<>();

    public List<Trade> addOrder(String id, Side side, long price, long quantity) {
        if (ordersById.containsKey(id) || quantity <= 0) {
            throw new IllegalArgumentException("Invalid order submission");
        }

        Order incoming = new Order(id, side, price, quantity);
        List<Trade> newTrades = match(incoming);

        if (incoming.quantity > 0) {
            var book = incoming.side == Side.BUY ? bids : asks;
            book.computeIfAbsent(incoming.price, k -> new LinkedHashSet<>()).add(incoming);
            ordersById.put(incoming.id, incoming);
        }

        trades.addAll(newTrades);
        return newTrades;
    }

    private List<Trade> match(Order incoming) {
        List<Trade> newTrades = new ArrayList<>();
        var opposite = incoming.side == Side.BUY ? asks : bids;

        while (incoming.quantity > 0 && !opposite.isEmpty()) {
            var bestLevel = opposite.firstEntry();
            long levelPrice = bestLevel.getKey();

            boolean crosses = incoming.side == Side.BUY ? incoming.price >= levelPrice : incoming.price <= levelPrice;
            if (!crosses) break;

            var queue = bestLevel.getValue();
            Order resting = queue.iterator().next();
            long fillQty = Math.min(incoming.quantity, resting.quantity);

            String buyId  = incoming.side == Side.BUY ? incoming.id : resting.id;
            String sellId = incoming.side == Side.BUY ? resting.id : incoming.id;
            newTrades.add(new Trade(buyId, sellId, levelPrice, fillQty));

            incoming.quantity -= fillQty;
            resting.quantity -= fillQty;

            if (resting.quantity == 0) {
                queue.remove(resting);
                ordersById.remove(resting.id);
                if (queue.isEmpty()) opposite.remove(levelPrice);
            }
        }
        return newTrades;
    }

    public boolean cancelOrder(String id) {
        Order order = ordersById.remove(id);
        if (order == null) return false;

        var book = order.side == Side.BUY ? bids : asks;
        var level = book.get(order.price);
        if (level != null) {
            level.remove(order);
            if (level.isEmpty()) book.remove(order.price);
        }
        return true;
    }

    public Long bestBid() { return bids.isEmpty() ? null : bids.firstKey(); }
    public Long bestAsk() { return asks.isEmpty() ? null : asks.firstKey(); }
    public List<Trade> getTrades() { return List.copyOf(trades); }
}
import java.util.*;

public class Portfolio {

    public enum Side { BUY, SELL }

    // Stores the position status for a single symbol (e.g., "AAPL")
    public static class Position {
        public long quantity = 0;
        public double averageCost = 0.0;
        public double realizedPnL = 0.0;
    }

    // Maps each ticker symbol (String) to its Position object
    private final Map<String, Position> positions = new HashMap<>();

    public void recordTrade(String symbol, Side side, long quantity, double price) {
        // Input Validation
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        if (price < 0) throw new IllegalArgumentException("Price cannot be negative");

        // Get existing position or create a new one if trading this symbol for the first time
        Position pos = positions.computeIfAbsent(symbol, k -> new Position());

        if (side == Side.BUY) {
            // 1. Calculate total dollar value spent across old and new shares
            double totalCost = (pos.quantity * pos.averageCost) + (quantity * price);

            // 2. Increase held quantity
            pos.quantity += quantity;

            // 3. Update weighted average cost
            pos.averageCost = totalCost / pos.quantity;

        } else { // SELL
            // Long-only check: cannot sell more than currently held
            if (quantity > pos.quantity) {
                throw new IllegalStateException("Cannot sell " + quantity + "; only hold " + pos.quantity);
            }

            // 1. Calculate and lock in Realised P&L
            pos.realizedPnL += (price - pos.averageCost) * quantity;

            // 2. Decrease held quantity (Average cost remains unchanged!)
            pos.quantity -= quantity;

            // 3. Reset cost basis if completely flat
            if (pos.quantity == 0) {
                pos.averageCost = 0.0;
            }
        }
    }

    public Position getPosition(String symbol) {
        // Return blank position (0 qty, 0 avg cost) if symbol was never traded
        return positions.getOrDefault(symbol, new Position());
    }

    public double getRealizedPnL(String symbol) {
        return getPosition(symbol).realizedPnL;
    }

    public double getUnrealizedPnL(String symbol, double currentPrice) {
        Position pos = getPosition(symbol);
        // Paper P&L = (Current Market Price - Entry Cost Basis) * Current Open Quantity
        return (currentPrice - pos.averageCost) * pos.quantity;
    }
}
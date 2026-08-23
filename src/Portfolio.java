import java.util.*;

public class Portfolio {

    // =========================================================================
    // STAGE 1: DATA MODELS
    // Enum for trade side and class tracking open positions & accounting stats.
    // =========================================================================

    // Indicates whether a trade is acquiring shares (BUY) or liquidating shares (SELL).
    public enum Side { BUY, SELL }

    // Holds the real-time position metrics for a single stock/ticker symbol.
    public static class Position {
        public long quantity = 0;          // Open shares currently held (e.g., 100 shares)
        public double averageCost = 0.0;   // Weighted average cost paid per share ($/share)
        public double realizedPnL = 0.0;   // Locked-in profit or loss realized from executed sales ($)
    }


    // =========================================================================
    // STAGE 2: STORAGE & STATE MANAGEMENT
    // How portfolio positions are held and indexed in memory.
    // =========================================================================

    // DIRECTORY: Maps Ticker Symbol (e.g., "AAPL") -> Position Object.
    // Allows O(1) instant lookup to read or update holding status for any symbol.
    private final Map<String, Position> positions = new HashMap<>();


    // =========================================================================
    // STAGE 3: CORE LOGIC - RECORDING TRADES
    // Primary execution engine: updates cost basis, quantities, and realized PnL.
    // =========================================================================

    public void recordTrade(String symbol, Side side, long quantity, double price) {

        // Safety Checks: Prevent processing invalid or corrupt trade execution values
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        if (price < 0) throw new IllegalArgumentException("Price cannot be negative");

        // Fetch existing position for symbol. If trading for the first time, initialize a new Position(0, 0.0, 0.0).
//        Position pos = positions.computeIfAbsent(symbol, k -> new Position());

        // 1. Try to get the existing position from the map
        Position pos = positions.get(symbol);

// 2. If it doesn't exist yet, create a new one and put it in the map
        if (pos == null) {
            pos = new Position();
            positions.put(symbol, pos);
        }

        // ---------------------------------------------------------------------
        // CASE A: BUYING SHARES (Increasing position size & shifting cost basis)
        // ---------------------------------------------------------------------
        if (side == Side.BUY) {

            // Step 1: Calculate total dollars spent across existing inventory AND new order:
            // Total Cost = (Old Quantity * Old Avg Cost) + (New Quantity * New Buy Price)
            double totalCost = (pos.quantity * pos.averageCost) + (quantity * price);

            // Step 2: Increase total share count held
            pos.quantity += quantity;

            // Step 3: Recalculate Weighted Average Cost per share:
            // New Average Cost = Total Dollar Investment / Total Shares Now Owned
            pos.averageCost = totalCost / pos.quantity;

            // ---------------------------------------------------------------------
            // CASE B: SELLING SHARES (Decreasing position size & locking in PnL)
            // ---------------------------------------------------------------------
        } else { // SELL

            // Risk Check: Long-only rule—prevent selling more shares than currently owned
            if (quantity > pos.quantity) {
                throw new IllegalStateException("Cannot sell " + quantity + "; only hold " + pos.quantity);
            }

            // Step 1: Calculate Realized Profit/Loss (PnL) on the sold shares:
            // PnL per share = (Selling Price - Weighted Average Entry Price)
            // Total Realized PnL = PnL per share * Shares Sold
            pos.realizedPnL += (price - pos.averageCost) * quantity;

            // Step 2: Deduct sold shares from open inventory.
            // NOTE: averageCost DOES NOT CHANGE during a sell! Your entry basis per remaining share stays identical.
            pos.quantity -= quantity;

            // Step 3: Cleanup: If position is completely closed (0 shares left), reset cost basis to zero
            if (pos.quantity == 0) {
                pos.averageCost = 0.0;
            }
        }
    }


    // =========================================================================
    // STAGE 4: METRICS & PORTFOLIO QUERIES
    // Read-only methods to inspect position health, realized PnL, and paper profit.
    // =========================================================================

    // Retrieves position object for a symbol (returns zeroed-out position if symbol was never traded)
    public Position getPosition(String symbol) {
        return positions.getOrDefault(symbol, new Position());
    }

    // Returns total cash profit or loss locked in from COMPLETED sales for a symbol
    public double getRealizedPnL(String symbol) {
        return getPosition(symbol).realizedPnL;
    }

    // Calculates UNREALIZED (Paper) PnL on OPEN holdings using current live market price:
    // Paper PnL = (Live Price - Average Entry Price) * Open Shares Held
    public double getUnrealizedPnL(String symbol, double currentPrice) {
        Position pos = getPosition(symbol);
        return (currentPrice - pos.averageCost) * pos.quantity;
    }
}
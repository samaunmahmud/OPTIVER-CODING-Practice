import java.util.*;

public class ExpiringInventoryStore {

    // =========================================================================
    // STAGE 1: DATA MODEL
    // Class representing an inventory item and its priority/expiration metadata.
    // =========================================================================

    private static class Item {
        final String id;                  // Unique identifier for the item (e.g., "item-A")
        final long weight;                // Priority score (higher weight = higher priority)
        final long expirationTimestamp;   // Time after which item becomes invalid/expired
        final long sequence;              // Global insertion counter used to break ties (FIFO)
        boolean removed = false;          // LAZY DELETION FLAG: set to true when removed manually

        Item(String id, long weight, long expirationTimestamp, long sequence) {
            this.id = id;
            this.weight = weight;
            this.expirationTimestamp = expirationTimestamp;
            this.sequence = sequence;
        }
    }


    // =========================================================================
    // STAGE 2: STORAGE & STATE MANAGEMENT
    // Dual-structure design: PriorityQueue for priority ordering + HashMap for O(1) lookups.
    // =========================================================================

    // MAX-HEAP: Orders items primarily by weight descending (highest priority on top).
    // TIE-BREAKER: If weights are equal, orders by sequence ascending (earliest inserted wins).
    private final PriorityQueue<Item> heap = new PriorityQueue<>((a, b) -> {
        if (a.weight != b.weight) {
            return Long.compare(b.weight, a.weight); // Highest weight first
        }
        return Long.compare(a.sequence, b.sequence);  // First-In, First-Out on ties
    });

    // ID DIRECTORY: Tracks active items by ID for O(1) existence checks and manual removal.
    private final Map<String, Item> liveItems = new HashMap<>();

    // MONOTONIC COUNTER: Increments with every insertion to maintain accurate FIFO order.
    private long sequenceCounter = 0;


    // =========================================================================
    // STAGE 3: CORE OPERATIONS - STORING & RETRIEVING ITEMS
    // Primary methods to add items and pull the highest-priority valid item.
    // =========================================================================

    /**
     * Stores a new item in the inventory.
     * Time Complexity: O(log N) due to heap insertion.
     */
    public void store(String itemId, long weight, long expirationTimestamp) {

        // Safety Check: Reject duplicate item IDs to prevent map desynchronization
        if (liveItems.containsKey(itemId)) {
            throw new IllegalArgumentException("Duplicate item id: " + itemId);
        }

        // Create item with current sequence counter value, then increment counter
        Item item = new Item(itemId, weight, expirationTimestamp, sequenceCounter++);

        // Save to fast O(1) lookup map
        liveItems.put(itemId, item);

        // Offer to priority max-heap (rebalances in O(log N))
        heap.offer(item);
    }

    /**
     * Retrieves and returns the non-expired item with the highest weight.
     * Time Complexity: Amortized O(log N).
     */
    public String retrieve(long currentTimestamp) {

        // Loop continuously through the top of the heap until a valid item is found
        while (!heap.isEmpty()) {

            // Inspect (peek) the top item without removing it yet
            Item top = heap.peek();

            // CASE A: Item was manually deleted via remove()
            if (top.removed) {
                heap.poll(); // Lazy deletion: discard stale item from heap
                continue;    // Move to next item in heap
            }

            // CASE B: Item has expired (expirationTimestamp <= currentTimestamp)
            if (top.expirationTimestamp <= currentTimestamp) {
                heap.poll();              // Discard expired item from heap
                liveItems.remove(top.id); // Remove dangling reference from tracking map
                continue;                 // Move to next item in heap
            }

            // CASE C: Success! Found the highest-priority active, non-expired item.
            heap.poll();              // Remove from heap
            liveItems.remove(top.id); // Clean up from tracking map
            return top.id;            // Return the winning item ID
        }

        // Return null if the store is empty or all items were removed/expired
        return null;
    }


    // =========================================================================
    // STAGE 4: MANUAL REMOVAL (LAZY DELETION)
    // Marks an item as dead in O(1) time without performing slow heap searches.
    // =========================================================================

    /**
     * Cancels/removes an item before it would naturally be retrieved.
     * Time Complexity: O(1) constant time.
     */
    public boolean remove(String itemId) {

        // Step 1: Remove from live tracking map in O(1) time
        Item item = liveItems.remove(itemId);

        // Return false if item doesn't exist (already retrieved or never added)
        if (item == null) {
            return false;
        }

        // Step 2: Mark flag to true so retrieve() knows to skip it when it hits top of heap
        item.removed = true;

        return true; // Successfully marked for removal
    }
}
import java.util.*;

public class ExpiringInventoryStore {

    // 1. Data model for a single item
    private static class Item {
        final String id;
        final long weight;
        final long expirationTimestamp;
        final long sequence; // Used to break ties on equal weights (FIFO)
        boolean removed = false; // Lazy deletion flag

        Item(String id, long weight, long expirationTimestamp, long sequence) {
            this.id = id;
            this.weight = weight;
            this.expirationTimestamp = expirationTimestamp;
            this.sequence = sequence;
        }
    }

    // 2. Max-Heap ordered by weight descending.
    // If weights match, order by insertion sequence ascending (earliest inserted wins).
    private final PriorityQueue<Item> heap = new PriorityQueue<>((a, b) -> {
        if (a.weight != b.weight) {
            return Long.compare(b.weight, a.weight); // Highest weight first
        }
        return Long.compare(a.sequence, b.sequence);  // First-In, First-Out on ties
    });

    // Fast O(1) map to track currently active items
    private final Map<String, Item> liveItems = new HashMap<>();
    private long sequenceCounter = 0;

    /**
     * Stores an item into the inventory.
     * Time Complexity: O(log N)
     */
    public void store(String itemId, long weight, long expirationTimestamp) {
        if (liveItems.containsKey(itemId)) {
            throw new IllegalArgumentException("Duplicate item id: " + itemId);
        }

        Item item = new Item(itemId, weight, expirationTimestamp, sequenceCounter++);
        liveItems.put(itemId, item);
        heap.offer(item);
    }

    /**
     * Retrieves and removes the highest-priority non-expired item.
     * Time Complexity: Amortized O(log N)
     */
    public String retrieve(long currentTimestamp) {
        while (!heap.isEmpty()) {
            Item top = heap.peek();

            // Case A: Item was explicitly cancelled/removed
            if (top.removed) {
                heap.poll(); // Discard from heap
                continue;
            }

            // Case B: Item has expired
            if (top.expirationTimestamp <= currentTimestamp) {
                heap.poll(); // Discard from heap
                liveItems.remove(top.id); // Clean up from tracking map
                continue;
            }

            // Case C: Found the highest valid item!
            heap.poll();
            liveItems.remove(top.id);
            return top.id;
        }

        return null; // Store is empty or all items are expired/removed
    }

    /**
     * Marks an item as removed in O(1) time.
     * Time Complexity: O(1)
     */
    public boolean remove(String itemId) {
        Item item = liveItems.remove(itemId);
        if (item == null) {
            return false; // Item does not exist or was already retrieved
        }

        item.removed = true; // Flag for lazy deletion in retrieve()
        return true;
    }
}

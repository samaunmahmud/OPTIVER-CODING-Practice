import java.util.*;


public class ExpiringlnventoryStore {



    private static class Item {
        final String id;
        final long expirationTimesstamp;
        final long weight;
        final long sequence;
        boolean removed = false;

        Item(String id, long expirationTimesstamp, long weight, long sequence) {
            this.id = id;
            this.expirationTimesstamp = expirationTimesstamp;
            this.weight = weight;
            this.sequence = sequence;

        }


        private final PriorityQueue<Item> heap = new PriorityQueue<>((Item a, Item b) -> {
            if (a.weight != b.weight) {
                return Long.compare(b.weight, a.weight);
            }
            return Long.compare(a.sequence, b.sequence);
        });


        private final Map<String, Item> liveItems = new HashMap<>();


        private long sequenceCounter = 0;


        public void store(String itemId, long weight, long expirationTimesstamp) {
            if (liveItems.containsKey(itemId)) {
                throw new IllegalStateException("Item with id " + itemId + " already exists");
            }


            Item item = new Item(itemId, expirationTimesstamp, weight, sequenceCounter++);

            liveItems.put(itemId, item);

            heap.offer(item);
        }

        public String retrieve(long currentTimestamp) {
            while (!heap.isEmpty()) {
                Item top = heap.peek();

                if (top.removed) {
                    heap.poll();
                    continue;
                }
                if (top.expirationTimesstamp <= currentTimestamp) {
                    heap.poll();
                    liveItems.remove(top.id);
                    continue;
                }

                heap.poll();

                liveItems.remove(top.id);
                return top.id;
            }

            return null;

        }


        public boolean remove(String itemId){
            Item item = liveItems.remove(itemId);
            if(item == null){
                return false;
            }

            item.removed = true;


            return true;
        }
    }





    }


















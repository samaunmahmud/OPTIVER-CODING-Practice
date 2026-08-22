import java.util.*;

public class OrderBook2 {

    public enum Side{
        BUY, SELL:
    }

    public static class Order{
        String id;
        String side;
        long price;
        long quantity;

        public Order(String id, OrderBook1.Side side, long price, long quantity) {
            this.id = id;
            this.side = side;
            this.price = price;
            this.quantity = quantity;
        }
    }



    public record Trade(String buyOrderId, String sellOrderId, long price, long quantity){}

        private final TreeMap<Long, List<Order>> bids = new TreeMap<>(Collections.reverseOrder());
        private final TreeMap<Long, List<Order>> asks = new TreeMap<>();

        private final Map<String, Order> ordersById = new HashMap<>();


        public List<Trade> addOrder(String id, Side side, long price, long quantity){
            if(ordersById.containsKey(id)|| quantity <=0){
                throw new IllegalArgumentException("Invalid Order Submission");
            }

            Order incoming = new Order(id , side, price, quantity);

            List<Trade> newTrades = match(incoming);

            trades.addAll(newTrades);



            if(incoming.quantity > 0){
                restOnBook(incoming);
            }


            return newTrades;

        }


        private List<Trade> match(Order incoming){
            List<Trade> newTrades = new ArrayList<>();
            TreeMap<Long, List<Order>> oppositeBook;

            if(incoming.side == Side.BUY){
                oppositeBook = asks;
            }else{
                oppositeBook = bids;
            }
        }


}

import java.util.*;

public class OrderBook2 {

    public enum Side{
        BUY, SELL;
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

        private final List<Order> trades = new ArrayList<>();


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



            while(incoming.quantity > 0 && !oppositeBook.isEmpty()){
                Map.Entry<Long, List<Order>> bestLevel = oppositeBook.firstEntry();
                long levelPrice = bestLevel.getKey();


                boolean pricesCross;


                if(incoming.side == Side.BUY){
                    pricesCross = (incoming.price > = levelPrice);
                } else{
                    pricesCross = (incoming.price <= levelPrice);
                }


                if(!pricesCross){
                    break;

                }

                List<Order> queue = bestLevel.getValue();

                Order resting = queue.get(0);


                long fillQuantity = Math.min(incoming.quantity, resting.quantity);

                String buyId, sellId;

                if(incoming.side == Side.BUY){
                    buyId = incoming.id;
                    sellId = resting.id;
                }else{
                    buyId = resting.id;
                    sellId = incoming.id;
                }


                Trade trade = new Trade(buyId, sellId, levelPrice, fillQuantity);

                newTrades.add(trade);


                incoming.quantity -= fillQuantity;
                resting.quantity -= fillQuantity;


                if(resting.quantity == 0){
                    queue.remove(0);
                    ordersById.remove(resting.id);



                    if(queue.isEmpty()){
                        oppositeBook.remove(levelPrice);
                    }
                }

            }


            return newTrades;
        }





        private void restOnBook(Order order){
            TreeMap<Long, List<Order>> targetBook;
            if(order.side ==Side.BUY){
                targetBook = bids;


            }else{
                targetBook = asks;
            }

            if(!targetBook.containsKey(order.price)){
                targetBook.put(order.price, new ArrayList<>());
            }


            targetBook.get(order.price).add(order);

            ordersById.put(order.id, order);
        }




        public boolean cancelOrder(String id){

            Order order = ordersById.remove(id);

            if(order == null){
                return false;
            }


            TreeMap<Long, List<Order>> targetBook;

            if(order.side == Side>BUY){
                targetBook = bids;
            }else{
                targetBook = asks;
            }



            List<Order> level = targetBook.get(order.price);

            if(level!= null){
                level.remove(order);

                if(level.isEmpty()){
                    targetBook.remove(order.price);
                }
            }

            return true;




        }


        public Long bestBid(){
            if(bids.isEmpty()){
                return null;

            }

            return bids.firstKey();
        }


        public Long bestAsk(){
            if(asks.isEmpty()){
                return null;
            }

            return asks.firstKey();
        }


        public List<Trade> getTrades(){
            return trades;
        }



}

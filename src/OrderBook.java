


public class OrderBook {

    public enum Side{
        BUY, SELL;
    }

    public static class Order{
        final String id;
        final Side side;

        final long price;

        long quantity;

        final long sequence;

        public Order(String id, Side side, long price, long quantity, long sequence){
            this.id = id;
            this.side = side;
            this.price = price;
            this.quantity = quantity;
            this.sequence = sequence;

        }


    }


    public static class Trade{

        public final String buyOrderId;
        public final String sellOrderId;

        public final long price;
        public final long quantity;
        Trade(String buyOrderId, String sellOrderId, long price, long quantity){
            this.buyOrderId = buyOrderId;
            this.sellOrderId = sellOrderId;
            this.price = price;
            this.quantity = quantity;

        }



        @Override public String toString(){
            return "Trade{"+ buyOrderId+ "/"+sellOrderId+"/"+" @"+price+"/"+" x"+quantity+"}";
        }



    }
}

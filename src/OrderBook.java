


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
}

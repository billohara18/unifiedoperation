package com.billo.util;

public class OrderItemBy {
    public String orderId;
    public double price;
    public double stopLoss;
    public double takeProfit;
    public double triggerPrice;
    public String orderType;
    public String orderSide;
    public double qty;
    public double leavesQty;
    public double leavesValue;
    public String stopOrderType;

    public static final String BY_SIDE_SELL = "Sell";
    public static final String BY_SIDE_BUY = "Buy";

    public static final String BY_TYPE_MARKET = "Market";
    public static final String BY_TYPE_LIMIT = "Limit";

    public static final String BY_STOP_TYPE_SL = "StopLoss";
    public static final String BY_STOP_TYPE_TP = "TakeProfit";
}

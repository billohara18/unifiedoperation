package com.billo.util;

public class OrderItem {
    public long orderId;
    public double stopPrice;
    public String orderType;
    public String orderSide;

    public static final String SIDE_SELL = "SELL";
    public static final String SIDE_BUY = "BUY";

    public static final String TYPE_TP_MARKET = "TAKE_PROFIT_MARKET";
    public static final String TYPE_TP_LIMIT = "TAKE_PROFIT";
    public static final String TYPE_SL_MARKET = "STOP_MARKET";
    public static final String TYPE_SL_LIMIT = "STOP";
    public static final String TYPE_MARKET = "MARKET";
    public static final String TYPE_LIMIT = "LIMIT";
}

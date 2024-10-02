package com.billo;

import com.billo.util.*;
import com.binance.connector.futures.client.exceptions.BinanceClientException;
import com.binance.connector.futures.client.exceptions.BinanceConnectorException;
import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.bybit.api.client.domain.CategoryType;
import com.bybit.api.client.domain.position.request.PositionDataRequest;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import static com.billo.PrivateConfig.PAIR_BTCUSDT;
import static com.billo.util.OrderItem.*;

public class BnSession extends PlatformSession {

    private double PRICE_DIFF = 0;

    public BnSession(String api_key, String secret_key) {
        super(PrivateConfig.PlatformVersion.Bn, api_key, secret_key);
    }

    public UMFuturesClientImpl getClientImpl() {
        return new UMFuturesClientImpl(Api_Key, Secret_Key, PrivateConfig.UM_BASE_URL);
    }

    public void changeLeverage(int leverage) {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();

        UMFuturesClientImpl client = getClientImpl();

        parameters.put("symbol", PAIR_BTCUSDT);
        parameters.put("leverage", leverage);

        try {
            String result = client.account().changeInitialLeverage(parameters);
            JSONObject resJSONObj = new JSONObject(result);
            if (resJSONObj != null && resJSONObj.has("leverage")) {
                Current_Leverage = leverage;
                printLog("set leverage to " + resJSONObj.optInt("leverage"));
            }
        } catch (BinanceConnectorException e) {
            printLog("full ErrorMessage: " + e.getMessage(), true);
        } catch (BinanceClientException e) {
            printLog("full ErrorMessage: " + e.getMessage() + "\nerrMessage: " + e.getErrMsg() + "\nerrCode: " + e.getErrorCode() + "\nHTTPStatusCode: " + e.getHttpStatusCode(), true);
        }
    }

    public void setDiff(double diff) {
        PRICE_DIFF = diff;
        printLog("set diff " + diff);
    }

    public void getDiff() {
        printLog("current diff = " + PRICE_DIFF);
    }

    public void displayBalance() {
        if (!isTurnedOn) return;
        double balanceBy = getBalance();
        if (this.sessionIndex == 0) {
            printLog("available balance = " + balanceBy);
        } else {
            printLog("batch amount = " + balanceBy);
        }

    }

    public double getBalance() {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();

        UMFuturesClientImpl client = getClientImpl();

        parameters.put("symbol", PAIR_BTCUSDT);

        try {
            String result = client.account().futuresAccountBalance(parameters);
            JSONArray balanceArray = new JSONArray(result);
            for (int i = 0; i < balanceArray.length(); i ++) {
                JSONObject balanceItem = balanceArray.getJSONObject(i);
                if (balanceItem.optString("asset").equals("USDT")) {
                    double balance = balanceItem.optDouble("availableBalance");
                    Current_Balance = balance * (100 - 2 * MAKER_FEE * Current_Leverage) / 100;
                    return balance;
                }
            }

        } catch (BinanceConnectorException e) {
            printLog("full ErrorMessage: " + e.getMessage(), true);
        } catch (BinanceClientException e) {
            printLog("full ErrorMessage: " + e.getMessage() + "\nerrMessage: " + e.getErrMsg() + "\nerrCode: " + e.getErrorCode() + "\nHTTPStatusCode: " + e.getHttpStatusCode(), true);
        }
        return 0;
    }

    public void getLastPositionHistory() {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();

        UMFuturesClientImpl client = getClientImpl();

        parameters.put("symbol", PAIR_BTCUSDT);

        try {
            String result = client.account().accountTradeList(parameters);
            JSONArray tradeArray = new JSONArray(result);


            ArrayList<TradeItem> tradeItemArray = new ArrayList<TradeItem>();

            for (int i = 0; i < tradeArray.length(); i ++) {
                JSONObject tradeItem = tradeArray.getJSONObject(i);
                long orderId = tradeItem.optLong("orderId");
                double orderPnl = tradeItem.optDouble("realizedPnl");
                double orderCom = tradeItem.optDouble("commission");
                String orderSide = tradeItem.optString("side");
                long orderTime = tradeItem.optLong("time");

                TradeItem lastTradeItem = null;
                if (tradeItemArray.size() > 0) {
                    lastTradeItem = tradeItemArray.get(tradeItemArray.size() - 1);
                }

                if (lastTradeItem == null || lastTradeItem.orderId != orderId) {
                    TradeItem currentTradeItem = new TradeItem();
                    currentTradeItem.orderId = orderId;
                    currentTradeItem.orderPnl = orderPnl;
                    currentTradeItem.orderCom = orderCom;
                    currentTradeItem.orderSide = orderSide;
                    currentTradeItem.orderTime = orderTime;

                    tradeItemArray.add(currentTradeItem);

                } else {
                    lastTradeItem.orderPnl += orderPnl;
                    lastTradeItem.orderCom += orderCom;
                }
            }

            double tempCom = 0;

            for (TradeItem tradeItem : tradeItemArray) {
                if (tradeItem.orderPnl == 0) {
                    tempCom += tradeItem.orderCom;
                } else {
                    printLog(DateUtils.getDateString(tradeItem.orderTime) + " " + (tradeItem.orderSide.equals(SIDE_SELL) ? "L" : "S") + " " + Utils.truncateByTwoDecimals(tradeItem.orderPnl - tradeItem.orderCom - tempCom));
                    tempCom = 0;
                }
            }

        } catch (BinanceConnectorException e) {
            printLog("full ErrorMessage: " + e.getMessage(), true);
        } catch (BinanceClientException e) {
            printLog("full ErrorMessage: " + e.getMessage() + "\nerrMessage: " + e.getErrMsg() + "\nerrCode: " + e.getErrorCode() + "\nHTTPStatusCode: " + e.getHttpStatusCode(), true);
        }
    }

    public void displayOpenOrders() {
        ArrayList<OrderItem> openOrders = getOpenOrders();
        if (openOrders != null && openOrders.size() > 0) {
            printLog("    " + openOrders.size() + " open order(s)");
            for (OrderItem orderItem : openOrders) {
                printLog("    stop at " + orderItem.stopPrice);
            }
            openOrders.clear();
        } else {
            printLog("no open order");
        }
    }

    public ArrayList<OrderItem> getOpenOrders() {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();

        UMFuturesClientImpl client = getClientImpl();

        parameters.put("symbol", PAIR_BTCUSDT);

        try {
            String result = client.account().currentAllOpenOrders(parameters);

            ArrayList<OrderItem> orderItemArray = new ArrayList<>();
            JSONArray orderJSONARRAY = new JSONArray(result);
            for (int i = 0; i < orderJSONARRAY.length(); i ++) {
                JSONObject orderJSONOBJ = orderJSONARRAY.getJSONObject(i);
                OrderItem orderItem = new OrderItem();
                orderItem.orderId = orderJSONOBJ.optLong("orderId");
                orderItem.orderSide = orderJSONOBJ.optString("side");
                orderItem.orderType = orderJSONOBJ.optString("type");
                orderItem.stopPrice = orderJSONOBJ.optDouble("stopPrice");
                orderItemArray.add(orderItem);

            }
            return orderItemArray;
        } catch (BinanceConnectorException e) {
            printLog("full ErrorMessage: " + e.getMessage(), true);
        } catch (BinanceClientException e) {
            printLog("full ErrorMessage: " + e.getMessage() + "\nerrMessage: " + e.getErrMsg() + "\nerrCode: " + e.getErrorCode() + "\nHTTPStatusCode: " + e.getHttpStatusCode(), true);
        }
        return null;
    }

    public void cancelAllOrders() {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();

        UMFuturesClientImpl client = getClientImpl();

        parameters.put("symbol", PAIR_BTCUSDT);

        try {
            String result = client.account().cancelAllOpenOrders(parameters);
            //logger.info(result);
            JSONObject resJSONObj = new JSONObject(result);
            if (resJSONObj != null && resJSONObj.has("msg")) {
                printLog(resJSONObj.optString("msg"));
            }

        } catch (BinanceConnectorException e) {
            printLog("full ErrorMessage: " + e.getMessage(), true);
        } catch (BinanceClientException e) {
            printLog("full ErrorMessage: " + e.getMessage() + "\nerrMessage: " + e.getErrMsg() + "\nerrCode: " + e.getErrorCode() + "\nHTTPStatusCode: " + e.getHttpStatusCode(), true);
        }
        Retain_SL_Task = false;
        Retain_TP_Task = false;
    }

    public void pollServer() {
        printLog("poll task started");
        Retain_Poll_Task = true;
        while (Retain_Poll_Task) {
            try {
                callPollingServer();
            } catch (Exception e) {
                printLog(e.getMessage());
            }

            try {
                Thread.sleep(20 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        printLog("poll task ended");
    }

    public void callPollingServer() {

        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();

        UMFuturesClientImpl client = getClientImpl();

        parameters.put("symbol", PAIR_BTCUSDT);

        try {
            String result = client.account().positionInformation(parameters);
            //logger.info(result);

            JSONArray positionArray = new JSONArray(result);
            if (positionArray != null && positionArray.length() > 0) {
                JSONObject positionJSONobj = positionArray.getJSONObject(0);
                PositionItem positionItem = new PositionItem();
                positionItem.entryPrice = positionJSONobj.optDouble("entryPrice");
                positionItem.quantity = positionJSONobj.optDouble("positionAmt");
                positionItem.liquidationPrice = positionJSONobj.optDouble("liquidationPrice");
                positionItem.pnl = positionJSONobj.optDouble("unRealizedProfit");
                positionItem.leverage = positionJSONobj.optInt("leverage");
                positionItem.isLong = positionItem.entryPrice > positionItem.liquidationPrice;
            }
        } catch (BinanceConnectorException e) {
//            printLog("full ErrorMessage: " + e.getMessage());
        } catch (BinanceClientException e) {
//            printLog("full ErrorMessage: " + e.getMessage() + "\nerrMessage: " + e.getErrMsg() + "\nerrCode: " + e.getErrorCode() + "\nHTTPStatusCode: " + e.getHttpStatusCode());
        }
    }

    public void refreshCurrentLeverage() {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();

        UMFuturesClientImpl client = getClientImpl();

        parameters.put("symbol", PAIR_BTCUSDT);

        try {
            String result = client.account().positionInformation(parameters);
            //logger.info(result);

            JSONArray positionArray = new JSONArray(result);
            if (positionArray != null && positionArray.length() > 0) {
                JSONObject positionJSONobj = positionArray.getJSONObject(0);
                PositionItem positionItem = new PositionItem();
                positionItem.leverage = positionJSONobj.optInt("leverage");

                int leverage = positionItem.leverage;
                printLog("current lev is " + leverage);
                if (leverage > 0) Current_Leverage = leverage;
            }
        } catch (BinanceConnectorException e) {
            printLog("full ErrorMessage: " + e.getMessage(), true);
        } catch (BinanceClientException e) {
            printLog("full ErrorMessage: " + e.getMessage() + "\nerrMessage: " + e.getErrMsg() + "\nerrCode: " + e.getErrorCode() + "\nHTTPStatusCode: " + e.getHttpStatusCode(), true);
        }
    }

    public void displayCurrentPosition() {
        PositionItem positionItem = getCurrentPosition();
        if (positionItem != null) {
            printLog((positionItem.isLong ? "L" : "S") + " position open at " + positionItem.entryPrice + " " + positionItem.quantity);
        } else {
            printLog("   no open position");
        }
    }

    public PositionItem getCurrentPosition() {
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();

        UMFuturesClientImpl client = getClientImpl();

        parameters.put("symbol", PAIR_BTCUSDT);

        try {
            String result = client.account().positionInformation(parameters);
            //logger.info(result);

            JSONArray positionArray = new JSONArray(result);
            if (positionArray != null && positionArray.length() > 0) {
                JSONObject positionJSONobj = positionArray.getJSONObject(0);
                PositionItem positionItem = new PositionItem();
                positionItem.entryPrice = positionJSONobj.optDouble("entryPrice");
                positionItem.quantity = positionJSONobj.optDouble("positionAmt");
                positionItem.liquidationPrice = positionJSONobj.optDouble("liquidationPrice");
                positionItem.pnl = positionJSONobj.optDouble("unRealizedProfit");
                positionItem.leverage = positionJSONobj.optInt("leverage");
                positionItem.isLong = positionItem.entryPrice > positionItem.liquidationPrice;

                if (positionItem.quantity != 0) return positionItem;
            }
        } catch (BinanceConnectorException e) {
            printLog("full ErrorMessage: " + e.getMessage(), true);
        } catch (BinanceClientException e) {
            printLog("full ErrorMessage: " + e.getMessage() + "\nerrMessage: " + e.getErrMsg() + "\nerrCode: " + e.getErrorCode() + "\nHTTPStatusCode: " + e.getHttpStatusCode(), true);
        }
        return null;
    }

    public void escapeNow() {
        PositionItem positionItem = getCurrentPosition();
        if (positionItem != null) {
            exitWithMarket(positionItem);
            cancelAllOrders();
        }
    }

    public void exitWithMarket(PositionItem positionItem) {

        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();

        UMFuturesClientImpl client = getClientImpl();

        parameters.put("symbol", PAIR_BTCUSDT);
        parameters.put("side", positionItem.isLong ? SIDE_SELL : SIDE_BUY);
        parameters.put("type", TYPE_MARKET);
        parameters.put("quantity", Math.abs(positionItem.quantity));

        try {
            String result = client.account().newOrder(parameters);
            //logger.info(result);
        } catch (BinanceConnectorException e) {
            printLog("full ErrorMessage: " + e.getMessage(), true);
        } catch (BinanceClientException e) {
            printLog("full ErrorMessage: " + e.getMessage() + "\nerrMessage: " + e.getErrMsg() + "\nerrCode: " + e.getErrorCode() + "\nHTTPStatusCode: " + e.getHttpStatusCode(), true);
        }
    }
    
    public void placeSLForce() {
        if (Retain_SL_Task) {
            printLog("SL task already exists");
            return;
        }
        Retain_SL_Task = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                printLog("SL task started");
                placeCasualSL();
            }
        }).start();
    }

    public void placeCasualSL() {
        boolean slSuccess = false;
        while (!slSuccess) {
            PositionItem positionItem = getCurrentPosition();
            if (positionItem != null) {
                slSuccess = placeSL(positionItem);
            }
            try {
                Thread.sleep(5 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!Retain_SL_Task) {
                printLog("SL task was cancelled");
                break;
            }
        }
        Retain_SL_Task = false;
    }
    
    public boolean placeSLWhenOrder(boolean isLong, double entryPrice) {
        PositionItem positionItem = new PositionItem();
        positionItem.isLong = isLong;
        positionItem.entryPrice = entryPrice;

        return placeSL(positionItem);
    }

    public void placeTPForce() {
        if (Retain_TP_Task) {
            printLog("TP task already exists");
            return;
        }
        Retain_TP_Task = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                printLog("TP task started");
                placeTP();
            }
        }).start();
    }
    
    public void placeTP() {
        boolean tpSuccess = false;
        while (!tpSuccess) {
            PositionItem positionItem = getCurrentPosition();
            if (positionItem != null) {
                tpSuccess = placeTPOrder(positionItem);
            }
            try {
                Thread.sleep(5 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!Retain_TP_Task) {
                printLog("TP task was cancelled");
                break;
            }
        }
        Retain_TP_Task = false;
    }

    public void placeOrderCasual(boolean isLong, double entryPrice, boolean isMarket) {
        double entryPrice_now = entryPrice + PRICE_DIFF;
        boolean slPlaced = placeSLWhenOrder(isLong, entryPrice_now);
        if (slPlaced) {
            printLog("SL is set");
            placeOrder(isLong, entryPrice_now, false);
        } else {
            printLog("SL is not set");
        }
    }

    private boolean placeOrder(boolean isLong, double entryPrice, boolean isMarket) {

        entryPrice = Utils.truncateByOneDecimal(entryPrice);
        double quantity = 0;
        quantity = Utils.truncateByMinQuantity(Current_Balance * Current_Leverage / entryPrice);

        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();

        UMFuturesClientImpl client = getClientImpl();

        parameters.put("symbol", PAIR_BTCUSDT);
        parameters.put("side", isLong ? SIDE_BUY : SIDE_SELL);
        parameters.put("type", isMarket ? TYPE_MARKET : TYPE_LIMIT);
        parameters.put("timeInForce", "GTC");
        parameters.put("quantity", quantity);
        if (!isMarket)
            parameters.put("price", entryPrice);
        parameters.put("workingType", "CONTRACT_PRICE");

        try {
            String result = client.account().newOrder(parameters);

            JSONObject orderJsonItem = new JSONObject(result);
            long orderId = orderJsonItem.optLong("orderId");

            printLog((isLong ? "L" : "S") + " position placed order at " + entryPrice);
            return true;

        } catch (BinanceConnectorException e) {
            printLog("full ErrorMessage: " + e.getMessage(), true);
        } catch (BinanceClientException e) {
            printLog("full ErrorMessage: " + e.getMessage() + "\nerrMessage: " + e.getErrMsg() + "\nerrCode: " + e.getErrorCode() + "\nHTTPStatusCode: " + e.getHttpStatusCode(), true);
        }

        return false;
    }

    public boolean placeTPOrder(PositionItem positionItem) {

        double entryPrice = positionItem.entryPrice;
        double tpPrice = 0;

        if (positionItem.isLong) {
            tpPrice = Utils.truncateByOneDecimal(entryPrice / 100 * (100 + Current_TP / Current_Leverage));
        } else {
            tpPrice = Utils.truncateByOneDecimal(entryPrice / 100 * (100 - Current_TP / Current_Leverage));
        }

        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();

        UMFuturesClientImpl client = getClientImpl();

        parameters.put("symbol", PAIR_BTCUSDT);
        parameters.put("side", positionItem.isLong ? SIDE_SELL : SIDE_BUY);
        parameters.put("type", TYPE_TP_MARKET);
        parameters.put("stopPrice", tpPrice);
        parameters.put("closePosition", true);
        parameters.put("workingType", "CONTRACT_PRICE");

        try {
            String result = client.account().newOrder(parameters);

            JSONObject orderJsonItem = new JSONObject(result);
            long orderId = orderJsonItem.optLong("orderId");
            String orderType = orderJsonItem.optString("type");

            if (orderType.equals(TYPE_TP_MARKET) || orderType.equals(TYPE_TP_LIMIT)) {
                printLog("TP order id " + orderId);
                return true;
            }

        } catch (BinanceConnectorException e) {
            printLog("full ErrorMessage: " + e.getMessage(), true);
        } catch (BinanceClientException e) {
            printLog("full ErrorMessage: " + e.getMessage() + "\nerrMessage: " + e.getErrMsg() + "\nerrCode: " + e.getErrorCode() + "\nHTTPStatusCode: " + e.getHttpStatusCode(), true);
        }

        return false;
    }

    public boolean placeSL(PositionItem positionItem) {

        double entryPrice = positionItem.entryPrice;
        double slPrice = 0;

        if (positionItem.isLong) {
            slPrice = Utils.truncateByOneDecimal(entryPrice / 100 * (100 - Current_SL / Current_Leverage));
        } else {
            slPrice = Utils.truncateByOneDecimal(entryPrice / 100 * (100 + Current_SL / Current_Leverage));
        }

        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();

        UMFuturesClientImpl client = getClientImpl();

        parameters.put("symbol", PAIR_BTCUSDT);
        parameters.put("side", positionItem.isLong ? SIDE_SELL : SIDE_BUY);
        parameters.put("type", TYPE_SL_MARKET);
        parameters.put("stopPrice", slPrice);
        parameters.put("closePosition", true);
        parameters.put("workingType", "CONTRACT_PRICE");

        try {
            String result = client.account().newOrder(parameters);

            JSONObject orderJsonItem = new JSONObject(result);
            long orderId = orderJsonItem.optLong("orderId");
            String orderType = orderJsonItem.optString("type");

            if (orderType.equals(TYPE_SL_MARKET) || orderType.equals(TYPE_SL_LIMIT)) {
                printLog("SL order id " + orderId);
                return true;
            }
        } catch (BinanceConnectorException e) {
            printLog("full ErrorMessage: " + e.getMessage(), true);
        } catch (BinanceClientException e) {
            printLog("full ErrorMessage: " + e.getMessage() + "\nerrMessage: " + e.getErrMsg() + "\nerrCode: " + e.getErrorCode() + "\nHTTPStatusCode: " + e.getHttpStatusCode(), true);
        }
        return false;
    }

    private boolean placeTPorSL(boolean isTP, String side, double stopPrice) {

        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();

        UMFuturesClientImpl client = getClientImpl();

        parameters.put("symbol", PAIR_BTCUSDT);
        parameters.put("side", side);
        parameters.put("type", isTP ? TYPE_TP_MARKET : TYPE_SL_MARKET);
        parameters.put("stopPrice", stopPrice);
        parameters.put("closePosition", true);
        parameters.put("workingType", "CONTRACT_PRICE");

        try {
            String result = client.account().newOrder(parameters);

            JSONObject orderJsonItem = new JSONObject(result);
            long orderId = orderJsonItem.optLong("orderId");

            printLog((isTP ? "TP order id " : "SL order id ") + orderId);
            return true;
        } catch (BinanceConnectorException e) {
            printLog("full ErrorMessage: " + e.getMessage(), true);
        } catch (BinanceClientException e) {
            printLog("full ErrorMessage: " + e.getMessage() + "\nerrMessage: " + e.getErrMsg() + "\nerrCode: " + e.getErrorCode() + "\nHTTPStatusCode: " + e.getHttpStatusCode(), true);
        }
        return false;
    }

    public boolean cancelOrder(long orderId) {

        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();

        UMFuturesClientImpl client = getClientImpl();

        parameters.put("symbol", PAIR_BTCUSDT);
        parameters.put("orderId", orderId);

        try {
            String result = client.account().cancelOrder(parameters);
            JSONObject resultObj = new JSONObject(result);
            return resultObj.optString("status").equals("CANCELED");
        } catch (BinanceConnectorException e) {
            printLog("full ErrorMessage: " + e.getMessage(), true);
        } catch (BinanceClientException e) {
            printLog("full ErrorMessage: " + e.getMessage() + "\nerrMessage: " + e.getErrMsg() + "\nerrCode: " + e.getErrorCode() + "\nHTTPStatusCode: " + e.getHttpStatusCode(), true);
        }
        return false;
    }

    public void modifyOrder(boolean isTP, double price) {
        double price_now = price + PRICE_DIFF;
        PositionItem positionItem = getCurrentPosition();
        if (positionItem == null) {
            printLog("failed to get position");
            return;
        }

        ArrayList<OrderItem> orderItemArray = getOpenOrders();
        OrderItem currentOrderItem = null;
        if (orderItemArray != null) {
            for (OrderItem orderItem : orderItemArray) {
                if ((isTP && (orderItem.orderType.equals(TYPE_TP_LIMIT) || orderItem.orderType.equals(TYPE_TP_MARKET))) ||
                        (!isTP && (orderItem.orderType.equals(TYPE_SL_LIMIT) || orderItem.orderType.equals(TYPE_SL_MARKET)))){
                    currentOrderItem = orderItem;
                    break;
                }
            }
        }
        if (currentOrderItem == null) {
            printLog("failed to get order item");
        }

        if (!cancelOrder(currentOrderItem.orderId)) {
            printLog("failed to cancel existing order");
        }

        placeTPorSL(isTP, positionItem.isLong ? SIDE_SELL : SIDE_BUY, price_now);
    }
}

package com.billo;

import com.billo.util.*;
import com.bybit.api.client.config.BybitApiConfig;
import com.bybit.api.client.domain.CategoryType;
import com.bybit.api.client.domain.account.AccountType;
import com.bybit.api.client.domain.asset.WithBonus;
import com.bybit.api.client.domain.asset.request.AssetDataRequest;
import com.bybit.api.client.domain.position.TpslMode;
import com.bybit.api.client.domain.position.request.PositionDataRequest;
import com.bybit.api.client.domain.trade.PositionIdx;
import com.bybit.api.client.domain.trade.request.TradeOrderRequest;
import com.bybit.api.client.restApi.BybitApiAssetRestClient;
import com.bybit.api.client.restApi.BybitApiTradeRestClient;
import com.bybit.api.client.service.BybitApiClientFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.billo.PrivateConfig.PAIR_BTCUSDT;
import static com.billo.util.OrderItemBy.*;
import static com.billo.util.OrderItemBy.BY_STOP_TYPE_SL;

public class BySession extends PlatformSession {

    public BySession(String api_key, String secret_key) {
        super(PrivateConfig.PlatformVersion.By, api_key, secret_key);
    }

    private BybitApiClientFactory getBybitClientFactory() {
        BybitApiClientFactory bybitClientFactory = BybitApiClientFactory.newInstance(Api_Key, Secret_Key, BybitApiConfig.MAINNET_DOMAIN);
        return bybitClientFactory;
    }

    public void changeLeverage_By(int leverage) {
        var client = getBybitClientFactory().newPositionRestClient();
        var setLeverageRequest = PositionDataRequest.builder().category(CategoryType.LINEAR).symbol(PAIR_BTCUSDT).buyLeverage(String.valueOf(leverage)).sellLeverage(String.valueOf(leverage)).build();
        var actionResponse = client.setPositionLeverage(setLeverageRequest);
        LinkedHashMap<?, ?> responseData = getResultFromResponse(actionResponse);
        if (responseData == null) {
            printLog("set leverage failed", true);
            return;
        }
        Current_Leverage = leverage;
        printLog("set leverage to " + Current_Leverage);
    }

    public LinkedHashMap<?, ?> getResultFromResponse(Object responseData) {
        LinkedHashMap<?, ?> responseMap = (LinkedHashMap<?, ?>) responseData;
        int retCode = (int) responseMap.get("retCode");
        if (retCode != 0) {
            printLog(responseMap.get("retMsg").toString(), true);
            return null;
        }
        return (LinkedHashMap<?, ?>) responseMap.get("result");
    }

    public void displayBalance() {
        if (!isTurnedOn) return;
        double balanceBy = getBalance_By();
        if (this.sessionIndex == 0) {
            printLog("available balance = " + balanceBy);
        } else {
            printLog("batch amount = " + balanceBy);
        }
    }

    public double getBalance_By() {
        BybitApiAssetRestClient client = getBybitClientFactory().newAssetRestClient();
        var singleCoinBalanceRequest = AssetDataRequest.builder().accountType(AccountType.UNIFIED).coin("USDT").withBonus(WithBonus.QUERY).build();
        var singleCoinBalance = (LinkedHashMap<?, ?>)client.getAssetSingleCoinBalance(singleCoinBalanceRequest);
        LinkedHashMap<?, ?> data = getResultFromResponse(singleCoinBalance);
        if (data == null) return 0;
        double transferBalance = Utils.safeStringObjectToDouble(((LinkedHashMap<?, ?>)(data.get("balance"))).get("transferBalance"));

        Current_Balance = Math.floor(transferBalance * (100 - (2 * TAKER_FEE) * Current_Leverage) / 100);
        return transferBalance;
    }

    public void getLastPositionHistory_By() {
        var client = getBybitClientFactory().newPositionRestClient();
        var closePnlRequest = PositionDataRequest.builder().category(CategoryType.LINEAR).symbol(PAIR_BTCUSDT).build();
        var closePnlResponse = client.getClosePnlList(closePnlRequest);

        LinkedHashMap<?, ?> data = getResultFromResponse(closePnlResponse);
        if (data == null) {
            printLog("failed to get hits", true);
            return;
        }
        ArrayList<LinkedHashMap<?, ?>> pnlList = (ArrayList<LinkedHashMap<?, ?>>)(data.get("list"));
        if (pnlList == null || pnlList.size() == 0) {
            printLog("no pnl");
            return;
        }

        for (int i = 0; i < pnlList.size(); i ++) {
            LinkedHashMap<?, ?> pnlDataItem = pnlList.get(pnlList.size() - i - 1);
            long timeStamp = Utils.safeStringObjectToLong(pnlDataItem.get("updatedTime"));
            double closedPnl = Utils.safeStringObjectToDouble(pnlDataItem.get("closedPnl"));
            String side = (String)pnlDataItem.get("side");
            printLog(DateUtils.getDateString(timeStamp) + " " + (side.equalsIgnoreCase(BY_SIDE_SELL) ? "L" : "S") + " " + Utils.truncateByTwoDecimals(closedPnl));
        }
    }

    public void displayOpenOrders() {
        ArrayList<OrderItemBy> openOrders = getOpenOrders_By();
        if (openOrders != null && openOrders.size() > 0) {
            printLog("    " + openOrders.size() + " open order(s)");
            for (OrderItemBy orderItem : openOrders) {
                if (orderItem.stopOrderType.isEmpty()) {
                    printLog("    start at " + orderItem.price);
                } else if (orderItem.stopOrderType.equalsIgnoreCase(BY_STOP_TYPE_SL)) {
                    printLog("    sl at " + orderItem.triggerPrice);
                } else if (orderItem.stopOrderType.equalsIgnoreCase(OrderItemBy.BY_STOP_TYPE_TP)) {
                    printLog("    tp at " + orderItem.triggerPrice);
                } else {
                    printLog("    unknown order");
                }
            }
            openOrders.clear();
        } else {
            printLog("no open order");
        }
    }

    public ArrayList<OrderItemBy> getOpenOrders_By() {
        var client = getBybitClientFactory().newTradeRestClient();
        var openOrderRequest = TradeOrderRequest.builder().category(CategoryType.LINEAR).symbol(PAIR_BTCUSDT).openOnly(0).build();
        var openLinearOrdersResult = client.getOpenOrders(openOrderRequest);

        LinkedHashMap<?, ?> data = getResultFromResponse(openLinearOrdersResult);
        if (data == null) {
            printLog("failed to get open orders", true);
            return null;
        }

        ArrayList<LinkedHashMap<?, ?>> orderList = (ArrayList<LinkedHashMap<?, ?>>)(data.get("list"));
        if (orderList == null || orderList.size() == 0) {
            return null;
        }

        ArrayList<OrderItemBy> orderItemArray = new ArrayList<>();
        for (LinkedHashMap<?, ?> orderDataItem : orderList) {
            OrderItemBy orderItem = new OrderItemBy();
            orderItem.orderId = String.valueOf(orderDataItem.get("orderId"));
            orderItem.price = Utils.safeStringObjectToDouble(orderDataItem.get("price"));
            orderItem.stopLoss = Utils.safeStringObjectToDouble(orderDataItem.get("stopLoss"));
            orderItem.takeProfit = Utils.safeStringObjectToDouble(orderDataItem.get("takeProfit"));
            orderItem.triggerPrice = Utils.safeStringObjectToDouble(orderDataItem.get("triggerPrice"));
            orderItem.qty = Utils.safeStringObjectToDouble(orderDataItem.get("qty"));
            orderItem.leavesQty = Utils.safeStringObjectToDouble(orderDataItem.get("leavesQty"));
            orderItem.leavesValue = Utils.safeStringObjectToDouble(orderDataItem.get("leavesValue"));
            orderItem.orderType = (String)orderDataItem.get("orderType");
            orderItem.orderSide = (String)orderDataItem.get("side");
            orderItem.stopOrderType = (String)orderDataItem.get("stopOrderType");
            orderItemArray.add(orderItem);
        }
        return orderItemArray;
    }

    public void cancelAllOrders_By() {
        BybitApiTradeRestClient client = getBybitClientFactory().newTradeRestClient();
        var cancelAllOrdersRequest = TradeOrderRequest.builder().category(CategoryType.LINEAR).symbol(PAIR_BTCUSDT).build();
        Object responseData = client.cancelAllOrder(cancelAllOrdersRequest);
        LinkedHashMap<?, ?> data = getResultFromResponse(responseData);
        if (data == null)
            printLog("cancel all orders failed", true);
        Retain_SL_Task = false;
        Retain_TP_Task = false;
    }

    public void pollServer_By() {
        printLog("poll task started");
        Retain_Poll_Task = true;
        while (Retain_Poll_Task) {
            callPollingServer_By();

            try {
                Thread.sleep(20 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        printLog("poll task ended");
    }

    public void callPollingServer_By() {
        try {
            var client = getBybitClientFactory().newPositionRestClient();
            // Get Position Info
            var positionListRequest = PositionDataRequest.builder().category(CategoryType.LINEAR).symbol(PAIR_BTCUSDT).build();
            var positionListData = client.getPositionInfo(positionListRequest);

            getResultFromResponse(positionListData);
        } catch (Exception e) {
            printLog(e.getMessage());
        }
    }

    public void refreshCurrentLeverage_By() {
        var client = getBybitClientFactory().newPositionRestClient();
        // Get Position Info
        var positionListRequest = PositionDataRequest.builder().category(CategoryType.LINEAR).symbol(PAIR_BTCUSDT).build();
        var positionListData = client.getPositionInfo(positionListRequest);

        LinkedHashMap<?, ?> data = getResultFromResponse(positionListData);
        if (data == null) {
            printLog("failed to refresh current leverage", true);
            return;
        }

        ArrayList<LinkedHashMap<?, ?>> positionList = (ArrayList<LinkedHashMap<?, ?>>)(data.get("list"));
        if (positionList == null) {
            printLog("failed to refresh current leverage");
            return;
        }

        for (LinkedHashMap<?, ?> positionItem : positionList) {
            String symbolId = (String) positionItem.get("symbol");
            int leverage = Utils.safeStringObjectToInt(positionItem.get("leverage"));
            if (symbolId.equals(PAIR_BTCUSDT)) {
                Current_Leverage = leverage;
                printLog("current leverage is " + Current_Leverage);
                return;
            }
        }
    }

    public void displayCurrentPosition() {
        PositionItemBy positionItem = getCurrentPosition_By();
        if (positionItem != null && positionItem.avgPrice > 0) {
            printLog((positionItem.side.equalsIgnoreCase(OrderItemBy.BY_SIDE_BUY) ? "L" : "S") + " position open at " + positionItem.avgPrice + " " + positionItem.size);
        } else {
            printLog("   no open position");
        }
    }

    public PositionItemBy getCurrentPosition_By() {
        var client = getBybitClientFactory().newPositionRestClient();
        // Get Position Info
        var positionListRequest = PositionDataRequest.builder().category(CategoryType.LINEAR).symbol(PAIR_BTCUSDT).build();
        var positionListData = client.getPositionInfo(positionListRequest);

        LinkedHashMap<?, ?> data = getResultFromResponse(positionListData);
        if (data == null) {
            printLog("position retrieval failed", true);
        }

        ArrayList<LinkedHashMap<?, ?>> positionList = (ArrayList<LinkedHashMap<?, ?>>)(data.get("list"));
        if (positionList == null) return null;

        for (LinkedHashMap<?, ?> positionItemData : positionList) {
            String symbolId = (String) positionItemData.get("symbol");
            if (symbolId.equals(PAIR_BTCUSDT)) {
                PositionItemBy positionItem = new PositionItemBy();
                positionItem.leverage = Utils.safeStringObjectToInt(positionItemData.get("leverage"));
                positionItem.size = Utils.safeStringObjectToDouble(positionItemData.get("size"));
                positionItem.positionValue = Utils.safeStringObjectToDouble(positionItemData.get("positionValue"));
                positionItem.avgPrice = Utils.safeStringObjectToDouble(positionItemData.get("avgPrice"));
                positionItem.liqPrice = Utils.safeStringObjectToDouble(positionItemData.get("liqPrice"));
                positionItem.takeProfit = Utils.safeStringObjectToDouble(positionItemData.get("takeProfit"));
                positionItem.stopLoss = Utils.safeStringObjectToDouble(positionItemData.get("stopLoss"));
                positionItem.unrealisedPnl = Utils.safeStringObjectToDouble(positionItemData.get("unrealisedPnl"));
                positionItem.side = (String)positionItemData.get("side");
                return positionItem;
            }
        }
        return null;
    }

    public void escapeNow_By() {
        PositionItemBy positionItem = getCurrentPosition_By();
        if (positionItem != null && positionItem.avgPrice > 0) {
            exitWithMarket_By(positionItem);
            cancelAllOrders_By();
        }
    }

    public void exitWithMarket_By(PositionItemBy positionItem) {

        String side = positionItem.side.equalsIgnoreCase(BY_SIDE_BUY) ? BY_SIDE_SELL : BY_SIDE_BUY;

        BybitApiTradeRestClient client = getBybitClientFactory().newTradeRestClient();
        Map<String, Object> order = Map.of(
                "category", "linear",
                "symbol", PAIR_BTCUSDT,
                "side", side,
                "orderType", BY_TYPE_MARKET,
                "qty", "0",
                "reduceOnly", true,
                "closeOnTrigger", true
        );
        var exitResponse = client.createOrder(order);

        LinkedHashMap<?, ?> responseData = getResultFromResponse(exitResponse);
        if (responseData == null) {
            printLog("exit with market failed", true);
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
                placeSL_By();
            }
        }).start();
    }

    public void placeSL_By() {
        boolean slSuccess = false;
        while (!slSuccess) {
            PositionItemBy positionItem = getCurrentPosition_By();
            if (positionItem != null && positionItem.avgPrice > 0) {
                ArrayList<OrderItemBy> orderItemArray = getOpenOrders_By();
                if (orderItemArray != null) {
                    for (OrderItemBy orderItem : orderItemArray) {
                        if (orderItem.stopOrderType.equalsIgnoreCase(BY_STOP_TYPE_SL)){
                            slSuccess = true;
                            break;
                        }
                    }
                }
                if (!slSuccess) {
                    slSuccess = placeSLOrder_By(positionItem);
                }
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
                placeTP_By();
            }
        }).start();
    }

    public void placeTP_By() {
        boolean tpSuccess = false;
        while (!tpSuccess) {
            PositionItemBy positionItem = getCurrentPosition_By();
            if (positionItem != null && positionItem.avgPrice > 0) {
                ArrayList<OrderItemBy> orderItemArray = getOpenOrders_By();
                if (orderItemArray != null) {
                    for (OrderItemBy orderItem : orderItemArray) {
                        if (orderItem.stopOrderType.equalsIgnoreCase(BY_STOP_TYPE_TP)){
                            tpSuccess = true;
                            break;
                        }
                    }
                }
                if (!tpSuccess) {
                    tpSuccess = placeTPOrder_By(positionItem);
                }
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

    public boolean placeOrder_By(boolean isLong, double entryPrice, boolean isMarket) {
        entryPrice = Utils.truncateByOneDecimal(entryPrice);
        double quantity = 0;
        quantity = Utils.truncateByMinQuantity(Current_Balance * Current_Leverage / entryPrice);

        String side = isLong ? BY_SIDE_BUY : BY_SIDE_SELL;

        double slPrice;
        if (isLong) {
            slPrice = Utils.truncateByOneDecimal(entryPrice / 100 * (100 - Current_SL / Current_Leverage));
        } else {
            slPrice = Utils.truncateByOneDecimal(entryPrice / 100 * (100 + Current_SL / Current_Leverage));
        }

        double tpPrice;
        if (isLong) {
            tpPrice = Utils.truncateByOneDecimal(entryPrice / 100 * (100 + Current_TP / Current_Leverage));
        } else {
            tpPrice = Utils.truncateByOneDecimal(entryPrice / 100 * (100 - Current_TP / Current_Leverage));
        }

        BybitApiTradeRestClient client = getBybitClientFactory().newTradeRestClient();
        Map<String, Object> order = Map.of(
                "category", "linear",
                "symbol", PAIR_BTCUSDT,
                "side", side,
                "orderType", BY_TYPE_LIMIT,
                "qty", String.valueOf(quantity),
                "price", String.valueOf(entryPrice),
                "takeProfit", String.valueOf(tpPrice),
                "stopLoss", String.valueOf(slPrice)
        );
        var placeResponse = client.createOrder(order);

        LinkedHashMap<?, ?> responseData = getResultFromResponse(placeResponse);
        if (responseData == null) {
            printLog("place order failed", true);
            return false;
        }

        printLog((isLong ? "L" : "S") + " position placed order at " + entryPrice);
        return true;
    }

    public boolean placeTPOrder_By(PositionItemBy positionItem) {
        double entryPrice = positionItem.avgPrice;
        double tpPrice = 0;

        if (positionItem.side.equalsIgnoreCase(BY_SIDE_BUY)) {
            tpPrice = Utils.truncateByOneDecimal(entryPrice / 100 * (100 + Current_TP / Current_Leverage));
        } else {
            tpPrice = Utils.truncateByOneDecimal(entryPrice / 100 * (100 - Current_TP / Current_Leverage));
        }

        var client = getBybitClientFactory().newPositionRestClient();
        var setTradingStopRequest = PositionDataRequest.builder().category(CategoryType.LINEAR).symbol(PAIR_BTCUSDT).takeProfit(String.valueOf(tpPrice))
                .tpslMode(TpslMode.FULL).positionIdx(PositionIdx.ONE_WAY_MODE).build();
        var setTradingStopRes = client.setTradingStop(setTradingStopRequest);

        LinkedHashMap<?, ?> responseData = getResultFromResponse(setTradingStopRes);
        if (responseData == null) {
            printLog("place tp order failed", true);
            return false;
        }
        return true;
    }

    public boolean placeSLOrder_By(PositionItemBy positionItem) {
        double entryPrice = positionItem.avgPrice;
        double slPrice = 0;

        if (positionItem.side.equalsIgnoreCase(BY_SIDE_BUY)) {
            slPrice = Utils.truncateByOneDecimal(entryPrice / 100 * (100 - Current_SL / Current_Leverage));
        } else {
            slPrice = Utils.truncateByOneDecimal(entryPrice / 100 * (100 + Current_SL / Current_Leverage));
        }

        var client = getBybitClientFactory().newPositionRestClient();
        var setTradingStopRequest = PositionDataRequest.builder().category(CategoryType.LINEAR).symbol(PAIR_BTCUSDT).stopLoss(String.valueOf(slPrice))
                .tpslMode(TpslMode.FULL).positionIdx(PositionIdx.ONE_WAY_MODE).build();
        var setTradingStopRes = client.setTradingStop(setTradingStopRequest);

        LinkedHashMap<?, ?> responseData = getResultFromResponse(setTradingStopRes);
        if (responseData == null) {
            printLog("place sl order failed", true);
            return false;
        }
        return true;
    }

    public boolean cancelOrder_By(String orderId) {
        BybitApiTradeRestClient client = getBybitClientFactory().newTradeRestClient();
        var cancelOrderRequest = TradeOrderRequest.builder().category(CategoryType.LINEAR).symbol(PAIR_BTCUSDT).orderId(orderId).build();
        var canceledOrder = client.cancelOrder(cancelOrderRequest);

        LinkedHashMap<?, ?> responseData = getResultFromResponse(canceledOrder);
        if (responseData == null) {
            printLog("cancel order failed", true);
            return false;
        }
        return true;
    }

    public void modifyOrderItem_By(String orderId, double price) {
        BybitApiTradeRestClient client = getBybitClientFactory().newTradeRestClient();
        var amendOrderRequest = TradeOrderRequest.builder().orderId(orderId).category(CategoryType.LINEAR).symbol(PAIR_BTCUSDT).triggerPrice(String.valueOf(price)).build();
        var amendedOrder = client.amendOrder(amendOrderRequest);

        LinkedHashMap<?, ?> responseData = getResultFromResponse(amendedOrder);
        if (responseData == null) {
            printLog("modify order failed", true);
        } else {
            printLog("modified order " + orderId);
        }
    }

    public void modifyOrder_By(boolean isTP, double price) {
        PositionItemBy positionItem = getCurrentPosition_By();
        if (positionItem == null) {
            printLog("failed to get position", true);
            return;
        }
        ArrayList<OrderItemBy> orderItemArray = getOpenOrders_By();
        OrderItemBy currentOrderItem = null;
        if (orderItemArray != null) {
            for (OrderItemBy orderItem : orderItemArray) {
                if ((isTP && (orderItem.stopOrderType.equalsIgnoreCase(BY_STOP_TYPE_TP))) ||
                        (!isTP && (orderItem.stopOrderType.equalsIgnoreCase(BY_STOP_TYPE_SL)))){
                    currentOrderItem = orderItem;
                    break;
                }
            }
        }
        if (currentOrderItem == null) {
            printLog("failed to get order item", true);
        }
        modifyOrderItem_By(currentOrderItem.orderId, price);
    }
}

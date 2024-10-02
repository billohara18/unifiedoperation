package com.billo.util;

public class Utils {

    public static final double MIN_QUANTITY = 0.003;

    public static double truncateByOneDecimal(double value) {
        return Math.floor(value * 10) / 10;
    }

    public static double truncateByTwoDecimals(double value) {
        return Math.floor(value * 100) / 100;
    }

    public static double truncateByMinQuantity(double value) {
//        return value - value % MIN_QUANTITY;
        return Math.floor(value * 1000) / 1000;
    }

    public static double safeStringObjectToDouble(Object value) {
        String valueStr = (String)value;
        double retValue = 0;
        try {
            retValue = Double.valueOf(valueStr);
        } catch (NumberFormatException e) {

        }
        return retValue;
    }

    public static int safeStringObjectToInt(Object value) {
        String valueStr = (String)value;
        int retValue = 0;
        try {
            retValue = Integer.valueOf(valueStr);
        } catch (NumberFormatException e) {
        }
        return retValue;
    }

    public static long safeStringObjectToLong(Object value) {
        String valueStr = (String)value;
        long retValue = 0;
        try {
            retValue = Long.valueOf(valueStr);
        } catch (NumberFormatException e) {
        }
        return retValue;
    }

    public static boolean isInteger(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            int d = Integer.parseInt(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
}

package com.billo;

public class PlatformSession {

    public int sessionIndex;

    public PrivateConfig.PlatformVersion version = PrivateConfig.PlatformVersion.None;
    protected String Api_Key;

    protected String Secret_Key;

    protected int Current_Leverage = 22;
    protected double Current_Balance = 8;

    protected double Current_TP = 20;
    protected double Current_SL = 10;

    protected boolean Retain_TP_Task = false;
    protected boolean Retain_SL_Task = false;
    protected boolean Retain_Poll_Task = false;

    protected final double MAKER_FEE = 0.02;
    protected final double TAKER_FEE = 0.055;

    public boolean isPrintable = true;
    public boolean isTurnedOn = true;

    public PlatformSession(PrivateConfig.PlatformVersion version, String api_key, String secret_key) {
        this.version = version;
        Api_Key = api_key;
        Secret_Key = secret_key;
    }

    public void setSize(double size) {
        Current_Balance = size;
        printLog("set size to " + size);
    }

    public void setTP(double tp) {
        Current_TP = tp;
        printLog("set TP to " + Current_TP);
    }

    public void showTP() {
        printLog("current TP is " + Current_TP);
    }

    public void setSL(double sl) {
        Current_SL = sl;
        printLog("set SL to " + sl);
    }

    public void showSL() {
        printLog("current SL is " + Current_SL);
    }

    public void cancelTP() {
        Retain_TP_Task = false;
    }

    public void cancelSL() {
        Retain_SL_Task = false;
    }

    public void exit() {
        Retain_TP_Task = false;
        Retain_SL_Task = false;
        Retain_Poll_Task = false;
    }

    public void printLog(String log, boolean forcePrint) {
        int maxLength = 3;
        String prefix = "";
        for (int i = 0; i < sessionIndex + 1; i ++) {
            prefix += "+";
        }
        String surfix = "";
        for (int i = 0; i < maxLength - sessionIndex - 1; i ++) {
            surfix += " ";
        }

        String tag = prefix + surfix;
        if (isPrintable || forcePrint)
            System.out.println(tag + log);
    }

    public void printLog(String log) {
        printLog(log, false);
    }
}

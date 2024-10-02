package com.billo;

import java.util.ArrayList;
import java.util.Map;

public final class PrivateConfig {

    public static String PAIR_BTCUSDT = "BTCUSDT";

    public enum PlatformVersion {
        None("NONE"),
        By("BY"),
        Bn("BN");

        private final String versionId;

        PlatformVersion(String versionId) {
            this.versionId = versionId;
        }

        public String getVersionId() {
            return this.versionId;
        }

        public static PlatformVersion getByString(String versionStr) {
            for (PlatformVersion b : PlatformVersion.values()) {
                if (b.getVersionId().equalsIgnoreCase(versionStr)) {
                    return b;
                }
            }
            return None;
        }
    }

    public enum SessionDataField {
        API_VERSION,
        API_KEY,
        SECRET_KEY
    }

    public static ArrayList<Map<Object, Object>> platformSessionDataArray= new ArrayList<>();

    static {
        platformSessionDataArray.add(Map.of(
                SessionDataField.API_VERSION, PlatformVersion.By,
                SessionDataField.API_KEY, "xxxxxxxxxxx",
                SessionDataField.SECRET_KEY, "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
        ));

        platformSessionDataArray.add(Map.of(
                SessionDataField.API_VERSION, PlatformVersion.By,
                SessionDataField.API_KEY, "xxxxxxxxxxx",
                SessionDataField.SECRET_KEY, "xxxxxxxxxxxxxxxxxxxxxxxx"
        ));

//        platformSessionDataArray.add(Map.of(
//                SessionDataField.API_VERSION, PlatformVersion.Bn,
//                SessionDataField.API_KEY, "xxxxxxxxx",
//                SessionDataField.SECRET_KEY, "xxxxxxxxxxxxxxxxxxxx"
//        ));
    }

    // bn
    public static final String UM_BASE_URL = "https://fapi.binance.com";
}
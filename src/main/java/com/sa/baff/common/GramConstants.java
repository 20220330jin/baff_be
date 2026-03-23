package com.sa.baff.common;

public class GramConstants {

    public static final String GRAM_NAME = "g";
    public static final String GRAM_UNIT = "";

    /** 최소 환전 금액 */
    public static final int EXCHANGE_MIN = 100;
    /** 최대 환전 금액 (1회) */
    public static final int EXCHANGE_MAX = 1000;
    /** 환전 비율 (1g = 1 토스포인트) */
    public static final int EXCHANGE_RATE = 1;

    public static String format(int amount) {
        return amount + GRAM_NAME;
    }

    public static String earnMessage(int amount) {
        return format(amount) + "을 받았어요!";
    }
}

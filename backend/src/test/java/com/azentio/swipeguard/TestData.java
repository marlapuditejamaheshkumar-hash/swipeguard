package com.azentio.swipeguard;

import java.time.Instant;
import java.util.Random;

/** Generates Luhn-valid synthetic PANs and request bodies. No real card data. */
public final class TestData {

    private static final Random RANDOM = new Random();

    private TestData() { }

    public static String randomPan() {
        StringBuilder pan = new StringBuilder("411111");
        while (pan.length() < 15) {
            pan.append(RANDOM.nextInt(10));
        }
        return pan.append(checkDigit(pan.toString())).toString();
    }

    private static int checkDigit(String payload) {
        int sum = 0;
        boolean doubleIt = true;
        for (int i = payload.length() - 1; i >= 0; i--) {
            int digit = payload.charAt(i) - '0';
            if (doubleIt) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleIt = !doubleIt;
        }
        return (10 - sum % 10) % 10;
    }

    public static String uniqueId() {
        return "tx-" + Long.toHexString(RANDOM.nextLong());
    }

    public static String json(String transactionId, String pan, String amount, String mcc,
                              String country, String channel, Instant timestamp) {
        return """
                {"transactionId":"%s","pan":"%s","amount":%s,"currency":"INR","mcc":"%s",\
                "merchantCountry":"%s","channel":"%s","timestamp":"%s"}"""
                .formatted(transactionId, pan, amount, mcc, country, channel, timestamp);
    }
}
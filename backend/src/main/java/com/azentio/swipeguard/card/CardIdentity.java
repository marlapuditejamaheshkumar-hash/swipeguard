package com.azentio.swipeguard.card;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** The only place that touches the full PAN: turns it into a safe cardRef and a masked PAN. */
@Component
public class CardIdentity {

    private static final String ALGORITHM = "HmacSHA256";
    private final SecretKeySpec key;

    public CardIdentity(@Value("${swipeguard.card-hmac-secret}") String secret) {
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    /** HMAC-SHA256(PAN) as hex. Same PAN always gives the same ref; the PAN cannot be recovered. */
    public String cardRef(String pan) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(pan.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is not available", e);
        }
    }

    /** First 6 + stars + last 4, e.g. 411111******1111. */
    public static String mask(String pan) {
        if (pan == null || pan.length() < 10) {
            return "****";
        }
        return pan.substring(0, 6) + "*".repeat(pan.length() - 10) + pan.substring(pan.length() - 4);
    }
}
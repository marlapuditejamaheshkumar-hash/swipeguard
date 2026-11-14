package com.azentio.swipeguard.authorisation;

import com.azentio.swipeguard.card.CardIdentity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record AuthorisationRequest(
        @NotBlank @Size(max = 64) String transactionId,
        @NotNull @Pattern(regexp = "\\d{16,19}", message = "must be 16-19 digits") @LuhnValid String pan,
        @NotNull @Positive BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter currency code") String currency,
        @NotNull @Pattern(regexp = "\\d{4}", message = "must be 4 digits") String mcc,
        @NotNull @Pattern(regexp = "[A-Z]{2}", message = "must be a 2-letter country code") String merchantCountry,
        @NotNull Channel channel,
        @NotNull Instant timestamp) {

    /** Overridden so the PAN can never leak through an accidental log of this object. */
    @Override
    public String toString() {
        return "AuthorisationRequest[transactionId=" + transactionId
                + ", pan=" + CardIdentity.mask(pan) + ", amount=" + amount + "]";
    }
}
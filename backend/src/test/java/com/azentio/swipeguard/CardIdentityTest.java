package com.azentio.swipeguard;

import static org.assertj.core.api.Assertions.assertThat;

import com.azentio.swipeguard.card.CardIdentity;
import org.junit.jupiter.api.Test;

class CardIdentityTest {

    private final CardIdentity identity = new CardIdentity("test-secret");

    @Test
    void masksFirstSixAndLastFour() {
        assertThat(CardIdentity.mask("4111111111111111")).isEqualTo("411111******1111");
    }

    @Test
    void cardRefIsStableHexAndHidesThePan() {
        String ref = identity.cardRef("4111111111111111");
        assertThat(ref).isEqualTo(identity.cardRef("4111111111111111")).hasSize(64).matches("[0-9a-f]+");
        assertThat(ref).doesNotContain("4111111111111111");
        assertThat(ref).isNotEqualTo(new CardIdentity("other-secret").cardRef("4111111111111111"));
    }
}
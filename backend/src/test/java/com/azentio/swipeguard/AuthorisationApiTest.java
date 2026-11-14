package com.azentio.swipeguard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.azentio.swipeguard.card.CardIdentity;
import com.azentio.swipeguard.velocity.VelocityStore;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class AuthorisationApiTest {

    private static final Instant START = Instant.parse("2026-11-14T06:00:00Z");

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired VelocityStore velocityStore;
    @Autowired CardIdentity cardIdentity;

    private String check(String body) throws Exception {
        return mvc.perform(post("/api/v1/authorisations/check").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private List<String> ruleNames(String responseJson) {
        return JsonPath.read(responseJson, "$.reasons[*].rule");
    }

    @Test
    void invalidInputReturns400WithoutEchoingThePan(CapturedOutput output) throws Exception {
        String badLuhnPan = "4111111111111112";
        String body = mvc.perform(post("/api/v1/authorisations/check").contentType(MediaType.APPLICATION_JSON)
                        .content(TestData.json("t-bad", badLuhnPan, "100", "5411", "IN", "POS", START)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[*].field", hasItem("pan")))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain(badLuhnPan);
        assertThat(output.getAll()).doesNotContain(badLuhnPan);
    }

    @Test
    void otherInvalidFieldsReturn400() throws Exception {
        String pan = TestData.randomPan();
        List<String> bodies = List.of(
                TestData.json("t1", pan, "-5", "5411", "IN", "POS", START),     // amount not positive
                TestData.json("t2", pan, "100", "12", "IN", "POS", START),      // MCC not 4 digits
                TestData.json("t3", pan, "100", "5411", "IND", "POS", START),   // country not 2 letters
                TestData.json("t4", "1234", "100", "5411", "IN", "POS", START), // PAN too short
                "{\"transactionId\":\"t5\"}");                                  // missing fields
        for (String body : bodies) {
            mvc.perform(post("/api/v1/authorisations/check").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void velocityFiresOnlyOnTheSixthAuthorisationAndReplayIsIdempotent() throws Exception {
        String pan = TestData.randomPan();
        String cardRef = cardIdentity.cardRef(pan);
        String lastBody = null;
        String lastResponse = null;

        for (int i = 1; i <= 6; i++) {
            lastBody = TestData.json(TestData.uniqueId(), pan, "100", "5411", "IN", "POS", START.plusSeconds(i * 60L));
            lastResponse = check(lastBody);
            assertThat(ruleNames(lastResponse).contains("VELOCITY")).as("authorisation #%d", i).isEqualTo(i == 6);
        }

        long countBefore = velocityStore.size(cardRef);
        String replay = check(lastBody); // same transactionId again
        assertThat(replay).isEqualTo(lastResponse);
        assertThat(velocityStore.size(cardRef)).isEqualTo(countBefore).isEqualTo(6);
    }

    @Test
    void fullPanNeverAppearsInDatabaseResponseOrLogs(CapturedOutput output) throws Exception {
        String pan = TestData.randomPan();
        String txId = TestData.uniqueId();
        String response = check(TestData.json(txId, pan, "60000", "5411", "IN", "POS", START)); // REVIEW + alert

        assertThat(response).doesNotContain(pan).contains(CardIdentity.mask(pan));

        List<Map<String, Object>> authorisationRows =
                jdbc.sql("SELECT * FROM card_authorisation WHERE transaction_id = :id").param("id", txId).query().listOfRows();
        List<Map<String, Object>> alertRows =
                jdbc.sql("SELECT * FROM alert WHERE transaction_id = :id").param("id", txId).query().listOfRows();
        assertThat(authorisationRows).hasSize(1);
        assertThat(alertRows).hasSize(1);
        assertThat(authorisationRows.toString()).doesNotContain(pan);
        assertThat(alertRows.toString()).doesNotContain(pan);

        assertThat(output.getAll()).doesNotContain(pan);
    }

    @Test
    void reviewCreatesAlertAndApproveDoesNot() throws Exception {
        String approve = check(TestData.json(TestData.uniqueId(), TestData.randomPan(), "100", "5411", "IN", "POS", START));
        assertThat((Object) JsonPath.read(approve, "$.alertId")).isNull();
        assertThat((String) JsonPath.read(approve, "$.decision")).isEqualTo("APPROVE");

        String review = check(TestData.json(TestData.uniqueId(), TestData.randomPan(), "60000", "5411", "IN", "POS", START));
        assertThat((String) JsonPath.read(review, "$.decision")).isEqualTo("REVIEW");
        assertThat((Object) JsonPath.read(review, "$.alertId")).isNotNull();
    }

    @Test
    void alertsAreListedNewestFirst() throws Exception {
        String first = TestData.uniqueId();
        String second = TestData.uniqueId();
        check(TestData.json(first, TestData.randomPan(), "60000", "5411", "IN", "POS", START));
        check(TestData.json(second, TestData.randomPan(), "60000", "5411", "IN", "POS", START));

        mvc.perform(get("/api/v1/alerts?status=OPEN&page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].transactionId").value(second))
                .andExpect(jsonPath("$.content[1].transactionId").value(first))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"));
    }

    @Test
    void invalidAlertPagingReturns400() throws Exception {
        mvc.perform(get("/api/v1/alerts?size=0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/alerts?status=NOPE")).andExpect(status().isBadRequest());
    }
}
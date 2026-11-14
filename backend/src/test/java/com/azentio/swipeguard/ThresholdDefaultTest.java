package com.azentio.swipeguard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ThresholdDefaultTest {

    @Autowired MockMvc mvc;

    @Test
    void amount60000IsReviewedWithTheDefaultThreshold() throws Exception {
        mvc.perform(post("/api/v1/authorisations/check").contentType(MediaType.APPLICATION_JSON)
                        .content(TestData.json(TestData.uniqueId(), TestData.randomPan(), "60000", "5411", "IN", "POS",
                                Instant.parse("2026-11-14T06:00:00Z"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("REVIEW"));
    }
}
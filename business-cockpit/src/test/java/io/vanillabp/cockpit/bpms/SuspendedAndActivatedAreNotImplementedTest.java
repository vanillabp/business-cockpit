package io.vanillabp.cockpit.bpms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

/**
 * The cockpit does nothing with a report that a user task was suspended or activated again.
 * <p>
 * The controllers do not implement the two operations, so the generated interface answers
 * {@code 501 Not Implemented}. The BPMS API says so for both operations. The sender in
 * {@code extensions-commons} gives up a report answered with 501, because the next answer is the
 * same. This test makes sure that the answer is still 501, so that what the specification says
 * stays true.
 */
@ExtendWith(SuppressOutputExtension.class)
class SuspendedAndActivatedAreNotImplementedTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @ParameterizedTest(name = "version {0}, {1}")
    @CsvSource({ "v1, suspended", "v1, activated", "v1_1, suspended", "v1_1, activated" })
    @DisplayName("A report that a user task was suspended or activated is answered with 501")
    void theReportIsAnsweredWithNotImplemented(
            final String version,
            final String kind) throws Exception {

        final Object controller = "v1".equals(version)
                ? new io.vanillabp.cockpit.bpms.api.v1.BpmsApiController()
                : new io.vanillabp.cockpit.bpms.api.v1_1.BpmsApiController();
        final var client = MockMvcBuilders.standaloneSetup(controller).build();
        final var report = Map.of(
                "id", "event-1",
                "userTaskId", "task-1",
                "timestamp", "2026-10-04T09:00:00Z");

        final var answer = client
                .perform(post("/bpms/api/%s/usertask/task-1/%s".formatted(version, kind))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(report)))
                .andReturn()
                .getResponse();

        assertEquals(501, answer.getStatus(), "the answer to a report the cockpit does nothing with");

    }

}

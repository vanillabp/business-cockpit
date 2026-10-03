package io.vanillabp.cockpit.config.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import io.vanillabp.cockpit.commons.exceptions.RestfulExceptionHandler;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * What the cockpit's exception handler answers when Spring validates a parameter of a controller
 * method because the parameter carries a constraint itself. No API of the cockpit has such a
 * parameter today. A controller added later gets the same answer as a body which breaks its
 * schema: {@code 400 Bad Request} with the parameter and the rule, and not the {@code 500} of the
 * catch-all.
 */
@ExtendWith(SuppressOutputExtension.class)
class AnInvalidParameterTest {

    @RestController
    static class ControllerWithAConstrainedParameter {

        @GetMapping("/constrained/{id}")
        public String read(
                @PathVariable("id") @Size(max = 3) final String id) {

            return id;

        }

    }

    private MockMvc client;

    @BeforeEach
    void setUp() throws Exception {

        final var exceptionHandler = new RestfulExceptionHandler();
        final var logger = RestfulExceptionHandler.class.getDeclaredField("logger");
        logger.setAccessible(true);
        logger.set(exceptionHandler, LoggerFactory.getLogger(AnInvalidParameterTest.class));

        client = MockMvcBuilders
                .standaloneSetup(new ControllerWithAConstrainedParameter())
                .setControllerAdvice(exceptionHandler)
                .build();

    }

    @Test
    void aParameterWhichBreaksItsRuleIsRefusedAndNamed() throws Exception {

        final var answer = client.perform(get("/constrained/toolong")).andReturn();

        assertEquals(400, answer.getResponse().getStatus());
        assertEquals(
                "The request is not valid: 'id' breaks the rule @Size.",
                answer.getResponse().getContentAsString());

    }

    /** The positive twin: a parameter which keeps its rule goes through. */
    @Test
    void aParameterWhichKeepsItsRuleGoesThrough() throws Exception {

        final var answer = client.perform(get("/constrained/abc")).andReturn();

        assertEquals(200, answer.getResponse().getStatus());
        assertEquals("abc", answer.getResponse().getContentAsString());

    }

}

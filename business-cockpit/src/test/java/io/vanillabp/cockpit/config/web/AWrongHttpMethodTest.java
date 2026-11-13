package io.vanillabp.cockpit.config.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.vanillabp.cockpit.exceptions.RestfulExceptionHandler;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What the cockpit's exception handler answers when a client uses an HTTP method a path does not
 * take, or sends a content type the path does not read. It is a mistake of the client, so the
 * answer is the one Spring gives without the handler: {@code 405} with the header {@code Allow},
 * and {@code 415} with the header {@code Accept}. Not the {@code 500} of the catch-all, which
 * would look like a fault of the cockpit.
 */
@ExtendWith(SuppressOutputExtension.class)
class AWrongHttpMethodTest {

  @RestController
  static class ControllerLikeTheGuiApi {

    @PostMapping("/user")
    public String user() {

      return "me";

    }

    @PatchMapping(path = "/usertask/{id}/mark-as-read", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String markAsRead(
        @RequestBody final String body) {

      return "done";

    }

  }

  private MockMvc client;

  @BeforeEach
  void setUp() {

    client = MockMvcBuilders
        .standaloneSetup(new ControllerLikeTheGuiApi())
        .setControllerAdvice(new RestfulExceptionHandler())
        .build();

  }

  @Test
  void aGetOnAPathWhichOnlyTakesPostAnswers405() throws Exception {

    final var answer = client.perform(get("/user")).andReturn().getResponse();

    assertEquals(405, answer.getStatus());
    assertEquals("POST", answer.getHeader("Allow"));

  }

  @Test
  void aPostOnAPathWhichOnlyTakesPatchAnswers405() throws Exception {

    final var answer = client.perform(post("/usertask/1/mark-as-read")).andReturn().getResponse();

    assertEquals(405, answer.getStatus());
    assertEquals("PATCH", answer.getHeader("Allow"));

  }

  @Test
  void aContentTypeThePathDoesNotReadAnswers415() throws Exception {

    final var answer = client
        .perform(patch("/usertask/1/mark-as-read").contentType(MediaType.TEXT_PLAIN).content("x"))
        .andReturn()
        .getResponse();

    assertEquals(415, answer.getStatus());
    assertEquals("application/json", answer.getHeader("Accept"));

  }

  /** The positive twin: the right method and content type go through. */
  @Test
  void theRightMethodGoesThrough() throws Exception {

    final var answer = client
        .perform(patch("/usertask/1/mark-as-read").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andReturn()
        .getResponse();

    assertEquals(200, answer.getStatus());

  }

}

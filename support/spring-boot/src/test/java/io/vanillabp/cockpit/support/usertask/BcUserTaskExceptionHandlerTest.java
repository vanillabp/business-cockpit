package io.vanillabp.cockpit.support.usertask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A controller of a workflow module throws one of the exceptions of a user task, and the handler turns it into
 * the status and the message a form can show.
 */
@ExtendWith(SuppressOutputExtension.class)
public class BcUserTaskExceptionHandlerTest {

  /**
   * Stands in for the form controller of a workflow module.
   */
  @RestController
  static class FormController {

    @PostMapping("/unknown/{workflowId}/{taskId}")
    String unknown(
        @PathVariable final String workflowId,
        @PathVariable final String taskId) {

      throw new BcUnknownTaskException(workflowId, taskId);

    }

    @PostMapping("/done/{workflowId}/{taskId}")
    String done(
        @PathVariable final String workflowId,
        @PathVariable final String taskId) {

      throw new BcTaskNotOpenException(workflowId, taskId);

    }

    @PostMapping("/claimed/{workflowId}/{taskId}/{claimedBy}")
    String claimed(
        @PathVariable final String workflowId,
        @PathVariable final String taskId,
        @PathVariable final String claimedBy) {

      throw new BcTaskClaimedByAnotherUserException(workflowId, taskId, claimedBy);

    }

  }

  private MockMvc mockMvc;

  @BeforeEach
  public void setUp() {

    mockMvc = MockMvcBuilders
        .standaloneSetup(new FormController())
        .setControllerAdvice(new BcUserTaskExceptionHandler())
        .build();

  }

  @Test
  public void anUnknownTaskIsNotFound() throws Exception {

    final var response = mockMvc
        .perform(post("/unknown/loan-1/task-9"))
        .andExpect(status().is(BcUnknownTaskException.HTTP_STATUS_CODE))
        .andReturn()
        .getResponse();

    assertThat(response.getContentAsString()).isEqualTo("There is no such user task 'task-9' in 'loan-1'");

  }

  @Test
  public void aTaskWhichIsNotOpenAnyMoreIsNotFound() throws Exception {

    final var response = mockMvc
        .perform(post("/done/loan-1/task-9"))
        .andExpect(status().is(BcTaskNotOpenException.HTTP_STATUS_CODE))
        .andReturn()
        .getResponse();

    assertThat(response.getContentAsString()).isEqualTo("The user task 'task-9' of 'loan-1' is not open any more");

  }

  @Test
  public void aTaskWhichAnotherUserClaimedIsAConflict() throws Exception {

    final var response = mockMvc
        .perform(post("/claimed/loan-1/task-9/mary"))
        .andExpect(status().is(BcTaskClaimedByAnotherUserException.HTTP_STATUS_CODE))
        .andReturn()
        .getResponse();

    assertThat(response.getContentType()).startsWith("text/plain");
    assertThat(response.getContentAsString()).isEqualTo("The user task 'task-9' of 'loan-1' is claimed by 'mary'");

  }

}

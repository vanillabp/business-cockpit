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
 * A controller of a workflow module throws one of the two exceptions, and the handler turns it into
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
        .andExpect(status().isNotFound())
        .andReturn()
        .getResponse();

    assertThat(response.getContentAsString()).isEqualTo("There is no such user task 'task-9' in 'loan-1'");

  }

  @Test
  public void aTaskWhichIsNotOpenAnyMoreIsAConflict() throws Exception {

    final var response = mockMvc
        .perform(post("/done/loan-1/task-9"))
        .andExpect(status().isConflict())
        .andReturn()
        .getResponse();

    assertThat(response.getContentAsString()).isEqualTo("The user task 'task-9' of 'loan-1' is not open any more");

  }

  @Test
  public void theExceptionsKeepTheIdsTheRequestNamed() {

    final var unknown = new BcUnknownTaskException("loan-1", "task-9");
    final var notOpen = new BcTaskNotOpenException("loan-2", "task-8");

    assertThat(unknown.getWorkflowId()).isEqualTo("loan-1");
    assertThat(unknown.getTaskId()).isEqualTo("task-9");
    assertThat(notOpen.getWorkflowId()).isEqualTo("loan-2");
    assertThat(notOpen.getTaskId()).isEqualTo("task-8");

  }

}

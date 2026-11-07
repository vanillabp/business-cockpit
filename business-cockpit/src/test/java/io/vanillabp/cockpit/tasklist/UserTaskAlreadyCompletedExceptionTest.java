package io.vanillabp.cockpit.tasklist;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The exception is raised for a task which ended in any way, completed or cancelled. Its message
 * goes to the log, and it must not claim a completion which did not happen.
 */
@ExtendWith(SuppressOutputExtension.class)
class UserTaskAlreadyCompletedExceptionTest {

  @Test
  void theMessageSaysTheTaskHasEnded() {

    final var exception = new UserTaskAlreadyCompletedException("task-1");

    assertThat(exception.getMessage()).isEqualTo("User task 'task-1' has ended already");
    assertThat(exception.getUserTaskId()).isEqualTo("task-1");

  }

}

package io.vanillabp.cockpit.support.usertask;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The two exceptions of a user task keep the ids the request named, so a handler on either
 * platform can log them or show them.
 */
@ExtendWith(SuppressOutputExtension.class)
public class TheTaskExceptionsKeepTheIdsTest {

  @Test
  public void theExceptionsKeepTheIdsTheRequestNamed() {

    final var unknown = new BcUnknownTaskException("loan-1", "task-9");
    final var notOpen = new BcTaskNotOpenException("loan-2", "task-8");

    assertThat(unknown.getWorkflowId()).isEqualTo("loan-1");
    assertThat(unknown.getTaskId()).isEqualTo("task-9");
    assertThat(notOpen.getWorkflowId()).isEqualTo("loan-2");
    assertThat(notOpen.getTaskId()).isEqualTo("task-8");

  }

  @Test
  public void theMessagesNameBothIds() {

    assertThat(new BcUnknownTaskException("loan-1", "task-9"))
        .hasMessage("There is no such user task 'task-9' in 'loan-1'");
    assertThat(new BcTaskNotOpenException("loan-1", "task-9"))
        .hasMessage("The user task 'task-9' of 'loan-1' is not open any more");

  }

}

package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The official API answers 404 for a user task or a workflow the simulator does not hold, as the
 * specification of the API says and the cockpit does. The controllers are called directly, past the
 * login.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:an-unknown-id-answers-404",
    // the simulator does not start without a user to log in as
    "dev-shell-simulator.users[0].id=john"
})
class AnUnknownIdAnswers404Test {

  @Autowired
  private OfficialApiTaskController tasks;

  @Autowired
  private OfficialApiWorkflowController workflows;

  @Test
  void anUnknownUserTaskAnswers404() {

    assertThat(tasks.getUserTask("no-such-user-task", null).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);

  }

  @Test
  void anUnknownWorkflowAnswers404() {

    assertThat(workflows.getWorkflow("no-such-workflow").getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);

  }

}

package io.vanillabp.cockpit.workflowlist.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Who a workflow is accessible to changes person by person, on the same object.
 */
@ExtendWith(SuppressOutputExtension.class)
class WorkflowAccessTest {

  private static Person person(
      final String id) {

    final var person = new Person();
    person.setId(id);
    return person;

  }

  @Test
  void aSecondPersonCanBeAddedToAWorkflowWhichHadNobody() {

    final var workflow = new Workflow();

    workflow.addPersonForAccess(person("petra"));
    workflow.addPersonForAccess(person("martin"));
    workflow.removeUserForAccess("petra");

    assertThat(workflow.getAccessibleToUsers())
        .extracting(Person::getId)
        .containsExactly("martin");

  }

  @Test
  void addingThePersonAgainReplacesItsEntry() {

    final var workflow = new Workflow();

    workflow.addPersonForAccess(person("petra"));
    workflow.addPersonForAccess(person("petra"));

    assertThat(workflow.getAccessibleToUsers())
        .extracting(Person::getId)
        .containsExactly("petra");

  }

}

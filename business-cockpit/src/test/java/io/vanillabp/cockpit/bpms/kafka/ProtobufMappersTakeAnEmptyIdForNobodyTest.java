package io.vanillabp.cockpit.bpms.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Protobuf has no null for a string. A Kafka event which names no user carries an empty id
 * instead. The REST mappers take a missing id for nobody, and the Kafka mappers have to do the
 * same with an empty one. A person with an empty id would otherwise be stored, as an initiator for
 * example.
 */
@ExtendWith(SuppressOutputExtension.class)
class ProtobufMappersTakeAnEmptyIdForNobodyTest {

  private ProtobufUserTaskMapper userTaskMapper;

  private ProtobufWorkflowMapper workflowMapper;

  @BeforeEach
  void setUp() throws Exception {

    final var personAndGroupMapper = mock(PersonAndGroupMapper.class);
    when(personAndGroupMapper.toModelPerson(anyString())).thenAnswer(invocation -> {
      final var person = new Person();
      person.setId(invocation.getArgument(0));
      return person;
    });

    userTaskMapper = new ProtobufUserTaskMapperImpl();
    final var userTaskField = ProtobufUserTaskMapper.class.getDeclaredField("personAndGroupMapper");
    userTaskField.setAccessible(true);
    userTaskField.set(userTaskMapper, personAndGroupMapper);

    workflowMapper = new ProtobufWorkflowMapperImpl();
    final var workflowField = ProtobufWorkflowMapper.class.getDeclaredField("personAndGroupMapper");
    workflowField.setAccessible(true);
    workflowField.set(workflowMapper, personAndGroupMapper);

  }

  @Test
  void anEmptyIdIsNobody() {

    assertNull(userTaskMapper.toPerson(""));
    assertNull(workflowMapper.toPerson(""));

  }

  @Test
  void aMissingIdIsNobody() {

    assertNull(userTaskMapper.toPerson(null));
    assertNull(workflowMapper.toPerson(null));

  }

  @Test
  void anIdIsAPerson() {

    assertEquals("petra", userTaskMapper.toPerson("petra").getId());
    assertEquals("petra", workflowMapper.toPerson("petra").getId());

  }

}

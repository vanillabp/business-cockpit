package io.vanillabp.cockpit.workflowmodules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DuplicateKeyException;

import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;
import io.vanillabp.cockpit.workflowmodules.model.WorkflowModule;
import io.vanillabp.cockpit.workflowmodules.model.WorkflowModuleRepository;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Two nodes of one application start at the same time and register the same workflow module, which
 * is not stored yet. Both read nothing, so both insert, and MongoDB refuses the second insert with a
 * duplicate key. That second registration must not fail: it reads what the first node wrote and
 * applies itself on top, like it does after an optimistic locking failure.
 */
@ExtendWith(SuppressOutputExtension.class)
class AFirstRegistrationOfAModuleTwiceAtOnceTest {

  private static final String MODULE_ID = "TestModule";

  private WorkflowModuleService service;
  private WorkflowModuleRepository workflowModules;

  @BeforeEach
  void setUp() throws Exception {
    service = new WorkflowModuleService();
    workflowModules = mock(WorkflowModuleRepository.class);
    inject("workflowModules", workflowModules);
    inject("microserviceProxyRegistry", mock(MicroserviceProxyRegistry.class));
  }

  private void inject(
      final String field,
      final Object value) throws Exception {
    final var declaredField = WorkflowModuleService.class.getDeclaredField(field);
    declaredField.setAccessible(true);
    declaredField.set(service, value);
  }

  private static WorkflowModule writtenByTheOtherNode(
      final String uri) {
    final var module = WorkflowModule.withId(MODULE_ID);
    module.setUri(uri);
    module.setTaskProviderApiUriPath("/tasks");
    module.setWorkflowProviderApiUriPath("/workflows");
    module.setAccessibleToGroups(List.of("staff"));
    return module;
  }

  @Test
  void theSecondInsertReadsAgainAndAppliesItsRegistration() {

    final var otherNode = writtenByTheOtherNode("http://other-node:8080");
    when(workflowModules.findById(MODULE_ID))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(otherNode));
    when(workflowModules.save(any(WorkflowModule.class)))
        .thenThrow(new DuplicateKeyException("E11000 duplicate key error"))
        .thenAnswer(invocation -> invocation.getArgument(0));

    final var written = service.registerOrUpdateWorkflowModule(
        MODULE_ID, "http://this-node:8080", "/tasks", "/workflows", List.of("staff"), null);

    assertTrue(written);
    verify(workflowModules, times(2)).save(any(WorkflowModule.class));
    assertEquals("http://this-node:8080", otherNode.getUri(),
        "the registration is applied to the document the other node wrote");

  }

  @Test
  void theSecondInsertOfTheSameRegistrationWritesNothing() {

    when(workflowModules.findById(MODULE_ID))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(writtenByTheOtherNode("http://app:8080")));
    when(workflowModules.save(any(WorkflowModule.class)))
        .thenThrow(new DuplicateKeyException("E11000 duplicate key error"));

    final var written = service.registerOrUpdateWorkflowModule(
        MODULE_ID, "http://app:8080", "/tasks", "/workflows", List.of("staff"), null);

    assertFalse(written, "the other node already stored the same registration");
    verify(workflowModules, times(1)).save(any(WorkflowModule.class));

  }

}

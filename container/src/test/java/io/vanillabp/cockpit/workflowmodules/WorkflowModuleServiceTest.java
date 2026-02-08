package io.vanillabp.cockpit.workflowmodules;

import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;
import io.vanillabp.cockpit.workflowmodules.model.WorkflowModule;
import io.vanillabp.cockpit.workflowmodules.model.WorkflowModuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WorkflowModuleService}.
 */
@ExtendWith(MockitoExtension.class)
class WorkflowModuleServiceTest {

    @Mock
    private WorkflowModuleRepository workflowModules;

    @Mock
    private MicroserviceProxyRegistry microserviceProxyRegistry;

    @InjectMocks
    private WorkflowModuleService service;

    // --- getWorkflowModule tests ---

    @Test
    void getWorkflowModule_withNullId_returnsEmpty() {
        // Act & Assert
        StepVerifier.create(service.getWorkflowModule(null))
                .verifyComplete();

        verifyNoInteractions(workflowModules);
    }

    @Test
    void getWorkflowModule_withExistingId_returnsModule() {
        // Arrange
        WorkflowModule module = createModule("module-1", "http://localhost:8080");
        when(workflowModules.findById("module-1")).thenReturn(Mono.just(module));

        // Act & Assert
        StepVerifier.create(service.getWorkflowModule("module-1"))
                .expectNext(module)
                .verifyComplete();
    }

    @Test
    void getWorkflowModule_withNonExistingId_returnsEmpty() {
        // Arrange
        when(workflowModules.findById("non-existent")).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(service.getWorkflowModule("non-existent"))
                .verifyComplete();
    }

    // --- getWorkflowModules tests ---

    @Test
    void getWorkflowModules_returnsModulesForUserRoles() {
        // Arrange
        List<String> userRoles = List.of("admin", "user");
        WorkflowModule module1 = createModule("module-1", "http://localhost:8081");
        WorkflowModule module2 = createModule("module-2", "http://localhost:8082");
        when(workflowModules.findByAccessibleToGroups(userRoles))
                .thenReturn(Flux.just(module1, module2));

        // Act & Assert
        StepVerifier.create(service.getWorkflowModules(userRoles))
                .expectNext(module1)
                .expectNext(module2)
                .verifyComplete();
    }

    @Test
    void getWorkflowModules_withNoMatchingRoles_returnsEmpty() {
        // Arrange
        List<String> userRoles = List.of("unknown-role");
        when(workflowModules.findByAccessibleToGroups(userRoles))
                .thenReturn(Flux.empty());

        // Act & Assert
        StepVerifier.create(service.getWorkflowModules(userRoles))
                .verifyComplete();
    }

    // --- registerOrUpdateWorkflowModule tests ---

    @Test
    void registerOrUpdateWorkflowModule_withNewModule_createsAndReturnsTrue() {
        // Arrange
        String moduleId = "new-module";
        String uri = "http://localhost:8080";
        WorkflowModule newModule = createModule(moduleId, uri);

        when(workflowModules.findById(moduleId)).thenReturn(Mono.empty());
        when(workflowModules.save(any(WorkflowModule.class))).thenReturn(Mono.just(newModule));

        // Act & Assert
        StepVerifier.create(service.registerOrUpdateWorkflowModule(
                        moduleId, uri, "/api/tasks", "/api/workflows", List.of("admin")))
                .expectNext(Boolean.TRUE)
                .verifyComplete();

        verify(workflowModules).save(any(WorkflowModule.class));
        verify(microserviceProxyRegistry).registerMicroservice(moduleId, uri);
    }

    @Test
    void registerOrUpdateWorkflowModule_withExistingModuleAndChangedUri_updatesAndReturnsTrue() {
        // Arrange
        String moduleId = "existing-module";
        String oldUri = "http://localhost:8080";
        String newUri = "http://localhost:9090";

        WorkflowModule existingModule = createModule(moduleId, oldUri);
        WorkflowModule updatedModule = createModule(moduleId, newUri);

        when(workflowModules.findById(moduleId)).thenReturn(Mono.just(existingModule));
        when(workflowModules.save(any(WorkflowModule.class))).thenReturn(Mono.just(updatedModule));

        // Act & Assert
        StepVerifier.create(service.registerOrUpdateWorkflowModule(
                        moduleId, newUri, "/api/tasks", "/api/workflows", List.of("admin")))
                .expectNext(Boolean.TRUE)
                .verifyComplete();

        verify(workflowModules).save(any(WorkflowModule.class));
    }

    @Test
    void registerOrUpdateWorkflowModule_withNoChanges_returnsFalse() {
        // Arrange
        String moduleId = "existing-module";
        String uri = "http://localhost:8080";
        String taskPath = "/api/tasks";
        String workflowPath = "/api/workflows";
        List<String> groups = List.of("admin");

        WorkflowModule existingModule = createModule(moduleId, uri);
        existingModule.setTaskProviderApiUriPath(taskPath);
        existingModule.setWorkflowProviderApiUriPath(workflowPath);
        existingModule.setAccessibleToGroups(groups);

        when(workflowModules.findById(moduleId)).thenReturn(Mono.just(existingModule));

        // Act & Assert - no changes, should return FALSE
        StepVerifier.create(service.registerOrUpdateWorkflowModule(
                        moduleId, uri, taskPath, workflowPath, groups))
                .expectNext(Boolean.FALSE)
                .verifyComplete();

        verify(workflowModules, never()).save(any(WorkflowModule.class));
    }

    @Test
    void registerOrUpdateWorkflowModule_withChangedTaskProviderPath_updatesAndReturnsTrue() {
        // Arrange
        String moduleId = "existing-module";
        String uri = "http://localhost:8080";

        WorkflowModule existingModule = createModule(moduleId, uri);
        existingModule.setTaskProviderApiUriPath("/old/path");

        WorkflowModule updatedModule = createModule(moduleId, uri);
        updatedModule.setTaskProviderApiUriPath("/new/path");

        when(workflowModules.findById(moduleId)).thenReturn(Mono.just(existingModule));
        when(workflowModules.save(any(WorkflowModule.class))).thenReturn(Mono.just(updatedModule));

        // Act & Assert
        StepVerifier.create(service.registerOrUpdateWorkflowModule(
                        moduleId, uri, "/new/path", null, null))
                .expectNext(Boolean.TRUE)
                .verifyComplete();

        verify(workflowModules).save(any(WorkflowModule.class));
    }

    @Test
    void registerOrUpdateWorkflowModule_withChangedAccessibleGroups_updatesAndReturnsTrue() {
        // Arrange
        String moduleId = "existing-module";
        String uri = "http://localhost:8080";

        WorkflowModule existingModule = createModule(moduleId, uri);
        existingModule.setAccessibleToGroups(List.of("old-group"));

        WorkflowModule updatedModule = createModule(moduleId, uri);
        updatedModule.setAccessibleToGroups(List.of("new-group"));

        when(workflowModules.findById(moduleId)).thenReturn(Mono.just(existingModule));
        when(workflowModules.save(any(WorkflowModule.class))).thenReturn(Mono.just(updatedModule));

        // Act & Assert
        StepVerifier.create(service.registerOrUpdateWorkflowModule(
                        moduleId, uri, null, null, List.of("new-group")))
                .expectNext(Boolean.TRUE)
                .verifyComplete();

        verify(workflowModules).save(any(WorkflowModule.class));
    }

    // --- registerProxiesForWorkflowModules tests ---

    @Test
    void registerProxiesForWorkflowModules_registersAllModules() {
        // Arrange
        WorkflowModule module1 = createModule("module-1", "http://localhost:8081");
        WorkflowModule module2 = createModule("module-2", "http://localhost:8082");

        when(workflowModules.findAll()).thenReturn(Flux.just(module1, module2));

        // Act
        service.registerProxiesForWorkflowModules();

        // Assert - allow time for async subscription
        verify(workflowModules).findAll();
    }

    // --- Helper methods ---

    private WorkflowModule createModule(String id, String uri) {
        WorkflowModule module = WorkflowModule.withId(id);
        module.setUri(uri);
        return module;
    }
}

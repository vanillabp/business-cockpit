package io.vanillabp.cockpit.workflowmodules.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link WorkflowModule}.
 */
class WorkflowModuleTest {

    @Test
    void withId_createsModuleWithId() {
        // Act
        WorkflowModule module = WorkflowModule.withId("module-123");

        // Assert
        assertThat(module.getId()).isEqualTo("module-123");
        assertThat(module.getUri()).isNull();
        assertThat(module.getVersion()).isEqualTo(0L);
    }

    @Test
    void settersAndGetters_workCorrectly() {
        // Arrange
        WorkflowModule module = new WorkflowModule();
        List<String> groups = List.of("group1", "group2");

        // Act
        module.setId("module-123");
        module.setVersion(5L);
        module.setUri("http://localhost:8080");
        module.setTaskProviderApiUriPath("/api/tasks");
        module.setWorkflowProviderApiUriPath("/api/workflows");
        module.setAccessibleToGroups(groups);

        // Assert
        assertThat(module.getId()).isEqualTo("module-123");
        assertThat(module.getVersion()).isEqualTo(5L);
        assertThat(module.getUri()).isEqualTo("http://localhost:8080");
        assertThat(module.getTaskProviderApiUriPath()).isEqualTo("/api/tasks");
        assertThat(module.getWorkflowProviderApiUriPath()).isEqualTo("/api/workflows");
        assertThat(module.getAccessibleToGroups()).isEqualTo(groups);
    }

    @Test
    void newModule_hasDefaultValues() {
        // Arrange
        WorkflowModule module = new WorkflowModule();

        // Assert
        assertThat(module.getId()).isNull();
        assertThat(module.getVersion()).isEqualTo(0L);
        assertThat(module.getUri()).isNull();
        assertThat(module.getTaskProviderApiUriPath()).isNull();
        assertThat(module.getWorkflowProviderApiUriPath()).isNull();
        assertThat(module.getAccessibleToGroups()).isNull();
    }

    @Test
    void collectionName_isCorrect() {
        // Assert
        assertThat(WorkflowModule.COLLECTION_NAME).isEqualTo("modules");
    }
}

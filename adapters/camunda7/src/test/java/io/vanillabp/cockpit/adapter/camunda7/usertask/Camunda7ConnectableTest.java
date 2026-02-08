package io.vanillabp.cockpit.adapter.camunda7.usertask;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Camunda7Connectable}.
 */
class Camunda7ConnectableTest {

    @Test
    void constructor_setsAllFields() {
        // Arrange & Act
        Camunda7Connectable connectable = new Camunda7Connectable(
                "test-process",
                "1.0",
                "UserTask_1",
                "reviewTask"
        );

        // Assert
        assertThat(connectable.getBpmnProcessId()).isEqualTo("test-process");
        assertThat(connectable.getVersionInfo()).isEqualTo("1.0");
        assertThat(connectable.getElementId()).isEqualTo("UserTask_1");
        assertThat(connectable.getTaskDefinition()).isEqualTo("reviewTask");
    }

    @Test
    void isExecutableProcess_returnsTrue() {
        // Arrange
        Camunda7Connectable connectable = new Camunda7Connectable(
                "test-process", "1.0", "UserTask_1", "reviewTask"
        );

        // Act & Assert
        assertThat(connectable.isExecutableProcess()).isTrue();
    }

    @Test
    void applies_matchesByElementId() {
        // Arrange
        Camunda7Connectable connectable = new Camunda7Connectable(
                "test-process", "1.0", "UserTask_1", "reviewTask"
        );

        // Act & Assert
        assertThat(connectable.applies("UserTask_1", "otherTask")).isTrue();
    }

    @Test
    void applies_matchesByTaskDefinition() {
        // Arrange
        Camunda7Connectable connectable = new Camunda7Connectable(
                "test-process", "1.0", "UserTask_1", "reviewTask"
        );

        // Act & Assert
        assertThat(connectable.applies("OtherTask", "reviewTask")).isTrue();
    }

    @Test
    void applies_returnsFalseWhenNeitherMatches() {
        // Arrange
        Camunda7Connectable connectable = new Camunda7Connectable(
                "test-process", "1.0", "UserTask_1", "reviewTask"
        );

        // Act & Assert
        assertThat(connectable.applies("OtherTask", "otherDefinition")).isFalse();
    }

    @Test
    void getters_returnCorrectValues() {
        // Arrange
        Camunda7Connectable connectable = new Camunda7Connectable(
                "process-id", "2.0", "element-id", "task-def"
        );

        // Act & Assert
        assertThat(connectable.getBpmnProcessId()).isEqualTo("process-id");
        assertThat(connectable.getVersionInfo()).isEqualTo("2.0");
        assertThat(connectable.getElementId()).isEqualTo("element-id");
        assertThat(connectable.getTaskDefinition()).isEqualTo("task-def");
    }

    @Test
    void applies_matchesBothElementIdAndTaskDefinition() {
        // Arrange
        Camunda7Connectable connectable = new Camunda7Connectable(
                "test-process", "1.0", "UserTask_1", "reviewTask"
        );

        // Act & Assert - both match
        assertThat(connectable.applies("UserTask_1", "reviewTask")).isTrue();
    }

    @Test
    void constructor_handlesNullValues() {
        // Arrange & Act
        Camunda7Connectable connectable = new Camunda7Connectable(
                null, null, null, null
        );

        // Assert
        assertThat(connectable.getBpmnProcessId()).isNull();
        assertThat(connectable.getVersionInfo()).isNull();
        assertThat(connectable.getElementId()).isNull();
        assertThat(connectable.getTaskDefinition()).isNull();
    }
}

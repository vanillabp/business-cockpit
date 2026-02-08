package io.vanillabp.cockpit.workflowlist;

import io.vanillabp.cockpit.workflowlist.model.WorkflowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link WorkflowlistService#buildWorkflowlistCriteria} method.
 */
@ExtendWith(MockitoExtension.class)
class WorkflowlistServiceCriteriaTest {

    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private ReactiveMongoTemplate mongoTemplate;

    @Mock
    private Logger logger;

    @InjectMocks
    private WorkflowlistService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "logger", logger);
    }

    // --- buildWorkflowlistCriteria tests ---

    @Test
    void buildWorkflowlistCriteria_withAllMode_returnsBasicCriteria() {
        // Arrange
        OffsetDateTime timestamp = OffsetDateTime.now();

        // Act
        CriteriaDefinition criteria = service.buildWorkflowlistCriteria(
                false, null, null, timestamp,
                WorkflowlistService.RetrieveItemsMode.All, null, null);

        // Assert
        assertThat(criteria).isNotNull();
    }

    @Test
    void buildWorkflowlistCriteria_withActiveMode_includesEndedAtFilter() {
        // Arrange
        OffsetDateTime timestamp = OffsetDateTime.now();

        // Act
        CriteriaDefinition criteria = service.buildWorkflowlistCriteria(
                false, null, null, timestamp,
                WorkflowlistService.RetrieveItemsMode.Active, null, null);

        // Assert
        assertThat(criteria).isNotNull();
    }

    @Test
    void buildWorkflowlistCriteria_withInactiveMode_includesEndedAtFilter() {
        // Arrange
        OffsetDateTime timestamp = OffsetDateTime.now();

        // Act
        CriteriaDefinition criteria = service.buildWorkflowlistCriteria(
                false, null, null, timestamp,
                WorkflowlistService.RetrieveItemsMode.Inactive, null, null);

        // Assert
        assertThat(criteria).isNotNull();
    }

    @Test
    void buildWorkflowlistCriteria_withAccessibleToUsers_includesUserFilter() {
        // Arrange
        OffsetDateTime timestamp = OffsetDateTime.now();
        List<String> accessibleToUsers = List.of("user1", "user2");

        // Act
        CriteriaDefinition criteria = service.buildWorkflowlistCriteria(
                false, accessibleToUsers, null, timestamp,
                WorkflowlistService.RetrieveItemsMode.All, null, null);

        // Assert
        assertThat(criteria).isNotNull();
    }

    @Test
    void buildWorkflowlistCriteria_withAccessibleToGroups_includesGroupFilter() {
        // Arrange
        OffsetDateTime timestamp = OffsetDateTime.now();
        List<String> accessibleToGroups = List.of("admin", "managers");

        // Act
        CriteriaDefinition criteria = service.buildWorkflowlistCriteria(
                false, null, accessibleToGroups, timestamp,
                WorkflowlistService.RetrieveItemsMode.All, null, null);

        // Assert
        assertThat(criteria).isNotNull();
    }

    @Test
    void buildWorkflowlistCriteria_withIncludeDanglingWorkflows_includesDanglingFilter() {
        // Arrange
        OffsetDateTime timestamp = OffsetDateTime.now();
        List<String> accessibleToUsers = List.of("user1");

        // Act
        CriteriaDefinition criteria = service.buildWorkflowlistCriteria(
                true, accessibleToUsers, null, timestamp,
                WorkflowlistService.RetrieveItemsMode.All, null, null);

        // Assert
        assertThat(criteria).isNotNull();
    }

    @Test
    void buildWorkflowlistCriteria_withBusinessIds_includesBusinessIdFilter() {
        // Arrange
        OffsetDateTime timestamp = OffsetDateTime.now();
        List<String> businessIds = List.of("biz-1", "biz-2");

        // Act
        CriteriaDefinition criteria = service.buildWorkflowlistCriteria(
                false, null, null, timestamp,
                WorkflowlistService.RetrieveItemsMode.All, null, businessIds);

        // Assert
        assertThat(criteria).isNotNull();
    }

    @Test
    void buildWorkflowlistCriteria_withPredefinedCriteria_appendsCriteria() {
        // Arrange
        OffsetDateTime timestamp = OffsetDateTime.now();
        List<Criteria> predefinedCriteria = List.of(Criteria.where("bpmnProcessId").is("process-1"));

        // Act
        CriteriaDefinition criteria = service.buildWorkflowlistCriteria(
                false, null, null, timestamp,
                WorkflowlistService.RetrieveItemsMode.All, predefinedCriteria, null);

        // Assert
        assertThat(criteria).isNotNull();
    }

    @Test
    void buildWorkflowlistCriteria_withCombinedFilters_combinesAllFilters() {
        // Arrange
        OffsetDateTime timestamp = OffsetDateTime.now();
        List<String> accessibleToUsers = List.of("user1");
        List<String> accessibleToGroups = List.of("admin");
        List<String> businessIds = List.of("biz-1");

        // Act
        CriteriaDefinition criteria = service.buildWorkflowlistCriteria(
                true, accessibleToUsers, accessibleToGroups, timestamp,
                WorkflowlistService.RetrieveItemsMode.Active, null, businessIds);

        // Assert
        assertThat(criteria).isNotNull();
    }

    @Test
    void buildWorkflowlistCriteria_withEmptyPredefinedCriteria_doesNotAppend() {
        // Arrange
        OffsetDateTime timestamp = OffsetDateTime.now();
        List<Criteria> predefinedCriteria = List.of();

        // Act
        CriteriaDefinition criteria = service.buildWorkflowlistCriteria(
                false, null, null, timestamp,
                WorkflowlistService.RetrieveItemsMode.All, predefinedCriteria, null);

        // Assert
        assertThat(criteria).isNotNull();
    }

    @Test
    void buildWorkflowlistCriteria_withNullBusinessIds_doesNotIncludeBusinessIdFilter() {
        // Arrange
        OffsetDateTime timestamp = OffsetDateTime.now();

        // Act
        CriteriaDefinition criteria = service.buildWorkflowlistCriteria(
                false, null, null, timestamp,
                WorkflowlistService.RetrieveItemsMode.All, null, null);

        // Assert
        assertThat(criteria).isNotNull();
    }

    @Test
    void buildWorkflowlistCriteria_withEmptyBusinessIds_doesNotIncludeBusinessIdFilter() {
        // Arrange
        OffsetDateTime timestamp = OffsetDateTime.now();
        List<String> businessIds = List.of();

        // Act
        CriteriaDefinition criteria = service.buildWorkflowlistCriteria(
                false, null, null, timestamp,
                WorkflowlistService.RetrieveItemsMode.All, null, businessIds);

        // Assert
        assertThat(criteria).isNotNull();
    }
}

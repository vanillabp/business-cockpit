package io.vanillabp.cockpit.adapter.common.workflowmodule.rest;

import io.vanillabp.cockpit.adapter.common.workflowmodule.events.RegisterWorkflowModuleEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowModuleRestMapperTest {

    private WorkflowModuleRestMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new WorkflowModuleRestMapper();
    }

    @Test
    void map_withNull_returnsNull() {
        assertThat(mapper.map(null)).isNull();
    }

    @Test
    void map_mapsAllFields() {
        RegisterWorkflowModuleEvent event = new RegisterWorkflowModuleEvent();
        List<String> groups = Arrays.asList("group1", "group2");

        event.setUri("http://localhost:8080");
        event.setTaskProviderApiUriPath("/api/tasks");
        event.setWorkflowProviderApiUriPath("/api/workflows");
        event.setAccessibleToGroups(groups);

        io.vanillabp.cockpit.bpms.api.v1_1.RegisterWorkflowModuleEvent result = mapper.map(event);

        assertThat(result.getUri()).isEqualTo("http://localhost:8080");
        assertThat(result.getTaskProviderApiUriPath()).isEqualTo("/api/tasks");
        assertThat(result.getWorkflowProviderApiUriPath()).isEqualTo("/api/workflows");
        assertThat(result.getAccessibleToGroups()).isEqualTo(groups);
    }

    @Test
    void map_withNullFields_mapsToNull() {
        RegisterWorkflowModuleEvent event = new RegisterWorkflowModuleEvent();

        io.vanillabp.cockpit.bpms.api.v1_1.RegisterWorkflowModuleEvent result = mapper.map(event);

        assertThat(result.getUri()).isNull();
        assertThat(result.getTaskProviderApiUriPath()).isNull();
        assertThat(result.getWorkflowProviderApiUriPath()).isNull();
        assertThat(result.getAccessibleToGroups()).isNull();
    }

    @Test
    void map_withPartialFields_mapsOnlySetFields() {
        RegisterWorkflowModuleEvent event = new RegisterWorkflowModuleEvent();
        event.setUri("http://example.com");

        io.vanillabp.cockpit.bpms.api.v1_1.RegisterWorkflowModuleEvent result = mapper.map(event);

        assertThat(result.getUri()).isEqualTo("http://example.com");
        assertThat(result.getTaskProviderApiUriPath()).isNull();
        assertThat(result.getWorkflowProviderApiUriPath()).isNull();
        assertThat(result.getAccessibleToGroups()).isNull();
    }

    @Test
    void map_withEmptyGroups_mapsToEmptyList() {
        RegisterWorkflowModuleEvent event = new RegisterWorkflowModuleEvent();
        event.setAccessibleToGroups(Arrays.asList());

        io.vanillabp.cockpit.bpms.api.v1_1.RegisterWorkflowModuleEvent result = mapper.map(event);

        assertThat(result.getAccessibleToGroups()).isEmpty();
    }
}

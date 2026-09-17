package io.vanillabp.cockpit.extension.springboot.versions;

import java.util.List;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import io.vanillabp.bpmsdouble.DummyProcessVersionSource;
import io.vanillabp.bpmsdouble.DummyTaskAwarenessSource;
import io.vanillabp.integration.adapter.spi.WorkflowAwareness;
import io.vanillabp.integration.adapter.spi.version.DeployedProcessVersion;
import io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleDetailsProvider;

/**
 * An application of its own for the workflow service whose providers serve one generation of
 * the model each. It is kept apart from the other test applications because it is the only one
 * whose BPMS reports more than one deployed version.
 */
@SpringBootApplication
public class VersionsApplication {

  /** The version tag the third deployment of the BPMN process carries. */
  public static final String TAG_OF_THE_THIRD = "release-2026";

  /**
   * What the BPMS holds of the versioned process, oldest first. It is what a real adapter
   * queries from its engine, and it is what a version tag is resolved against.
   *
   * @return The deployed versions of every process this application asks about
   */
  @Bean
  public DummyProcessVersionSource processVersions() {

    return (
        adapterId,
        workflowModuleId,
        bpmnProcessId) -> List
            .of(
                DeployedProcessVersion.of("1", null),
                DeployedProcessVersion.of("2", null),
                DeployedProcessVersion.of("3", TAG_OF_THE_THIRD));

  }

  @Bean
  public DummyTaskAwarenessSource workflowAwareness() {

    return (
        adapterId,
        workflowAggregateId,
        taskId) -> WorkflowAwareness.ACTIVE;

  }

  @Bean
  public VersionsBpmsBridge versionsBpmsBridge() {

    return new VersionsBpmsBridge();

  }

  @Bean
  public WorkflowModuleDetailsProvider workflowModuleDetailsProvider() {

    return new WorkflowModuleDetailsProvider() {

      @Override
      public List<String> getAccessibleToGroups() {

        return List.of("clerks");

      }

      @Override
      public String getWorkflowModuleId() {

        return "test-module";

      }

    };

  }

}

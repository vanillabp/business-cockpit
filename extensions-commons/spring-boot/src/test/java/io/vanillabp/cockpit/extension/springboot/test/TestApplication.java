package io.vanillabp.cockpit.extension.springboot.test;

import java.util.List;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import io.vanillabp.bpmsdouble.DummyTaskAwarenessSource;
import io.vanillabp.integration.adapter.spi.WorkflowAwareness;
import io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleDetailsProvider;

/**
 * The application the Business Cockpit extension is tested inside: a JPA workflow aggregate, a
 * workflow service with details providers, the BPMS double of the platform, and the test's own
 * BPMS half of the extension.
 */
@SpringBootApplication
public class TestApplication {

  /** The groups the test application reports as allowed to see its cases. */
  public static final List<String> ACCESSIBLE_TO_GROUPS = List.of("clerks", "approvers");

  /**
   * The BPMS double holds every workflow the test starts, which is what the election reads
   * when the application asks which BPMS a workflow aggregate's workflow lives in.
   *
   * @return The awareness of the double
   */
  @Bean
  public DummyTaskAwarenessSource workflowAwareness() {

    return (
        adapterId,
        workflowAggregateId,
        taskId) -> WorkflowAwareness.ACTIVE;

  }

  @Bean
  public RecordingBpmsBridge recordingBpmsBridge() {

    return new RecordingBpmsBridge();

  }

  @Bean
  public WorkflowModuleDetailsProvider workflowModuleDetailsProvider() {

    return new WorkflowModuleDetailsProvider() {

      @Override
      public List<String> getAccessibleToGroups() {

        return ACCESSIBLE_TO_GROUPS;

      }

      @Override
      public String getWorkflowModuleId() {

        return "test-module";

      }

    };

  }

}

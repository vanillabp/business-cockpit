package io.vanillabp.cockpit.extension.springboot.broken;

import java.util.List;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import io.vanillabp.bpmsdouble.DummyTaskAwarenessSource;
import io.vanillabp.integration.adapter.spi.WorkflowAwareness;
import io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleDetailsProvider;

/**
 * An application which is complete except for the one thing a test wants refused. Each of these
 * tests adds the workflow service with the defect, so the boot fails for that reason and for no
 * other.
 */
@SpringBootApplication
public class BrokenApplication {

  @Bean
  public DummyTaskAwarenessSource workflowAwareness() {

    return (
        adapterId,
        workflowAggregateId,
        taskId) -> WorkflowAwareness.ACTIVE;

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

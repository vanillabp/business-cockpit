package io.vanillabp.cockpit.extension.springboot.everytask;

import java.util.List;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import io.vanillabp.bpmsdouble.DummyTaskAwarenessSource;
import io.vanillabp.cockpit.extension.springboot.test.RecordingBpmsBridge;
import io.vanillabp.integration.adapter.spi.WorkflowAwareness;
import io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleDetailsProvider;

/**
 * An application of its own for the workflow service whose one provider serves every user
 * task. It is kept apart from the other test application because a method claiming every task
 * is exactly what those tests must not have.
 */
@SpringBootApplication
public class EveryTaskApplication {

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

        return List.of("clerks");

      }

      @Override
      public String getWorkflowModuleId() {

        return "test-module";

      }

    };

  }

}

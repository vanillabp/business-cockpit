package io.vanillabp.cockpit.extension.springboot.deployedversion;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import io.vanillabp.bpmsdouble.DummyProcessVersionSource;
import io.vanillabp.bpmsdouble.DummyTaskAwarenessSource;
import io.vanillabp.cockpit.extension.spi.BusinessCockpitEventPublisher;
import io.vanillabp.integration.adapter.spi.WorkflowAwareness;
import io.vanillabp.integration.adapter.spi.version.DeployedProcessVersion;
import io.vanillabp.integration.extension.spi.ExtensionWiringService;
import io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleDetailsProvider;

/**
 * An application whose BPMS says which version it gave the model of this start. Each test adds
 * the workflow service it is about, so the start ends for that reason or runs, and for no other.
 * <p>
 * The BPMS holds versions 1 and 2. This start deployed version 2 of {@link #BPMN_PROCESS} and
 * version 1 of {@link #CALLED_PROCESS}.
 */
@SpringBootApplication
public class DeployedVersionApplication {

  /** The BPMN process the BPMS double deploys. */
  public static final String BPMN_PROCESS = "TestProcess";

  /** A second process of the same file, deployed only where a test declares it. */
  public static final String CALLED_PROCESS = "CalledProcess";

  /** The task definition of the one user task of {@link #BPMN_PROCESS}. */
  public static final String APPROVE = "approve";

  /** The element id of that user task. */
  public static final String APPROVE_ELEMENT = "Activity_approve";

  /** The task definition of the one user task of {@link #CALLED_PROCESS}. */
  public static final String CHECK = "check";

  /** The element id of that user task. */
  public static final String CHECK_ELEMENT = "Activity_check";

  /** The task definition of a task the model does not have. */
  public static final String AUDIT = "audit";

  /**
   * @return What the BPMS holds, and which version it gave the model of this start
   */
  @Bean
  public DummyProcessVersionSource processVersions() {

    return new DummyProcessVersionSource() {

      @Override
      public List<DeployedProcessVersion> versionsOf(
          final String adapterId,
          final String workflowModuleId,
          final String bpmnProcessId) {

        return List.of(DeployedProcessVersion.of("1", null), DeployedProcessVersion.of("2", null));

      }

      @Override
      public String deployedVersionOf(
          final String adapterId,
          final String workflowModuleId,
          final String bpmnProcessId) {

        return CALLED_PROCESS.equals(bpmnProcessId)
            ? "1"
            : "2";

      }

    };

  }

  /**
   * What a BPMS half does while it wires a model: it names the user tasks of the model to the
   * extension. The BPMS double reads no model, so this stands in for it.
   *
   * @param publisher The extension, looked up when the first model is wired
   * @return The wiring service of the stand-in half
   */
  @Bean
  public ExtensionWiringService<Object, Object> userTasksOfTheModel(
      final ObjectProvider<BusinessCockpitEventPublisher> publisher) {

    return new ExtensionWiringService<>() {

      @Override
      public Class<Object> getModelType() {

        return Object.class;

      }

      @Override
      public Class<Object> getProcessContextType() {

        return Object.class;

      }

      @Override
      public void wireBpmn(
          final String workflowModuleId,
          final String filename,
          final String bpmnProcessId,
          final Object model,
          final Object context) {

        if (CALLED_PROCESS.equals(bpmnProcessId)) {
          publisher.getObject().userTaskWired(workflowModuleId, bpmnProcessId, CHECK, CHECK_ELEMENT);
        } else {
          publisher.getObject().userTaskWired(workflowModuleId, bpmnProcessId, APPROVE, APPROVE_ELEMENT);
        }

      }

      @Override
      public void startWorkflowProcessing(
          final String workflowModuleId,
          final Object bpmsProcessingContext) {

        // nothing to start

      }

      @Override
      public void stopWorkflowProcessing(
          final String workflowModuleId,
          final Object bpmsProcessingContext) {

        // nothing to stop

      }

    };

  }

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

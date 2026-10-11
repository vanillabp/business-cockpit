package io.vanillabp.cockpit.extension.quarkus.it;

import io.quarkus.arc.Unremovable;
import io.vanillabp.cockpit.extension.spi.BusinessCockpitEventPublisher;
import io.vanillabp.integration.extension.spi.ExtensionWiringService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Does what a BPMS half does while it wires a model: it tells the extension the user tasks of the
 * model. The BPMS double reads no model, so this class does it instead. The process at the top
 * has the user task 'approve', the called process has the user task 'check'.
 */
@ApplicationScoped
@Unremovable
public class DeployedVersionUserTasks implements ExtensionWiringService<Object, Object> {

  /** The task definition of the user task of the process at the top. */
  public static final String APPROVE = "approve";

  /** Its element id. */
  public static final String APPROVE_ELEMENT = "Activity_approve";

  /** The task definition of the user task of the called process. */
  public static final String CHECK = "check";

  /** Its element id. */
  public static final String CHECK_ELEMENT = "Activity_check";

  /** The task definition of a task the model does not have. */
  public static final String AUDIT = "audit";

  @Inject
  Instance<BusinessCockpitEventPublisher> publisher;

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

    if (DeployedVersionProcessVersions.CALLED_PROCESS.equals(bpmnProcessId)) {
      publisher.get().userTaskWired(workflowModuleId, bpmnProcessId, CHECK, CHECK_ELEMENT);
    } else {
      publisher.get().userTaskWired(workflowModuleId, bpmnProcessId, APPROVE, APPROVE_ELEMENT);
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

}

package io.vanillabp.cockpit.extension.wiring;

import io.vanillabp.cockpit.extension.BusinessCockpitExtension;
import io.vanillabp.integration.extension.spi.ExtensionWiringService;

/**
 * The extension's place in VanillaBP's deployment pipeline, for everything which is the same
 * whichever BPMS a workflow module runs on.
 * <p>
 * It declares <code>Object</code> as both its model and its processing-context type, so it
 * takes part in the deployment of every workflow module of every BPMS. It touches neither of the
 * two. Reading and enriching a BPMN model is what a BPMS half does, in the repository of its
 * BPMS. What is left here is the one thing every workflow module needs whatever its engine is:
 * telling the cockpit server that the module exists.
 */
public class BusinessCockpitWiringService implements ExtensionWiringService<Object, Object> {

  /**
   * Where the Business Cockpit asks to sit in the pipeline. The number is high, so the cockpit
   * runs behind every extension around today which picked a lower one.
   * <p>
   * That is all the number does. It places a wish. An extension which picks a higher number
   * runs after the cockpit, and nothing in the platform stops it. Raising this number would
   * only move the wish, because the next extension can raise its own.
   * <p>
   * What the cockpit does count on is that the BPMS adapter has wired a model before any
   * extension sees it. The deployment pipeline calls the adapter first for that reason, so it
   * holds whatever number stands here.
   */
  public static final int ORDER = 1000;

  private final BusinessCockpitExtension extension;

  /**
   * @param extension The extension the registration is scheduled through
   */
  public BusinessCockpitWiringService(
      final BusinessCockpitExtension extension) {

    this.extension = extension;

  }

  @Override
  public Class<Object> getModelType() {

    return Object.class;

  }

  @Override
  public Class<Object> getProcessContextType() {

    return Object.class;

  }

  @Override
  public int getOrder() {

    return ORDER;

  }

  /**
   * Notes which workflow module the BPMN process belongs to.
   * <p>
   * Nothing is added to the model: what a model needs is added by the BPMS half, which is the
   * only one knowing what a listener looks like in that engine. What this callback carries, and
   * nothing else does, is which module deployed which process. The registration of a workflow
   * module is written into the outbox store of one of that module's own workflow aggregates.
   */
  @Override
  public void wireBpmn(
      final String workflowModuleId,
      final String filename,
      final String bpmnProcessId,
      final Object model,
      final Object context) {

    extension.workflowWired(workflowModuleId, bpmnProcessId);

  }

  /**
   * Notes that the workflow module is ready to be registered at the cockpit server.
   * <p>
   * The registration is not written here. VanillaBP calls this while the deployment pipeline
   * runs. On Quarkus the outbox store creates its table in a startup observer which comes after
   * that. So the entries are written once the application is up, and the platform module
   * triggers that. Why it is the same on both platforms is decision 6 in the repository's
   * DECISIONS.md.
   */
  @Override
  public void startWorkflowProcessing(
      final String workflowModuleId,
      final Object bpmsProcessingContext) {

    extension.workflowModuleStarted(workflowModuleId);

  }

  @Override
  public void stopWorkflowProcessing(
      final String workflowModuleId,
      final Object bpmsProcessingContext) {

    // the transport is released by the platform module when the application shuts down, once
    // rather than per workflow module

  }

}

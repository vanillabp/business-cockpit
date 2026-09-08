package io.vanillabp.cockpit.extension.wiring;

import io.vanillabp.cockpit.extension.BusinessCockpitExtension;
import io.vanillabp.integration.extension.spi.ExtensionWiringService;

/**
 * The extension's place in VanillaBP's deployment pipeline, for everything which is the same
 * whichever BPMS a workflow module runs on.
 * <p>
 * It declares <code>Object</code> as both its model and its processing-context type, so it
 * takes part in the deployment of every workflow module of every BPMS, and it touches neither:
 * reading and enriching a BPMN model is what a BPMS half does, in the repository of its BPMS.
 * What is left here is the one thing every workflow module needs regardless of its engine -
 * telling the cockpit server that the module exists.
 * <p>
 * The order is the last one, which is the convention the Business Cockpit follows everywhere:
 * whatever an adapter or another extension does to a model has happened by the time this runs.
 */
public class BusinessCockpitWiringService implements ExtensionWiringService<Object, Object> {

  /**
   * Where the Business Cockpit sits in the pipeline. Late enough that VanillaBP's own wiring
   * has decided which elements the application's methods serve.
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

  @Override
  public void wireBpmn(
      final String workflowModuleId,
      final String filename,
      final String bpmnProcessId,
      final Object model,
      final Object context) {

    // nothing: what a model needs is added by the BPMS half, which is the only one knowing
    // what a listener looks like in that engine

  }

  /**
   * Notes that the workflow module is ready to be registered at the cockpit server.
   * <p>
   * The registration is not written here. VanillaBP calls this while the deployment pipeline
   * runs, and on Quarkus the outbox store creates its table in a startup observer which comes
   * after that - so the entries are written once the application is up, which the platform
   * module triggers. Why that is the same on both platforms is decision 6 in the repository's
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

package io.vanillabp.cockpit.extension.quarkus.it;

import java.util.Collection;
import java.util.List;

import io.vanillabp.bpmsdouble.DummyTaskWiringSource;
import io.vanillabp.integration.adapter.spi.workflowtask.BpmnTaskSpec;
import jakarta.enterprise.context.ApplicationScoped;

/** Makes the BPMN file of the BPMS double hold a second process, the one a call activity calls. */
@ApplicationScoped
public class DeployedVersionCalledProcessInTheSameFile implements DummyTaskWiringSource {

  @Override
  public Collection<BpmnTaskSpec> tasksOf(
      final String adapterId,
      final String workflowModuleId,
      final String bpmnProcessId) {

    // the user tasks are not @WorkflowTask methods, so VanillaBP's wiring has nothing to check
    return List.of();

  }

  @Override
  public List<String> executableProcessesOf(
      final String adapterId,
      final String workflowModuleId,
      final String filename) {

    return List.of(DeployedVersionProcessVersions.BPMN_PROCESS, DeployedVersionProcessVersions.CALLED_PROCESS);

  }

}

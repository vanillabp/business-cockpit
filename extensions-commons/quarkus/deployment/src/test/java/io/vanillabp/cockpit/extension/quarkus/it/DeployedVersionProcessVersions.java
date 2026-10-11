package io.vanillabp.cockpit.extension.quarkus.it;

import java.util.List;

import io.vanillabp.bpmsdouble.DummyProcessVersionSource;
import io.vanillabp.integration.adapter.spi.version.DeployedProcessVersion;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * A BPMS which holds versions 1 and 2. It says which version the model of this start got:
 * version 2 of the process at the top and version 1 of the called process.
 */
@ApplicationScoped
public class DeployedVersionProcessVersions implements DummyProcessVersionSource {

  /** The BPMN process the BPMS double deploys. */
  public static final String BPMN_PROCESS = "TestProcess";

  /** A second process of the same file, deployed only where a test declares it. */
  public static final String CALLED_PROCESS = "CalledProcess";

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

}

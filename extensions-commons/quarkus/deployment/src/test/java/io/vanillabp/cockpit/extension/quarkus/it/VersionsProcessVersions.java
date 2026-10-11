package io.vanillabp.cockpit.extension.quarkus.it;

import java.util.List;

import io.vanillabp.bpmsdouble.DummyProcessVersionSource;
import io.vanillabp.integration.adapter.spi.version.DeployedProcessVersion;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * The versions of the versioned process the BPMS holds, oldest first. It stands in for the query
 * a real adapter runs against its engine, and a version tag is resolved against it.
 */
@ApplicationScoped
public class VersionsProcessVersions implements DummyProcessVersionSource {

  /** The version tag the third deployment of the BPMN process carries. */
  public static final String TAG_OF_THE_THIRD = "release-2026";

  @Override
  public List<DeployedProcessVersion> versionsOf(
      final String adapterId,
      final String workflowModuleId,
      final String bpmnProcessId) {

    return List
        .of(
            DeployedProcessVersion.of("1", null),
            DeployedProcessVersion.of("2", null),
            DeployedProcessVersion.of("3", TAG_OF_THE_THIRD));

  }

}

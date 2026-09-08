package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.quarkus.BusinessCockpitProducer;
import io.vanillabp.integration.runtime.deployment.VanillaBpDeploymentRunner;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * When the workflow modules are registered at the cockpit server.
 * <p>
 * The registration is an outbox entry, so it must be written after VanillaBP's outbox store
 * created its table - and the store does that in a startup observer of its own. The runtime
 * half of this extension cannot read that priority, because an extension does not compile
 * against a platform integration (decision 2 in the repository's DECISIONS.md); it carries the
 * number as its own. This module does see both, and a platform moving its observer later would
 * end here rather than in an application whose first registration finds no table.
 */
@ExtendWith(SuppressOutputExtension.class)
public class RegistrationStartupPriorityTest {

  @Test
  @DisplayName("The registration of the workflow modules runs after the outbox created its store")
  public void theRegistrationRunsAfterTheOutboxStartup() {

    assertTrue(
        BusinessCockpitProducer.REGISTRATION_STARTUP_PRIORITY > VanillaBpDeploymentRunner.OUTBOX_DISPATCHER_STARTUP_PRIORITY,
        "the extension registers its workflow modules at priority %d, VanillaBP starts its outbox dispatchers at %d"
            .formatted(
                BusinessCockpitProducer.REGISTRATION_STARTUP_PRIORITY,
                VanillaBpDeploymentRunner.OUTBOX_DISPATCHER_STARTUP_PRIORITY));

  }

  @Test
  @DisplayName("The stores and transactions are resolved after the deployment and before the first entry")
  public void theValidationRunsBetweenTheDeploymentAndTheRegistration() {

    assertTrue(
        BusinessCockpitProducer.VALIDATION_STARTUP_PRIORITY > VanillaBpDeploymentRunner.STARTUP_PRIORITY,
        "the extension resolves the stores at priority %d, VanillaBP deploys the workflow modules at %d - and only then does it know which aggregate a BPMN process works on"
            .formatted(
                BusinessCockpitProducer.VALIDATION_STARTUP_PRIORITY,
                VanillaBpDeploymentRunner.STARTUP_PRIORITY));
    assertTrue(
        BusinessCockpitProducer.VALIDATION_STARTUP_PRIORITY < BusinessCockpitProducer.REGISTRATION_STARTUP_PRIORITY,
        "the extension resolves the stores at priority %d and writes its first entry at %d"
            .formatted(
                BusinessCockpitProducer.VALIDATION_STARTUP_PRIORITY,
                BusinessCockpitProducer.REGISTRATION_STARTUP_PRIORITY));

  }

}

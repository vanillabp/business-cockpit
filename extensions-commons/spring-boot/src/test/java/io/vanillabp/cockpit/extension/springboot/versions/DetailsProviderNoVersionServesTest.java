package io.vanillabp.cockpit.extension.springboot.versions;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import io.vanillabp.cockpit.extension.config.ConfigurationKeys;
import io.vanillabp.integration.test.utils.CapturedOutput;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A details provider whose version the BPMS does not hold is named while the application boots.
 * <p>
 * Nothing else would ever say so. A user task without a provider is reported with what the BPMS
 * carried, so a method which never runs looks exactly like a task nobody wrote a provider for,
 * and the developer would wait for an event which does not arrive. The boot keeps running, as
 * it does for a <code>&#64;WorkflowTask</code> method in the same position: a version which is
 * not deployed yet is a normal state during a rolling deployment.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
public class DetailsProviderNoVersionServesTest {

  @Test
  @DisplayName("A provider naming a version nobody deploys is named while booting")
  public void aProviderOfAVersionNobodyDeploysIsReported(
      final CapturedOutput output) {

    try (var ignored = new SpringApplicationBuilder(VersionsApplication.class)
        .web(WebApplicationType.NONE)
        .properties(
            "spring.datasource.url=jdbc:h2:mem:cockpit-versions-unserved;DB_CLOSE_DELAY=-1",
            "vanillabp.cockpit.rest.base-url=http://localhost:1")
        .run()) {

      final var reported = output.getAll();

      assertTrue(reported.contains("auditOfAVersionNobodyDeploys"), reported);
      assertTrue(reported.contains(ConfigurationKeys.EXTENSION_ID), reported);
      assertTrue(reported.contains("the method never runs"), reported);
      // the versions the BPMS does hold belong in the message: without them the developer
      // cannot tell a typo in the range from a deployment which never happened
      assertTrue(reported.contains("held: 1, 2, 3"), reported);

    }

  }

}

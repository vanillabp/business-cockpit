package io.vanillabp.cockpit.coverage;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.integration.test.utils.PublishedPoms;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Checks that a tool which only compiles our source stays off the classpath of the applications
 * which use our artifacts.
 * <p>
 * Lombok and the annotation processors of Quarkus and Spring Boot are used while javac runs.
 * Once the class file exists, they have nothing left to do. An application asked for the
 * cockpit, not for them. Every jar it did not ask for is one more thing to ship and to answer a
 * CVE report about. The scope for such a tool is {@code provided}: it puts the jar on our own
 * compile path and hands it to nobody.
 * <p>
 * The check itself is in {@link PublishedPoms} in the module 'test-utils' of the platform. Every
 * VanillaBP repository can make this mistake, and they all make it in the same way. So the rule
 * lives in one place and each repository calls it. A copy per repository drifts, and the check
 * is only worth something if it still runs in two years. This test is the caller. It names the
 * file this repository publishes and the tools of this build.
 * <p>
 * This repository publishes its POMs as they are. So an application reads the same file a
 * reviewer reads, and the check reads it too. The test lives in the module which already gates
 * this repository as a whole, because it asks about the repository and not about one module.
 */
@ExtendWith(SuppressOutputExtension.class)
public class PublishedPomsTest {

  /**
   * The tools of this build, each as {@code groupId:artifactId}. When this repository starts to
   * use a new tool, add it here, because nothing else knows that it is a tool.
   */
  private static final Set<String> TOOLS_OF_THIS_BUILD = Set
      .of(
          "org.projectlombok:lombok",
          "io.quarkus:quarkus-extension-processor",
          "org.mapstruct:mapstruct-processor");

  @Test
  @DisplayName("no POM of this repository hands an application a tool of the build")
  public void noPomHandsAnApplicationAToolOfTheBuild() {

    PublishedPoms
        .ofTheRepositoryUnderTest(PublishedPoms.THE_SOURCE_POM)
        .handAnApplicationNoToolOfTheBuild(TOOLS_OF_THIS_BUILD);

  }

}

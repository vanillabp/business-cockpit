package io.vanillabp.cockpit.coverage;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.integration.test.utils.CoverageGate;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import io.vanillabp.integration.test.utils.TestClassConventions;

/**
 * What keeps a red build of this repository short: every test class registers
 * {@link SuppressOutputExtension}, so the log carries what a FAILING test printed and
 * little else.
 * <p>
 * A checked rule rather than a reviewed one, because a class which forgets the extension
 * stays quiet as long as its own tests pass and starts talking on the day it gets a mock
 * which warns or a container which boots. By then nobody connects the noise to the class,
 * and the next reader pays for it again.
 * <p>
 * The check reads the test sources, so this one class covers every module of the
 * repository, the ones added after it included.
 */
@ExtendWith(SuppressOutputExtension.class)
public class TestClassConventionsTest {

  @Test
  @DisplayName("Every test class of this repository suppresses its output")
  public void everyTestClassSuppressesItsOutput() {

    final var root = CoverageGate.repositoryRoot("coverage.repository.root");

    final var offenders = TestClassConventions.testClassesWithoutOutputSuppression(root);

    assertTrue(
        offenders.isEmpty(),
        () -> TestClassConventions.describeTestClassesWithoutOutputSuppression(offenders));

  }

  @Test
  @DisplayName("No test class registers the suppression after '@Testcontainers'")
  public void noTestClassSuppressesTooLate() {

    final var root = CoverageGate.repositoryRoot("coverage.repository.root");

    final var offenders = TestClassConventions.testClassesSuppressingTooLate(root);

    assertTrue(
        offenders.isEmpty(),
        () -> TestClassConventions.describeTestClassesSuppressingTooLate(offenders));

  }

}

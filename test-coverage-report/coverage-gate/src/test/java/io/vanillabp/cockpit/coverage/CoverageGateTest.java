package io.vanillabp.cockpit.coverage;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The coverage gate: it breaks the build when a measurement's aggregated coverage drops
 * below the threshold, so a drop is noticed while it happens instead of a year later. The
 * rule is 90 in every VanillaBP repository while the threshold sits below it, which keeps
 * one repository's bad week from being answered by editing the number.
 * <p>
 * JaCoCo's own <code>check</code> goal cannot do this: it judges ONE module's classes
 * against ONE execution-data file, while both numbers of this repository come from
 * aggregated reports spanning many modules. So the gate reads exactly the report which is
 * published, and report and gate can never disagree.
 * <p>
 * The completeness test comes first for a reason: a threshold checked against an
 * incomplete aggregate fails builds for coverage which exists and is only not counted, and
 * nobody can fix that by writing a test.
 * <p>
 * This test class prints while it passes, which no other one does. Here the passing run IS
 * the measurement: it is the only place the build states where each half of the repository
 * stands against the rule, and a repository sitting between the threshold and the rule has
 * a gap which would otherwise be visible only to whoever opens the report.
 */
public class CoverageGateTest {

  private static final Path ROOT = CoverageGate.repositoryRoot("coverage.repository.root");

  /**
   * What a report is expected to show, as opposed to the threshold, which is where the
   * build stops. Reported on every run, never asserted: a build breaking at the rule
   * would leave a repository nothing to do but edit the rule.
   */
  private static final double RULE = Double.parseDouble(System.getProperty("coverage.rule"));

  private static final List<Path> AGGREGATE_POMS = List
      .of(
          ROOT.resolve("test-coverage-report/application/spring-boot/pom.xml"),
          ROOT.resolve("test-coverage-report/adapters/spring-boot/pom.xml"),
          ROOT.resolve("test-coverage-report/extensions-commons/spring-boot/pom.xml"),
          ROOT.resolve("test-coverage-report/extensions-commons/quarkus/pom.xml"));

  /**
   * Modules whose execution data belongs to no coverage report. Each entry is a
   * decision - a module missing here and missing from both aggregates is the defect
   * this test exists for.
   */
  private static final Set<String> DELIBERATELY_NOT_AGGREGATED = Set
      .of(
          // a development aid impersonating a workflow module, so that the cockpit can be
          // run without one. It is deployed nowhere and ships in no artifact, so its
          // coverage would say nothing about what users get.
          "simulator");

  @Test
  @DisplayName("Every module producing coverage data is read by an aggregated report")
  public void everyModuleProducingCoverageDataIsAggregated() {

    final var missing = CoverageGate
        .modulesMissingFromAggregates(ROOT, AGGREGATE_POMS, DELIBERATELY_NOT_AGGREGATED);

    assertTrue(missing.isEmpty(), () -> CoverageGate.describeMissingModules(missing, AGGREGATE_POMS));

  }

  @Test
  @DisplayName("The Business Cockpit container report is above the coverage threshold")
  public void theBusinessCockpitContainerReportIsAboveTheThreshold() {

    assertAboveThreshold(
        "Business Cockpit container (Spring Boot)",
        "application/spring-boot",
        "coverage.threshold.application-spring-boot");

  }

  @Test
  @DisplayName("The BPMS adapters report is above the coverage threshold")
  public void theBpmsAdaptersReportIsAboveTheThreshold() {

    assertAboveThreshold(
        "BPMS adapters (Spring Boot)",
        "adapters/spring-boot",
        "coverage.threshold.adapters-spring-boot");

  }

  @Test
  @DisplayName("The Business Cockpit extension's Spring Boot report is above the coverage threshold")
  public void theExtensionSpringBootReportIsAboveTheThreshold() {

    assertAboveThreshold(
        "Business Cockpit extension (Spring Boot)",
        "extensions-commons/spring-boot",
        "coverage.threshold.extensions-commons-spring-boot");

  }

  @Test
  @DisplayName("The Business Cockpit extension's Quarkus report is above the coverage threshold")
  public void theExtensionQuarkusReportIsAboveTheThreshold() {

    assertAboveThreshold(
        "Business Cockpit extension (Quarkus)",
        "extensions-commons/quarkus",
        "coverage.threshold.extensions-commons-quarkus");

  }

  /**
   * The threshold is read per measurement, because a report exists per measurement.
   * Both numbers are the floor against regression rather than the goal: the rule is the
   * {@code coverage.rule} property, and a report between the two has a gap somebody
   * still owes a test for.
   */
  private void assertAboveThreshold(
      final String measurement,
      final String reportDirectory,
      final String thresholdProperty) {

    final var threshold = Double.parseDouble(System.getProperty(thresholdProperty));

    final var coverage = CoverageGate
        .read(
            ROOT
                .resolve("test-coverage-report")
                .resolve(reportDirectory)
                .resolve("report")
                .resolve("jacoco.csv"),
            measurement,
            CoverageGate.Metric.INSTRUCTIONS);

    report(coverage, threshold);

    assertTrue(
        coverage.percentage() >= threshold,
        () -> """
            %s - below the %s %% at which every VanillaBP build stops. The rule is %s, so \
            anything under that is already a gap, and this number is where the gap grew too big to \
            carry. Coverage is measured separately per report, so this report's own tests have to \
            close it: sort the per-package numbers of the report's jacoco.csv by MISSED \
            instructions, not by percentage, and put the test where the uncovered code belongs. \
            Code nobody can reach is dead and gets deleted rather than covered."""
            .formatted(coverage, plain(threshold), plain(RULE)));

  }

  /**
   * The line a passing run leaves behind. It names both numbers, because the one which
   * breaks the build is not the one to aim at, and it says how far a report is from the
   * rule while that distance is still small enough to close.
   */
  private void report(
      final CoverageGate.Coverage coverage,
      final double threshold) {

    final var verdict = coverage.percentage() >= RULE
        ? "at the rule of %s %%".formatted(plain(RULE))
        : "%.2f points below the rule of %s %%, build breaks below %s %%"
            .formatted(RULE - coverage.percentage(), plain(RULE), plain(threshold));

    System.out.println("coverage gate | %s | %s".formatted(coverage, verdict));

  }

  /** A whole percentage without the '.0' a double would print. */
  private static String plain(
      final double percentage) {

    return percentage == Math.rint(percentage)
        ? String.valueOf((long) percentage)
        : String.valueOf(percentage);

  }

}

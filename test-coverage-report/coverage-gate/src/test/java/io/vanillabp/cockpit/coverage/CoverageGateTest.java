package io.vanillabp.cockpit.coverage;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.vanillabp.integration.test.utils.CoverageGate;
import io.vanillabp.integration.test.utils.PrintsWhenPassing;

/**
 * The coverage gate. It breaks the build when the aggregated coverage of a report drops
 * below its threshold, so a drop is noticed when it happens and not a year later. The rule
 * is 90 in every VanillaBP repository. The threshold sits below it, so one bad week of a
 * repository is not answered by editing the number.
 * <p>
 * JaCoCo's own <code>check</code> goal cannot do this. It judges the classes of ONE module
 * against ONE execution data file, but the numbers of this repository come from aggregated
 * reports over many modules. So the gate reads exactly the report which is published, and
 * report and gate never disagree.
 * <p>
 * The completeness test comes first for a reason. A threshold checked against an incomplete
 * aggregate fails builds for coverage which exists but is not counted, and no new test can fix
 * that.
 * <p>
 * This is the only test class of this repository which prints while it passes, so it is the
 * only one without {@code SuppressOutputExtension}. Everywhere else a passing test prints
 * nothing, because output nobody reads hides the output somebody has to read. Here the passing
 * run IS the measurement. It is the only place where the build says how far each report is
 * from the rule. Without it, a repository between the threshold and the rule has a gap which
 * only somebody who opens the report sees.
 */
@PrintsWhenPassing(
  "the measurement is the result: what each report reached belongs in the log of every build "
      + "and not only of a red one, so whoever just wrote tests reads there whether the gap to the "
      + "rule got smaller")
public class CoverageGateTest {

  private static final Path ROOT = CoverageGate.repositoryRoot("coverage.repository.root");

  /**
   * What a report is expected to show. The threshold, in contrast, is where the build stops.
   * The rule is reported on every run but never asserted. If the build broke at the rule, a
   * repository could do nothing but edit the rule.
   */
  private static final double RULE = Double.parseDouble(System.getProperty("coverage.rule"));

  /**
   * The command line this build was started with. The Surefire configuration of the module
   * passes it in. It answers the one question the gate cannot read from the disk: whether this
   * run writes the reports at all.
   */
  private static final String MAVEN_COMMAND_LINE = System.getProperty("coverage.maven.command", "");

  private static final List<Path> AGGREGATE_POMS = List
      .of(
          ROOT.resolve("test-coverage-report/application/spring-boot/pom.xml"),
          ROOT.resolve("test-coverage-report/extensions-commons/spring-boot/pom.xml"),
          ROOT.resolve("test-coverage-report/extensions-commons/quarkus/pom.xml"));

  /**
   * Modules whose execution data belongs to no coverage report. Each entry is a decision. A
   * module which is missing here and from every aggregate is the defect this test looks for.
   */
  private static final Set<String> DELIBERATELY_NOT_AGGREGATED = Set
      .of(
          // a development aid which plays a workflow module, so that the cockpit can be
          // run without one. It is deployed nowhere and ships in no artifact, so its
          // coverage would say nothing about what users get.
          "simulator",
          // the backend of the dev shell, which serves made up users and cases while a
          // developer builds a user task form. Same reason as above. It runs on a
          // developer's machine and reaches no user.
          "dev-shell-simulator");

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
   * A run which stops before <code>verify</code> writes no report. Then the gate says so and
   * skips, instead of failing over a file this run could not have written. It does not stay
   * quiet about it: it prints the line and reports the test as skipped, so nobody takes a green
   * run for a checked one.
   * <p>
   * The threshold is read per report, because each report has its own. In this repository the
   * container report has a lower one than the two extension reports. A threshold is the floor
   * against a regression and not the goal. The goal is the {@code coverage.rule} property. A
   * report between the two still owes a test for the gap.
   */
  private void assertAboveThreshold(
      final String measurement,
      final String reportDirectory,
      final String thresholdProperty) {

    if (CoverageGate.stopsBeforeTheReportsAreWritten(MAVEN_COMMAND_LINE)) {
      final var reason = CoverageGate.describeRunWithoutReports(MAVEN_COMMAND_LINE);
      System.out.println("coverage gate | %s | %s".formatted(measurement, reason));
      Assumptions.abort(reason);
    }

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
   * Prints the line a passing run leaves behind. It names both numbers, because the number
   * which breaks the build is not the one to aim at. It also says how far a report is from the
   * rule, while that distance is still small enough to close.
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

package io.vanillabp.cockpit.coverage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The two questions a repository's coverage gate has to answer, asked the same way in
 * every VanillaBP repository.
 * <p>
 * This is a local copy of <code>io.vanillabp:test-utils</code>'s class of the same name.
 * That artifact is not released to Maven Central yet, and the release build of this
 * repository resolves against Central only, so it cannot be depended upon here. Replace
 * the copy with the artifact once it is released.
 * <p>
 * The gate exists because of what an audit of another repository found: seven modules
 * produced execution data which no aggregated report ever read, so everything covered ONLY
 * by them counted as missed. That is why {@link #modulesMissingFromAggregates} comes first
 * - a threshold checked against an incomplete aggregate fails builds for coverage which
 * exists and is only not counted, and no test can fix that.
 */
public final class CoverageGate {

  /**
   * The metric the coverage rule is measured in. VanillaBP measures
   * {@link #INSTRUCTIONS}: it is JaCoCo's own headline metric, it does not move when
   * code is only reformatted (line coverage depends on the compiler's line table),
   * and it is the stricter of the two in this codebase.
   */
  public enum Metric {

    /** Column 'INSTRUCTION_MISSED' / 'INSTRUCTION_COVERED' of JaCoCo's CSV. */
    INSTRUCTIONS(3, 4),

    /** Column 'LINE_MISSED' / 'LINE_COVERED' of JaCoCo's CSV. */
    LINES(7, 8);

    private final int missedColumn;

    private final int coveredColumn;

    Metric(
        final int missedColumn,
        final int coveredColumn) {

      this.missedColumn = missedColumn;
      this.coveredColumn = coveredColumn;

    }

  }

  /**
   * What one aggregated report says, in one metric.
   *
   * @param report The report's name, used in the failure message
   * @param metric The metric the numbers are counted in
   * @param missed The number of missed items
   * @param covered The number of covered items
   */
  public record Coverage(
                         String report,
                         Metric metric,
                         long missed,
                         long covered) {

    /** The covered ratio in percent, or 0 if the report holds nothing at all. */
    public double percentage() {

      final var total = missed + covered;
      return total == 0 ? 0.0d : (covered * 100.0d) / total;

    }

    @Override
    public String toString() {

      return "%s: %.2f %% %s (%d of %d missed)"
          .formatted(
              report,
              percentage(),
              metric.name().toLowerCase(Locale.ROOT),
              missed,
              missed + covered);

    }

  }

  private static final Pattern DEPENDENCY = Pattern
      .compile("<dependency>(.*?)</dependency>", Pattern.DOTALL);

  private static final Pattern ARTIFACT_ID = Pattern.compile("<artifactId>([^<]+)</artifactId>");

  private CoverageGate() {
    // utility class
  }

  /**
   * Reads the totals of an aggregated report from the <code>jacoco.csv</code> JaCoCo
   * writes next to it.
   *
   * @param jacocoCsv The report's <code>jacoco.csv</code>
   * @param report The report's name, used in the failure message
   * @param metric The metric to count
   * @return The totals of that report
   */
  public static Coverage read(
      final Path jacocoCsv,
      final String report,
      final Metric metric) {

    if (!Files.isRegularFile(jacocoCsv)) {
      throw new IllegalStateException(
          """
              The coverage report '%s' was not found at '%s'! The gate can only judge a report which \
              was built: run the full build ('mvn install') instead of a single module."""
              .formatted(report, jacocoCsv));
    }

    var missed = 0L;
    var covered = 0L;
    try (var lines = Files.lines(jacocoCsv, StandardCharsets.UTF_8)) {
      for (final var line : lines.skip(1).toList()) {
        final var columns = line.split(",");
        if (columns.length <= metric.coveredColumn) {
          continue;
        }
        missed += Long.parseLong(columns[metric.missedColumn].trim());
        covered += Long.parseLong(columns[metric.coveredColumn].trim());
      }
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
    return new Coverage(report, metric, missed, covered);

  }

  /**
   * Every module which produced execution data has to be aggregated by one of the
   * reports, otherwise what only its tests cover counts as missed.
   * <p>
   * A module is recognised by its <code>target/jacoco.exec</code>, so modules without
   * tests need no entry anywhere and adding a test to a module which no report
   * aggregates is what this reports.
   *
   * @param repositoryRoot The repository's root directory
   * @param aggregatePoms The POMs of the reports, whose
   *          <code>&lt;dependency&gt;</code> entries are the aggregated modules
   * @param deliberatelyNotAggregated Artifact IDs of modules whose execution data
   *          belongs to no report - each of them a decision, not an oversight
   * @return The artifact IDs of the modules nobody aggregates, empty if there are
   *         none
   */
  public static List<String> modulesMissingFromAggregates(
      final Path repositoryRoot,
      final Collection<Path> aggregatePoms,
      final Set<String> deliberatelyNotAggregated) {

    final var aggregated = new ArrayList<String>();
    aggregatePoms.forEach(pom -> aggregated.addAll(artifactIdsOfDependencies(pom)));

    final var missing = new ArrayList<String>();
    try (var files = Files.walk(repositoryRoot)) {
      files
          .filter(path -> path.endsWith(Path.of("target", "jacoco.exec")))
          .map(path -> path
              .getParent()
              .getParent())
          .map(CoverageGate::artifactIdOf)
          .filter(Objects::nonNull)
          .filter(artifactId -> !deliberatelyNotAggregated.contains(artifactId))
          .filter(artifactId -> !aggregated.contains(artifactId))
          .distinct()
          .sorted()
          .forEach(missing::add);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
    return missing;

  }

  /**
   * The message the gate fails with - it names what to change instead of only saying
   * that something is wrong.
   *
   * @param missing The artifact IDs {@link #modulesMissingFromAggregates} found
   * @param aggregatePoms The POMs to add them to
   * @return The message
   */
  public static String describeMissingModules(
      final List<String> missing,
      final Collection<Path> aggregatePoms) {

    return """
        %d module(s) produce coverage data which no aggregated report reads: %s. Everything covered \
        ONLY by their tests counts as missed, which is exactly what this gate is about. \
        Add each of them as a <dependency> to the report they belong to (%s) - or, if their data \
        belongs to no report, say so in this test's list of deliberate exceptions and why."""
        .formatted(
            missing.size(),
            String.join(", ", missing),
            aggregatePoms
                .stream()
                .map(Path::toString)
                .reduce((
                    one,
                    other) -> one
                        + ", "
                        + other)
                .orElse(""));

  }

  private static List<String> artifactIdsOfDependencies(
      final Path pom) {

    final String content;
    try {
      content = Files.readString(pom, StandardCharsets.UTF_8);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
    return DEPENDENCY
        .matcher(content)
        .results()
        .map(match -> ARTIFACT_ID.matcher(match.group(1)))
        .filter(Matcher::find)
        .map(matcher -> matcher.group(1))
        .toList();

  }

  private static String artifactIdOf(
      final Path moduleDirectory) {

    final var pom = moduleDirectory.resolve("pom.xml");
    if (!Files.isRegularFile(pom)) {
      return null;
    }
    final String content;
    try {
      content = Files.readString(pom, StandardCharsets.UTF_8);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
    // the project's own artifact ID is the first one outside <parent>
    final var withoutParent = content.replaceAll("(?s)<parent>.*?</parent>", "");
    final var matcher = ARTIFACT_ID.matcher(withoutParent);
    return matcher.find() ? matcher.group(1) : null;

  }

  /**
   * Convenience for a gate test: the repository root passed in as a system property
   * by the module's Surefire configuration.
   *
   * @param systemProperty The property's name
   * @return The repository's root directory
   */
  public static Path repositoryRoot(
      final String systemProperty) {

    final var configured = System.getProperty(systemProperty);
    if ((configured == null) || configured.isBlank()) {
      throw new IllegalStateException(
          """
              The system property '%s' is not set! The coverage gate needs the repository's root \
              directory - set it in the module's maven-surefire-plugin configuration to \
              ${project.basedir}/../.."""
              .formatted(systemProperty));
    }
    return Path.of(configured).toAbsolutePath().normalize();

  }

}

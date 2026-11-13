package io.vanillabp.cockpit.bpms.api.protobuf.v1;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A generated class refuses to load on a protobuf-java older than the protoc which wrote it. The
 * classes of this module run in the applications of others, on the protobuf-java their platform
 * manages. So the protoc of this build must not be newer than the oldest runtime a supported platform
 * manages. The POM of this module names that runtime and says why.
 */
@ExtendWith(SuppressOutputExtension.class)
class ProtocIsNotNewerThanThePlatformsTest {

  private static final Pattern GENCODE_VERSION = Pattern.compile(
      "validateProtobufGencodeVersion\\(\\s*[\\w.]+,\\s*/\\* major= \\*/ (\\d+),\\s*/\\* minor= \\*/ (\\d+),"
          + "\\s*/\\* patch= \\*/ (\\d+),");

  private static final Comparator<List<Integer>> BY_VERSION = Comparator
      .<List<Integer>, Integer>comparing(version -> version.get(0))
      .thenComparing(version -> version.get(1))
      .thenComparing(version -> version.get(2));

  @Test
  void theGeneratedClassesRunOnTheOldestRuntimeOfAPlatform() throws IOException {

    final var lowestPlatformRuntime = versionOf(System.getProperty("protobuf.lowest-platform-runtime"));
    final var generatedBy = generatedBy(Path.of(System.getProperty("protobuf.generated-sources")));

    assertThat(BY_VERSION.compare(generatedBy, lowestPlatformRuntime))
        .as("protoc %s wrote the classes, but a supported platform runs them on protobuf-java %s", generatedBy,
            lowestPlatformRuntime)
        .isLessThanOrEqualTo(0);

  }

  @Test
  void theGeneratedClassesLoadOnTheRuntimeOfThisBuild() {

    assertThat(BcEvent.getDefaultInstance()).isNotNull();

  }

  private static List<Integer> generatedBy(
      final Path generatedSources) throws IOException {

    final var source = Files.readString(generatedSources.resolve(
        Path.of("io", "vanillabp", "cockpit", "bpms", "api", "protobuf", "v1", "BcEvent.java")));
    final var matcher = GENCODE_VERSION.matcher(source);
    assertThat(matcher.find())
        .as("the version of protoc in BcEvent.java")
        .isTrue();
    return List.of(Integer.valueOf(matcher.group(1)), Integer.valueOf(matcher.group(2)),
        Integer.valueOf(matcher.group(3)));

  }

  private static List<Integer> versionOf(
      final String version) {

    final var parts = version.split("\\.");
    return List.of(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]), Integer.valueOf(parts[2]));

  }

}

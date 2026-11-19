package io.vanillabp.cockpit.bpms.kafka;

import java.time.Duration;

import org.testcontainers.kafka.KafkaContainer;

import io.vanillabp.integration.test.utils.ContainerImages;

/**
 * The Kafka broker of the tests, a {@link KafkaContainer} which does not fail at random when it
 * starts.
 * <p>
 * The image is {@code ContainerImages.KAFKA} of the test utilities, overridable with
 * {@code -Dkafka.image=...} like every image there.
 * <p>
 * Testcontainers starts the container with a shell loop which waits until a start script exists
 * and then runs it as a program. The script is copied into the container after the container
 * started. The loop may see the file while the copy still has it open for writing. Then Linux
 * refuses to run it, the shell prints {@code /tmp/testcontainers_start.sh: Text file busy}, the
 * container exits with code 126, and the test fails with a timeout of the wait strategy. This
 * happened on GitHub runners now and then.
 * <p>
 * This broker waits until the last line of the script is there and hands the script to a shell
 * to read. A shell which reads a file does not need the file to be closed, so there is no "Text
 * file busy" any more.
 */
public class KafkaBroker extends KafkaContainer {

  /** The script Testcontainers copies into the container after it started. */
  private static final String START_SCRIPT = "/tmp/testcontainers_start.sh";

  /** The last line of the start script, which starts Kafka itself. */
  private static final String LAST_LINE_OF_THE_START_SCRIPT = "/etc/kafka/docker/run";

  /** Kafka starts in a few seconds. A busy build machine gets more time than the default minute. */
  private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(3);

  public KafkaBroker() {

    super(ContainerImages.KAFKA);
    withCommand(
        "sh",
        "-c",
        "while ! grep -qs '^"
            + LAST_LINE_OF_THE_START_SCRIPT
            + "' "
            + START_SCRIPT
            + "; do sleep 0.1; done; exec sh "
            + START_SCRIPT);
    withStartupTimeout(STARTUP_TIMEOUT);

  }

}

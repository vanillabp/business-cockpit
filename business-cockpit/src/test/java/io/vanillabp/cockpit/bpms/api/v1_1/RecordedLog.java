package io.vanillabp.cockpit.bpms.api.v1_1;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Records what the given classes log, so a test can say which lines a report left in the log. The
 * services write to a logger of their own class, so the test cannot hand them one, and it listens
 * on those loggers instead. A test class calls {@link #stopAll()} after each test.
 */
final class RecordedLog {

  private static final Queue<RecordedLog> RECORDING = new ConcurrentLinkedQueue<>();

  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

  private final List<Logger> loggers;

  RecordedLog(
      final Class<?>... classes) {

    appender.start();
    loggers = Arrays
        .stream(classes)
        .map(type -> (Logger) LoggerFactory.getLogger(type))
        .toList();
    loggers.forEach(logger -> logger.addAppender(appender));
    RECORDING.add(this);

  }

  /**
   * How many lines on level INFO were logged whose pattern contains the given text and whose
   * arguments are the given ones, in this order.
   *
   * @param partOfThePattern A part of the message pattern, before the arguments are filled in
   * @param arguments The arguments of the line
   * @return The number of such lines
   */
  long infos(
      final String partOfThePattern,
      final Object... arguments) {

    return appender.list
        .stream()
        .filter(event -> event.getLevel() == Level.INFO)
        .filter(event -> event.getMessage().contains(partOfThePattern))
        .filter(event -> Arrays.equals(event.getArgumentArray(), arguments))
        .count();

  }

  /**
   * Stops every recording which was started since the last call.
   */
  static void stopAll() {

    RecordedLog recorded;
    while ((recorded = RECORDING.poll()) != null) {
      final var appender = recorded.appender;
      recorded.loggers.forEach(logger -> logger.detachAppender(appender));
      appender.stop();
    }

  }

}

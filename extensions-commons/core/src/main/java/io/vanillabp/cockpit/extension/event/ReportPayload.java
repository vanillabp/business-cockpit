package io.vanillabp.cockpit.extension.event;

import java.io.IOException;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;
import io.vanillabp.integration.spi.PhaseTwoPermanentFailure;

/**
 * The finished report, written as the bytes an outbox entry carries and read back when that
 * entry is dispatched.
 * <p>
 * A report is built while the BPMS event is being observed, so it has to survive the wait for
 * the dispatch somewhere. VanillaBP keeps the bytes of a phase-two call beside the entry and
 * hands them back at the dispatch, which is what lets a report carry the state of its event
 * instead of the state of its send. See decision 26 in the repository's DECISIONS.md.
 * <p>
 * The format is JSON, and nobody outside this class reads it. It is written and read by one
 * application, within seconds and by the same version of the code, so it is an internal form
 * and not a contract with anybody.
 *
 * @see io.vanillabp.integration.spi.PhaseTwoCall#payload()
 */
public final class ReportPayload {

  /**
   * The mapper of the payload. It writes what the transports write, so a value reaches the
   * cockpit server the way it always did: dates as ISO-8601 in UTC, and nothing absent written
   * out.
   * <p>
   * Three settings are its own. A detail which was set to nothing is written out as null
   * instead of being left out, because that is a value a details provider chose and the
   * transports have a case for it. A decimal number is read back as a
   * {@link java.math.BigDecimal}, because the cockpit is told a number as its decimal text and a
   * detour through a double would change a business amount on the way. And a property the
   * reader does not know is skipped, so an entry written by the instance which is being replaced
   * during a rolling deployment is still dispatched by the instance which replaces it.
   */
  private static final ObjectMapper JSON = mapper();

  private ReportPayload() {
  }

  /**
   * @param event The report of a user task, built and enriched
   * @return The bytes the outbox entry carries
   * @throws IllegalStateException If a value of the report cannot be written (guiding message)
   */
  public static byte[] of(
      final UserTaskEvent event) {

    return bytesOf(event, "user task '%s'".formatted(event.getUserTaskId()));

  }

  /**
   * @param event The report of a workflow, built and enriched
   * @return The bytes the outbox entry carries
   * @throws IllegalStateException If a value of the report cannot be written (guiding message)
   */
  public static byte[] of(
      final WorkflowEvent event) {

    return bytesOf(event, "workflow '%s'".formatted(event.getWorkflowId()));

  }

  /**
   * @param payload The bytes of the entry being dispatched
   * @return The report as it was built at the event
   * @throws PhaseTwoPermanentFailure If the bytes cannot be read, which repeating the entry
   *           does not heal
   */
  public static UserTaskEvent userTaskEvent(
      final byte[] payload) {

    return reportFrom(payload, UserTaskEvent.class);

  }

  /**
   * @param payload The bytes of the entry being dispatched
   * @return The report as it was built at the event
   * @throws PhaseTwoPermanentFailure If the bytes cannot be read, which repeating the entry
   *           does not heal
   */
  public static WorkflowEvent workflowEvent(
      final byte[] payload) {

    return reportFrom(payload, WorkflowEvent.class);

  }

  /**
   * Writes a report, and says whose report it was where a value of it cannot be written.
   * <p>
   * This happens in the transaction of the BPMS event, so the application hears about it at the
   * moment its own code put the value in. Before reports were built at the event, the same
   * value ended as a failed outbox entry which was repeated until it was blocked, and the line
   * saying so was hours away from the code which wrote it.
   */
  private static byte[] bytesOf(
      final Object event,
      final String described) {

    try {
      return JSON.writeValueAsBytes(event);
    } catch (final JsonProcessingException | RuntimeException e) {
      throw new IllegalStateException(
          """
              The Business Cockpit cannot write its report about %s: one of the values of the \
              report cannot be turned into JSON. The report is put together while the BPMS event \
              is observed and it travels with the outbox entry, so every value your details \
              provider sets has to be writable. Set values Jackson can write - the fields of your \
              business data rather than an object of a framework - or register what it takes to \
              write them on the ObjectMapper of your application."""
              .formatted(described), e);
    }

  }

  private static <T> T reportFrom(
      final byte[] payload,
      final Class<T> type) {

    try {
      return JSON.readValue(payload, type);
    } catch (final IOException | RuntimeException e) {
      throw new PhaseTwoPermanentFailure(
          """
              The outbox entry of a Business Cockpit report carries %d bytes which are no report \
              this version of the adapter can read. The entry cannot be dispatched and repeating \
              it will not change that; remove it from the outbox store."""
              .formatted(payload == null ? 0 : payload.length), e);
    }

  }

  private static ObjectMapper mapper() {

    return JsonMapper
        .builder()
        .addModule(new JavaTimeModule())
        .addMixIn(UserTaskEvent.class, UserTaskEventShape.class)
        .addMixIn(WorkflowEvent.class, WorkflowEventShape.class)
        .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
        .defaultTimeZone(TimeZone.getTimeZone("UTC"))
        .defaultPropertyInclusion(
            JsonInclude.Value
                .construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.ALWAYS))
        .build();

  }

  /**
   * What Jackson has to be told about a user-task report, kept out of the event class itself so
   * that the event stays the object a details provider works with.
   * <p>
   * The kind of event is final and is set when the report is built, so it is read through the
   * constructor. The three properties left out are read from another field, are always empty or
   * hold whatever an application handed the templating; the titles are rendered at the event,
   * so the context which rendered them has nothing left to do at the dispatch.
   */
  @JsonIgnoreProperties({
      "id", "eventTimestamp", "detailsCharacteristics", "templateContext"
  })
  private abstract static class UserTaskEventShape {

    @JsonCreator
    UserTaskEventShape(
        @JsonProperty("eventKind") final UserTaskEventKind eventKind) {
    }

  }

  /** What Jackson has to be told about a workflow report, for the reasons above. */
  @JsonIgnoreProperties({
      "eventTimestamp", "detailsCharacteristics", "templateContext"
  })
  private abstract static class WorkflowEventShape {

    @JsonCreator
    WorkflowEventShape(
        @JsonProperty("eventKind") final WorkflowEventKind eventKind) {
    }

  }

}

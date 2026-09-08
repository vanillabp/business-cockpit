package io.vanillabp.cockpit.extension.transport;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;

import feign.FeignException;
import feign.RetryableException;
import feign.auth.BasicAuthRequestInterceptor;
import io.vanillabp.cockpit.bpms.api.v1_1.ApiClient;
import io.vanillabp.cockpit.bpms.api.v1_1.BpmsApi;
import io.vanillabp.cockpit.extension.config.RestTransportConfiguration;
import io.vanillabp.cockpit.extension.event.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.extension.event.UserTaskEvent;
import io.vanillabp.cockpit.extension.event.WorkflowEvent;
import io.vanillabp.integration.spi.PhaseTwoPermanentFailure;
import io.vanillabp.integration.spi.PhaseTwoRetryLater;

/**
 * Reports events to the cockpit server over its REST API, version 1.1.
 * <p>
 * The client is the one generated from the cockpit's own OpenAPI document, so the payloads
 * cannot drift from what the server accepts.
 * <p>
 * What the server answers decides what happens to the outbox entry. A status which says "not
 * now" - the server is unavailable, or asks to slow down - gives the entry back with the time
 * the server named. A status which says "not like this" - anything else the client is blamed
 * for - ends the entry, because the same bytes would be refused again and an entry retried
 * forever hides the report which is actually broken. Everything else is repeated.
 */
public class RestTransport implements BusinessCockpitTransport {

  /** What the cockpit server serves its BPMS API of version 1.1 under. */
  static final String API_PATH = "/bpms/api/v1_1";

  /** The header a server names the length of its own unavailability in. */
  static final String RETRY_AFTER_HEADER = "Retry-After";

  private static final int FIRST_CLIENT_ERROR = 400;

  private static final int REQUEST_TIMEOUT = 408;

  private static final int TOO_MANY_REQUESTS = 429;

  private static final int FIRST_SERVER_ERROR = 500;

  private static final int SERVICE_UNAVAILABLE = 503;

  private final BpmsApi api;

  private final String baseUrl;

  /**
   * @param configuration Where the server is and how to authenticate
   */
  public RestTransport(
      final RestTransportConfiguration configuration) {

    this.baseUrl = configuration.baseUrl();
    final var client = new ApiClient();
    client
        .setBasePath(
            baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1) + API_PATH
                : baseUrl + API_PATH);
    if (configuration.authenticates()) {
      client
          .getFeignBuilder()
          .requestInterceptor(
              new BasicAuthRequestInterceptor(
                  configuration.username(), configuration.password()));
    }
    this.api = client.buildClient(BpmsApi.class);

  }

  /**
   * @param api A client built elsewhere - what a test pointing at its own HTTP server hands in
   */
  public RestTransport(
      final BpmsApi api,
      final String baseUrl) {

    this.api = api;
    this.baseUrl = baseUrl;

  }

  @Override
  public String describe() {

    return "the cockpit server's REST API at %s".formatted(baseUrl);

  }

  @Override
  public void publishUserTaskEvent(
      final UserTaskEvent event) {

    send(
        "user task '%s' as %s".formatted(event.getUserTaskId(), event.getEventKind()),
        () -> {
          switch (event.getEventKind()) {
            case CREATED -> api.userTaskCreatedEvent(UserTaskRestMapper.mapCreated(event));
            case UPDATED -> api
                .userTaskUpdatedEvent(event.getUserTaskId(), UserTaskRestMapper.mapUpdated(event));
            case COMPLETED -> api
                .userTaskCompletedEvent(
                    event.getUserTaskId(), UserTaskRestMapper.mapCompleted(event));
            case CANCELED -> api
                .userTaskCancelledEvent(
                    event.getUserTaskId(), UserTaskRestMapper.mapCancelled(event));
          }
        });

  }

  @Override
  public void publishWorkflowEvent(
      final WorkflowEvent event) {

    send(
        "workflow '%s' as %s".formatted(event.getWorkflowId(), event.getEventKind()),
        () -> {
          switch (event.getEventKind()) {
            case CREATED -> api.workflowCreatedEvent(WorkflowRestMapper.mapCreated(event));
            case UPDATED -> api
                .workflowUpdatedEvent(event.getWorkflowId(), WorkflowRestMapper.mapUpdated(event));
            case COMPLETED -> api
                .workflowCompletedEvent(
                    event.getWorkflowId(), WorkflowRestMapper.mapCompleted(event));
            case CANCELLED -> api
                .workflowCancelledEvent(
                    event.getWorkflowId(), WorkflowRestMapper.mapCancelled(event));
          }
        });

  }

  @Override
  public void registerWorkflowModule(
      final RegisterWorkflowModuleEvent event) {

    send(
        "workflow module '%s'".formatted(event.workflowModuleId()),
        () -> api
            .registerWorkflowModule(
                event.workflowModuleId(), WorkflowModuleRestMapper.map(event)));

  }

  /**
   * Runs one call and turns what the server answered into what the outbox is to do about it.
   *
   * @param what The report being sent, for the message of a failure
   * @param call The call
   */
  private void send(
      final String what,
      final Runnable call) {

    try {
      call.run();
    } catch (final FeignException e) {
      throw failureOf(what, e);
    }

  }

  /**
   * What a status the server answered with means for the entry.
   *
   * @param what The report being sent
   * @param failure What the client threw
   * @return The exception ending this dispatch
   */
  private RuntimeException failureOf(
      final String what,
      final FeignException failure) {

    final var status = failure.status();
    if ((status == SERVICE_UNAVAILABLE) || (status == TOO_MANY_REQUESTS)) {
      return new PhaseTwoRetryLater(
          """
              Reporting %s to %s was answered with %d, so the server is not taking reports right \
              now. The report waits and is sent again."""
              .formatted(what, describe(), status), retryAfterOf(failure));
    }
    if ((status >= FIRST_CLIENT_ERROR) && (status < FIRST_SERVER_ERROR) && (status != REQUEST_TIMEOUT)) {
      return new PhaseTwoPermanentFailure(
          """
              Reporting %s to %s was refused with %d, and sending the same report again would be \
              refused again. Check whether the workflow module and the cockpit server speak the \
              same version of the BPMS API and whether the credentials of '%s' are the ones the \
              server knows."""
              .formatted(what, describe(), status, baseUrl), failure);
    }
    return failure;

  }

  /**
   * @param failure What the client threw
   * @return How long the server asked to wait, or <code>null</code> where it did not say and
   *         the outbox store's own backoff decides
   */
  private static Duration retryAfterOf(
      final FeignException failure) {

    // Feign reads 'Retry-After' itself and hands the moment on rather than the header, so that
    // is the first place to look; the header is read where an answer came through unparsed
    if (failure instanceof final RetryableException retryable) {
      final var until = retryable.retryAfter();
      if (until != null) {
        return positive(Duration.between(Instant.now(), Instant.ofEpochMilli(until)));
      }
    }
    return retryAfterOf(failure.responseHeaders());

  }

  /**
   * @param headers What the server answered with
   * @return How long the server asked to wait, or <code>null</code> where it did not say
   */
  private static Duration retryAfterOf(
      final Map<String, Collection<String>> headers) {

    final var values = headers == null
        ? null
        : headers
            .entrySet()
            .stream()
            .filter(header -> header.getKey().equalsIgnoreCase(RETRY_AFTER_HEADER))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
    if ((values == null) || values.isEmpty()) {
      return null;
    }
    final var value = values.iterator().next().trim();
    try {
      return Duration.ofSeconds(Long.parseLong(value));
    } catch (final NumberFormatException e) {
      // the other spelling of the header: the moment the server expects to be back
    }
    try {
      return positive(
          Duration
              .between(
                  OffsetDateTime.now(),
                  OffsetDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)));
    } catch (final RuntimeException e) {
      return null;
    }

  }

  /**
   * @param waiting How long the server asked to wait
   * @return The same, or <code>null</code> where the moment it named has passed already and the
   *         store's own backoff is the better answer
   */
  private static Duration positive(
      final Duration waiting) {

    return (waiting == null) || waiting.isNegative() || waiting.isZero() ? null : waiting;

  }

}

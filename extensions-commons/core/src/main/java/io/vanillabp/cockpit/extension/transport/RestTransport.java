package io.vanillabp.cockpit.extension.transport;

import io.vanillabp.cockpit.bpms.api.v1_1.ApiClient;
import io.vanillabp.cockpit.bpms.api.v1_1.BpmsApi;
import io.vanillabp.cockpit.extension.config.RestTransportConfiguration;
import io.vanillabp.cockpit.extension.event.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.extension.event.UserTaskEvent;
import io.vanillabp.cockpit.extension.event.WorkflowEvent;

/**
 * Reports events to the cockpit server over its REST API, version 1.1.
 * <p>
 * The client is the one generated from the cockpit's own OpenAPI document, so the payloads
 * cannot drift from what the server accepts. What a failed call means is decided by the
 * dispatch which called this: an exception here means the entry is retried.
 */
public class RestTransport implements BusinessCockpitTransport {

  /** What the cockpit server serves its BPMS API of version 1.1 under. */
  static final String API_PATH = "/bpms/api/v1_1";

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
              new feign.auth.BasicAuthRequestInterceptor(
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

    switch (event.getEventKind()) {
      case CREATED -> api.userTaskCreatedEvent(UserTaskRestMapper.mapCreated(event));
      case UPDATED -> api
          .userTaskUpdatedEvent(event.getUserTaskId(), UserTaskRestMapper.mapUpdated(event));
      case COMPLETED -> api
          .userTaskCompletedEvent(event.getUserTaskId(), UserTaskRestMapper.mapCompleted(event));
      case CANCELED -> api
          .userTaskCancelledEvent(event.getUserTaskId(), UserTaskRestMapper.mapCancelled(event));
    }

  }

  @Override
  public void publishWorkflowEvent(
      final WorkflowEvent event) {

    switch (event.getEventKind()) {
      case CREATED -> api.workflowCreatedEvent(WorkflowRestMapper.mapCreated(event));
      case UPDATED -> api
          .workflowUpdatedEvent(event.getWorkflowId(), WorkflowRestMapper.mapUpdated(event));
      case COMPLETED -> api
          .workflowCompletedEvent(event.getWorkflowId(), WorkflowRestMapper.mapCompleted(event));
      case CANCELLED -> api
          .workflowCancelledEvent(event.getWorkflowId(), WorkflowRestMapper.mapCancelled(event));
    }

  }

  @Override
  public void registerWorkflowModule(
      final RegisterWorkflowModuleEvent event) {

    api
        .registerWorkflowModule(
            event.workflowModuleId(), WorkflowModuleRestMapper.map(event));

  }

}

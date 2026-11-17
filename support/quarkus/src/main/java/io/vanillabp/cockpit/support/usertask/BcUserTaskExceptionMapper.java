package io.vanillabp.cockpit.support.usertask;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import jakarta.ws.rs.core.MediaType;

/**
 * Answers the two exceptions of a user task for every REST endpoint of the application. The body
 * is the message of the exception as plain text, so a form can show it to the user. It is the
 * Quarkus twin of {@code BcUserTaskExceptionHandler} of the Spring Boot module, and it answers with
 * the same status and the same body.
 * <p>
 * Quarkus REST finds the methods by their annotation, because the jar of this module carries a
 * Jandex index. So an application which has this module and Quarkus REST gets the answers without
 * any line of its own. On Spring Boot a workflow module imports the handler itself. Quarkus has no
 * component scan which an application could aim at a library, so registering on its own is the
 * only way which needs no code in every workflow module.
 */
public class BcUserTaskExceptionMapper {

  /**
   * Creates the mapper. Quarkus calls this constructor.
   */
  public BcUserTaskExceptionMapper() {
  }

  /**
   * Answers a task id which does not belong to the workflow, or belongs to a task of another kind,
   * with {@code 404 Not Found}.
   *
   * @param exception What the workflow module found
   * @return The answer, with the message as the body
   */
  @ServerExceptionMapper
  public RestResponse<String> unknownTask(
      final BcUnknownTaskException exception) {

    return plainText(RestResponse.Status.NOT_FOUND, exception);

  }

  /**
   * Answers a task which was completed or canceled before, for example by a form sent twice, with
   * {@code 409 Conflict}.
   *
   * @param exception What the workflow module found
   * @return The answer, with the message as the body
   */
  @ServerExceptionMapper
  public RestResponse<String> taskNotOpen(
      final BcTaskNotOpenException exception) {

    return plainText(RestResponse.Status.CONFLICT, exception);

  }

  private static RestResponse<String> plainText(
      final RestResponse.Status status,
      final RuntimeException exception) {

    return RestResponse.ResponseBuilder
        .create(status, exception.getMessage())
        .type(MediaType.TEXT_PLAIN_TYPE)
        .build();

  }

}

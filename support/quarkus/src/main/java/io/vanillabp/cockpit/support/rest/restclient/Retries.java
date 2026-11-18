package io.vanillabp.cockpit.support.rest.restclient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Calls a method of a client again when its request got no answer: a refused connection, a read
 * timeout. This is what the Spring Boot module and the REST client of version 1 retry. An
 * answer with an error status is not retried, because the server did see the request.
 * <p>
 * The one exception is a {@code 401} of a client with OAuth: {@link ClientCredentials} has dropped
 * the refused token by then, so the next attempt fetches a new one. A failed token request is not
 * retried, because the next attempt would get the same answer.
 * <p>
 * The pauses are those of Feign's {@code Retryer.Default}: {@code retry.period} first, then one and
 * a half times longer each time, at most {@code retry.max-period}.
 * <p>
 * Quarkus' REST client has no retry of its own, and the annotation {@code @Retry} of MicroProfile
 * Fault Tolerance sits on the methods of a bean, while the methods of a generated interface carry
 * no annotation of ours. So the client is wrapped. Only a method which waits for its answer is
 * retried. A method which returns a {@code Uni} or a {@link CompletionStage} is called once.
 */
final class Retries implements InvocationHandler {

  private final Object client;

  private final RestClientKeys.Retry settings;

  private final boolean retryUnauthorized;

  private Retries(
      final Object client,
      final RestClientKeys.Retry settings,
      final boolean retryUnauthorized) {

    this.client = client;
    this.settings = settings;
    this.retryUnauthorized = retryUnauthorized;

  }

  /**
   * Wraps a client, so that its methods retry.
   *
   * @param api               the interface of the client
   * @param client            the client Quarkus built
   * @param settings          the keys {@code retry.*}
   * @param retryUnauthorized whether a {@code 401} is retried, for clients with OAuth
   * @return the client which retries
   */
  static <T> T around(
      final Class<T> api,
      final T client,
      final RestClientKeys.Retry settings,
      final boolean retryUnauthorized) {

    return api.cast(Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[]{
        api
    }, new Retries(client, settings, retryUnauthorized)));

  }

  @Override
  public Object invoke(
      final Object proxy,
      final Method method,
      final Object[] args) throws Throwable {

    if (method.getDeclaringClass() == Object.class) {
      return switch (method.getName()) {
        case "equals" -> proxy == args[0];
        case "hashCode" -> System.identityHashCode(proxy);
        case "toString" -> "retrying "
            + client;
        default -> callOnce(method, args);
      };
    }
    if (!waitsForItsAnswer(method)) {
      return callOnce(method, args);
    }

    var pause = settings.period();
    for (var attempt = 1;; ++attempt) {
      try {
        return method.invoke(client, args);
      } catch (InvocationTargetException e) {
        final var failure = e.getCause();
        if ((attempt >= settings.maxAttempts()) || !isWorthAnotherAttempt(failure)) {
          throw failure;
        }
      }
      Thread.sleep(pause.toMillis());
      final var longer = Duration.ofNanos((long) (pause.toNanos() * 1.5));
      pause = longer.compareTo(settings.maxPeriod()) > 0 ? settings.maxPeriod() : longer;
    }

  }

  private Object callOnce(
      final Method method,
      final Object[] args) throws Throwable {

    try {
      return method.invoke(client, args);
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }

  }

  private static boolean waitsForItsAnswer(
      final Method method) {

    final var type = method.getReturnType();
    return !CompletionStage.class.isAssignableFrom(type) && !"io.smallrye.mutiny.Uni"
        .equals(type.getName()) && !"io.smallrye.mutiny.Multi".equals(type.getName());

  }

  private boolean isWorthAnotherAttempt(
      final Throwable failure) {

    if (failure instanceof WebApplicationException answer) {
      return retryUnauthorized && (answer.getResponse() != null) && (answer.getResponse()
          .getStatus() == Response.Status.UNAUTHORIZED.getStatusCode());
    }
    if (!(failure instanceof ProcessingException)) {
      return false;
    }
    for (var cause = failure; cause != null; cause = cause.getCause() == cause ? null : cause.getCause()) {
      if (cause instanceof TokenRequestFailed) {
        return false;
      }
    }
    return true;

  }

}

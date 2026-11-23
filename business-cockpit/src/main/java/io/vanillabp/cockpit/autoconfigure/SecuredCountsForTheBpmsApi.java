package io.vanillabp.cockpit.autoconfigure;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.MethodMatchers;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authorization.method.AuthorizationAdvisor;

/**
 * Ends the start if {@code @Secured} does not count for the controllers of the BPMS API.
 * <p>
 * The controllers keep their endpoints to the role {@code BPMS-API} with {@code @Secured}. Without a
 * chain for the BPMS API, its paths fall to the GUI chain, which lets every user who is logged in
 * through, and then {@code @Secured} is the only thing which keeps them out. An application which
 * sets {@code business-cockpit.method-security.enabled: false} and writes a plain
 * {@code @EnableMethodSecurity} switches {@code @Secured} off without noticing.
 * <p>
 * Whether method security is on carries no bean name Spring Security publishes. So the check looks
 * at the controllers themselves once the application is ready: an endpoint counts as protected if an
 * advisor of Spring Security's method security applies to it. It calls no endpoint, because a call
 * which is not refused would run the endpoint.
 */
public class SecuredCountsForTheBpmsApi implements ApplicationListener<ApplicationReadyEvent> {

  private final List<Class<?>> controllerTypes;

  /**
   * Creates the check.
   *
   * @param controllerTypes the controllers which carry {@code @Secured}
   */
  public SecuredCountsForTheBpmsApi(
      final List<Class<?>> controllerTypes) {

    this.controllerTypes = List.copyOf(controllerTypes);

  }

  @Override
  public void onApplicationEvent(
      final ApplicationReadyEvent event) {

    for (final var type : controllerTypes) {
      event
          .getApplicationContext()
          .getBeanProvider(type)
          .ifAvailable(controller -> {
            if (!isProtected(controller, type)) {
              throw new IllegalStateException("""
                  @Secured does not count for '%s', so the BPMS API answers every user who is \
                  logged in, not only a caller with the role 'BPMS-API'. The application set \
                  'business-cockpit.method-security.enabled: false' and switched method security on \
                  without @Secured. Write this on a configuration class of the application:

                    @EnableMethodSecurity(securedEnabled = true)

                  or remove 'business-cockpit.method-security.enabled: false'."""
                  .formatted(type.getName()));
            }
          });
    }

  }

  /**
   * Whether an advisor of Spring Security's method security applies to an endpoint of the
   * controller.
   *
   * @param controller the bean, a proxy if method security applies to it
   * @param type       the class of the controller
   * @return whether the endpoint refuses a call without the role
   */
  static boolean isProtected(
      final Object controller,
      final Class<?> type) {

    if (!(controller instanceof Advised advised)) {
      return false;
    }
    final var targetClass = AopUtils.getTargetClass(controller);
    final var endpoint = anEndpointOf(type);
    if (endpoint.isEmpty()) {
      return true;
    }
    return Arrays
        .stream(advised.getAdvisors())
        .filter(advisor -> (advisor instanceof AuthorizationAdvisor) || (advisor
            .getAdvice() instanceof AuthorizationAdvisor))
        .filter(PointcutAdvisor.class::isInstance)
        .map(PointcutAdvisor.class::cast)
        .anyMatch(advisor -> advisor.getPointcut().getClassFilter().matches(targetClass) && MethodMatchers
            .matches(advisor.getPointcut().getMethodMatcher(), endpoint.get(), targetClass, false));

  }

  private static Optional<Method> anEndpointOf(
      final Class<?> type) {

    return Arrays
        .stream(type.getDeclaredMethods())
        .filter(method -> Modifier.isPublic(method.getModifiers()) && !method.isSynthetic())
        .findFirst();

  }

}

package io.vanillabp.cockpit.gui.api.v1.client.autoconfigure;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

/**
 * Says which key is missing if a bean needs a client of the official GUI API and there is none.
 * The clients are there only if {@value OfficialGuiApiClientAutoConfiguration#ADDRESS} is set, see
 * {@link OfficialGuiApiClientAutoConfiguration}. Without this analyzer the developer reads only
 * that no bean of the interface exists.
 * <p>
 * It runs before Spring Boot's own analyzer of a missing bean, and it answers only for an
 * interface of this API while the address is not set. Every other missing bean is left to Spring
 * Boot. Registered in {@code META-INF/spring.factories}, which is where Spring Boot looks for
 * failure analyzers.
 */
public class TheOfficialGuiApiHasNoAddress extends AbstractFailureAnalyzer<NoSuchBeanDefinitionException> implements Ordered {

  private final Environment environment;

  /**
   * Creates the analyzer. Spring Boot creates it from {@code META-INF/spring.factories}.
   *
   * @param environment where the address is looked up
   */
  public TheOfficialGuiApiHasNoAddress(
      final Environment environment) {
    this.environment = environment;
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override
  protected @Nullable FailureAnalysis analyze(
      final Throwable rootFailure,
      final NoSuchBeanDefinitionException cause) {

    final var missingType = cause.getBeanType();
    if ((missingType == null) || !OfficialGuiApiClientAutoConfiguration.APIS.contains(missingType)) {
      return null;
    }
    if (environment.containsProperty(OfficialGuiApiClientAutoConfiguration.ADDRESS)) {
      return null;
    }

    return new FailureAnalysis("""
        A bean needs the client %s of the cockpit's official GUI API, but there is none, because \
        '%s' is not set."""
        .formatted(missingType.getSimpleName(), OfficialGuiApiClientAutoConfiguration.ADDRESS), """
            Set '%s' to the address of the cockpit followed by '/gui/api/v1', for example \
            'https://cockpit.example.com/gui/api/v1'. If the bean calls the cockpit only when the \
            application sets the address, inject an ObjectProvider of %s instead. To read the keys \
            below a prefix of your own, import the interfaces into an HTTP service group of your \
            own, see the README of official-gui-api-spring-boot-client."""
            .formatted(OfficialGuiApiClientAutoConfiguration.ADDRESS, missingType.getSimpleName()), cause);

  }

}

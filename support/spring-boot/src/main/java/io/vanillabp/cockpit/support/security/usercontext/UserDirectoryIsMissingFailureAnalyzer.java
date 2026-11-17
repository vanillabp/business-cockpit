package io.vanillabp.cockpit.support.security.usercontext;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Says what to do when a bean needs a {@link UserDirectoryService} and the application has none.
 * <p>
 * The login token of the Business Cockpit names the user but carries no groups. So whatever reads
 * the token, the cockpit or a workflow module behind it, asks a directory for the groups of the
 * user. Without this analyzer Spring Boot only reports that a bean of that type is missing, which
 * does not tell a developer where the directory should come from. A workflow module meets this
 * first, because the cockpit's own directory is not shared with it.
 * <p>
 * Registered in {@code META-INF/spring.factories}, which is where Spring Boot looks for failure
 * analyzers. It goes first, so it is asked before Spring Boot's general analyzer for a missing
 * bean. It answers nothing for any other type, so going first changes nothing else.
 * <p>
 * The words are those of {@link UserDirectoryIsMissing}, which a Quarkus workflow module reads at
 * its start as well.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserDirectoryIsMissingFailureAnalyzer extends AbstractFailureAnalyzer<NoSuchBeanDefinitionException> {

  /**
   * Creates the analyzer. Spring Boot creates it from {@code META-INF/spring.factories}.
   */
  public UserDirectoryIsMissingFailureAnalyzer() {
  }

  @Override
  protected FailureAnalysis analyze(
      final Throwable rootFailure,
      final NoSuchBeanDefinitionException cause) {

    if (!UserDirectoryService.class.equals(cause.getBeanType())) {
      return null;
    }

    final var dependentBean = findCause(rootFailure, UnsatisfiedDependencyException.class);
    final var whoNeedsIt = (dependentBean == null) || (dependentBean.getBeanName() == null)
        ? "A bean of the application needs one."
        : "The bean '"
            + dependentBean.getBeanName()
            + "' needs one.";

    return new FailureAnalysis(
        UserDirectoryIsMissing.whatIsWrong(whoNeedsIt), UserDirectoryIsMissing.whatToDo("""
            @Bean
            public UserDirectoryService userDirectory() {
                return ...;
            }
            """), cause);

  }

}

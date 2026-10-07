package io.vanillabp.cockpit.config.startup;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Turns an unusable lifetime of the login token into Spring Boot's "APPLICATION FAILED TO START"
 * report, the same way {@link CockpitIsNotConfiguredFailureAnalyzer} does it for a missing value.
 * <p>
 * Registered in {@code META-INF/spring.factories}, which is where Spring Boot looks for failure
 * analyzers.
 */
public class JwtLifetimeIsNotUsableFailureAnalyzer
        extends AbstractFailureAnalyzer<JwtLifetimeIsNotUsableException> {

    @Override
    protected FailureAnalysis analyze(
            final Throwable rootFailure,
            final JwtLifetimeIsNotUsableException cause) {

        return new FailureAnalysis(
                cause.describeWhatIsWrong(),
                cause.describeWhatToDo(),
                cause);

    }

}

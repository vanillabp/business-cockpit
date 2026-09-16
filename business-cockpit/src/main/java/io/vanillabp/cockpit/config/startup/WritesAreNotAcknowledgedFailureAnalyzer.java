package io.vanillabp.cockpit.config.startup;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Turns an unusable write concern into Spring Boot's "APPLICATION FAILED TO START" report, the same
 * way {@link CockpitIsNotConfiguredFailureAnalyzer} does it for a missing value. The first thing a
 * developer sees is what the cockpit would have written with and which property to set, and not the
 * bean which happened to be built when it was noticed.
 * <p>
 * Registered in {@code META-INF/spring.factories}, which is where Spring Boot looks for failure
 * analyzers.
 */
public class WritesAreNotAcknowledgedFailureAnalyzer
        extends AbstractFailureAnalyzer<WritesAreNotAcknowledgedException> {

    @Override
    protected FailureAnalysis analyze(
            final Throwable rootFailure,
            final WritesAreNotAcknowledgedException cause) {

        return new FailureAnalysis(
                cause.describeWhatIsWrong(),
                cause.describeWhatToDo(),
                cause);

    }

}

package io.vanillabp.cockpit.config.startup;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Turns an unusable limit of indexes for sorting into Spring Boot's "APPLICATION FAILED TO START"
 * report, the same way {@link CockpitIsNotConfiguredFailureAnalyzer} does it for a missing value.
 * <p>
 * Registered in {@code META-INF/spring.factories}, which is where Spring Boot looks for failure
 * analyzers.
 */
public class SortIndexLimitIsNotUsableFailureAnalyzer
        extends AbstractFailureAnalyzer<SortIndexLimitIsNotUsableException> {

    @Override
    protected FailureAnalysis analyze(
            final Throwable rootFailure,
            final SortIndexLimitIsNotUsableException cause) {

        return new FailureAnalysis(
                cause.describeWhatIsWrong(),
                cause.describeWhatToDo(),
                cause);

    }

}

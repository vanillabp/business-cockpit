package io.vanillabp.cockpit.config.startup;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Turns the incomplete configuration into Spring Boot's "APPLICATION FAILED TO START" report. The
 * point of the whole exercise: the first thing a developer sees is the list of property names to
 * add, not a stack trace of the bean that happened to need one of them first.
 * <p>
 * Registered in {@code META-INF/spring.factories}, which is where Spring Boot looks for failure
 * analyzers.
 */
public class CockpitIsNotConfiguredFailureAnalyzer
        extends AbstractFailureAnalyzer<CockpitIsNotConfiguredException> {

    @Override
    protected FailureAnalysis analyze(
            final Throwable rootFailure,
            final CockpitIsNotConfiguredException cause) {

        return new FailureAnalysis(
                cause.describeWhatIsMissing(),
                cause.describeWhatToDo(),
                cause);

    }

}

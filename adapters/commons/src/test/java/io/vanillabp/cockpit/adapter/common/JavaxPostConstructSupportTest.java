package io.vanillabp.cockpit.adapter.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documents whether Spring still honours {@code javax.annotation.PostConstruct} - the pre-Jakarta
 * annotation - as used in three production classes of this module:
 *
 * <ul>
 * <li>{@code io.vanillabp.cockpit.adapter.common.usertask.rest.UserTaskRestPublishing:37}</li>
 * <li>{@code io.vanillabp.cockpit.adapter.common.workflow.rest.WorkflowRestPublishing:31}</li>
 * <li>{@code io.vanillabp.cockpit.adapter.common.workflowmodule.rest.WorkflowModuleRestPublishing:36}</li>
 * </ul>
 *
 * <p><b>Measured, and the prediction held.</b> Two data points:
 * <ul>
 * <li><b>Spring Framework 6</b> (Spring Boot 3.0 and 3.5) honoured the {@code javax} variant: its
 * {@code CommonAnnotationBeanPostProcessor} registered {@code jakarta.annotation.PostConstruct} and,
 * when present on the classpath, additionally the legacy {@code javax.annotation.PostConstruct}.</li>
 * <li><b>Spring Framework 7</b> (Spring Boot 4.1) no longer does. This test failed on the Boot 4.1 bump
 * in T15 and was updated to the new measurement - which is precisely what it exists for.</li>
 * </ul>
 *
 * <p>No production impact: the three methods listed above carried a {@code jakarta} {@code @PostConstruct}
 * all along, with the fully qualified {@code javax} one stacked redundantly on top. T14 removed the
 * redundant line. Had they relied on the legacy fallback, they would have silently stopped running here -
 * no error at startup, just missing initialisation.
 *
 * <p>Kept as a record of when the support disappeared, and as a guard should anyone add a
 * {@code javax.annotation} lifecycle annotation again.
 */
class JavaxPostConstructSupportTest {

    static final AtomicBoolean JAVAX_CALLED = new AtomicBoolean();

    static final AtomicBoolean JAKARTA_CALLED = new AtomicBoolean();

    public static class JavaxAnnotated {

        @javax.annotation.PostConstruct
        public void init() {
            JAVAX_CALLED.set(true);
        }

    }

    public static class JakartaAnnotated {

        @jakarta.annotation.PostConstruct
        public void init() {
            JAKARTA_CALLED.set(true);
        }

    }

    @Configuration
    static class TestConfiguration {

        @Bean
        JavaxAnnotated javaxAnnotated() {
            return new JavaxAnnotated();
        }

        @Bean
        JakartaAnnotated jakartaAnnotated() {
            return new JakartaAnnotated();
        }

    }

    @Test
    void jakartaPostConstructIsInvokedAndTheLegacyJavaxOneIsNot() {

        JAVAX_CALLED.set(false);
        JAKARTA_CALLED.set(false);

        try (final var context = new AnnotationConfigApplicationContext(TestConfiguration.class)) {

            context.getBean(JavaxAnnotated.class);
            context.getBean(JakartaAnnotated.class);

            assertThat(JAKARTA_CALLED)
                    .as("jakarta.annotation.PostConstruct must be honoured")
                    .isTrue();

            assertThat(JAVAX_CALLED)
                    .as("Spring Framework 7 dropped the legacy javax.annotation.PostConstruct support. "
                            + "Should this become true again, the support is back - unexpected, and worth "
                            + "understanding before relying on it")
                    .isFalse();

        }

    }

}

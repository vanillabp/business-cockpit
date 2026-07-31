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
 * <p><b>Measured result:</b> Spring Framework 6 still honours the {@code javax} variant. Its
 * {@code CommonAnnotationBeanPostProcessor} registers {@code jakarta.annotation.PostConstruct} and, when
 * present on the classpath, additionally the legacy {@code javax.annotation.PostConstruct}. So the three
 * production methods above <em>do</em> run today - there is no latent defect.
 *
 * <p>That legacy fallback is exactly what may disappear in Spring Framework 7. If it does, those three
 * methods stop being called - silently, with no error at startup. This test is the tripwire: should the
 * {@code javax} assertion below start failing after the migration, T14's replacement of the annotations
 * with the {@code jakarta} variant is not cosmetic but mandatory.
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
    void bothJakartaAndLegacyJavaxPostConstructAreInvoked() {

        JAVAX_CALLED.set(false);
        JAKARTA_CALLED.set(false);

        try (final var context = new AnnotationConfigApplicationContext(TestConfiguration.class)) {

            context.getBean(JavaxAnnotated.class);
            context.getBean(JakartaAnnotated.class);

            assertThat(JAKARTA_CALLED)
                    .as("jakarta.annotation.PostConstruct must be honoured")
                    .isTrue();

            assertThat(JAVAX_CALLED)
                    .as("documented current behaviour: Spring Framework 6 still honours the legacy "
                            + "javax.annotation.PostConstruct. If this fails after the migration, the "
                            + "three *RestPublishing initialisation methods have stopped running - "
                            + "switch them to jakarta.annotation.PostConstruct (T14)")
                    .isTrue();

        }

    }

}

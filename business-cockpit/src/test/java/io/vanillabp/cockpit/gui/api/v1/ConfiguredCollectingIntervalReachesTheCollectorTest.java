package io.vanillabp.cockpit.gui.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.vanillabp.cockpit.commons.security.usercontext.UserContext;
import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import io.vanillabp.cockpit.users.model.PersonAndGroupApiMapper;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;

/**
 * The collecting interval of the update stream, as the scheduler reads it off the annotation.
 *
 * <p>This test exists because the interval was configurable in name only. The placeholder was
 * written in camel case and named no key the configuration carries, so the default of 250
 * milliseconds counted whatever anybody wrote. Nothing failed. The collector just ran at its own
 * pace.
 *
 * <p>What is asserted is the task the scheduler registered, not a number of ticks within a second,
 * so the test says the same thing on a loaded machine as on an idle one. The context holds the
 * controller and a double for everything it is wired to. Its scheduler is a double as well, which
 * is why no task ever runs here.
 */
@ExtendWith(SuppressOutputExtension.class)
class ConfiguredCollectingIntervalReachesTheCollectorTest {

    /**
     * Any value which is not the default and not a round number, so it cannot pass by accident.
     */
    private static final int A_CONFIGURED_INTERVAL = 777;

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SchedulingAsTheApplicationHasIt.class);

    @Test
    @DisplayName("A configured collecting interval is the interval the collector is scheduled at")
    void configuredIntervalIsUsed() {

        contextRunner
                .withPropertyValues(
                        "business-cockpit.gui-sse.collecting-interval=" + A_CONFIGURED_INTERVAL)
                .run(context -> assertThat(collectingIntervalOf(context))
                        .isEqualTo(Duration.ofMillis(A_CONFIGURED_INTERVAL)));

    }

    /**
     * The default lives twice, in the annotation and in {@link GuiSseProperties}. Whoever changes
     * one of them is sent here by this test.
     */
    @Test
    @DisplayName("Without configuration the collector runs at the default of GuiSseProperties")
    void defaultOfThePropertiesIsUsed() {

        contextRunner
                .run(context -> assertThat(collectingIntervalOf(context))
                        .isEqualTo(Duration.ofMillis(
                                new GuiSseProperties().getCollectingInterval())));

    }

    /**
     * The interval of the one scheduled task which runs {@code LoginApiController.updateClients}.
     * The controller schedules a second task, the ping, which is why the task is looked up by its
     * method and not by being the only one.
     */
    private Duration collectingIntervalOf(
            final ApplicationContext context) throws Exception {

        final var collector = LoginApiController.class.getName()
                + "." + LoginApiController.class.getMethod("updateClients").getName();

        return context
                .getBeansOfType(ScheduledTaskHolder.class)
                .values()
                .stream()
                .flatMap(holder -> holder.getScheduledTasks().stream())
                .map(ScheduledTask::getTask)
                .filter(task -> collector.equals(task.toString()))
                .filter(IntervalTask.class::isInstance)
                .map(IntervalTask.class::cast)
                .map(IntervalTask::getIntervalDuration)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No scheduled task found running '" + collector + "'"));

    }

    /**
     * What the application does to make {@code @Scheduled} count, reduced to the one controller
     * under test. Everything the controller is wired to is a double; none of it is reached,
     * because the scheduler never runs a task.
     */
    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    static class SchedulingAsTheApplicationHasIt {

        @Bean
        LoginApiController loginApiController() {
            return new LoginApiController();
        }

        @Bean
        Logger logger() {
            return mock(Logger.class);
        }

        @Bean
        ApplicationProperties applicationProperties() {
            return new ApplicationProperties();
        }

        @Bean
        UserContext userContext() {
            return mock(UserContext.class);
        }

        @Bean
        TaskScheduler taskScheduler() {
            return mock(TaskScheduler.class);
        }

        @Bean
        PersonAndGroupApiMapper personAndGroupApiMapper() {
            return mock(PersonAndGroupApiMapper.class);
        }

    }

}

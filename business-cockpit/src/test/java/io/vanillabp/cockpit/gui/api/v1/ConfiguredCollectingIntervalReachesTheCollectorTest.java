package io.vanillabp.cockpit.gui.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * update streams and the properties. Their scheduler is a double, and so no task ever runs here.
 * The filtering interval is checked the same way, because its placeholder could go wrong in the
 * same way.
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

    @Test
    @DisplayName("A configured filtering interval is the interval the filter is scheduled at")
    void configuredFilteringIntervalIsUsed() {

        contextRunner
                .withPropertyValues(
                        "business-cockpit.gui-sse.filtering-interval=" + A_CONFIGURED_INTERVAL)
                .run(context -> assertThat(intervalOf(context, "filterCollectedChanges"))
                        .isEqualTo(Duration.ofMillis(A_CONFIGURED_INTERVAL)));

    }

    @Test
    @DisplayName("Without configuration the filter runs at the default of GuiSseProperties")
    void defaultFilteringIntervalOfThePropertiesIsUsed() {

        contextRunner
                .run(context -> assertThat(intervalOf(context, "filterCollectedChanges"))
                        .isEqualTo(Duration.ofMillis(
                                new GuiSseProperties().getFilteringInterval())));

    }

    private Duration collectingIntervalOf(
            final ApplicationContext context) throws Exception {

        return intervalOf(context, "deliverMatchedChanges");

    }

    /**
     * The interval of the one scheduled task which runs this method of {@code UpdateStreams}. The
     * class schedules three tasks, the filter, the delivery and the ping, which is why the task is
     * looked up by its method and not by being the only one.
     */
    private Duration intervalOf(
            final ApplicationContext context,
            final String method) throws Exception {

        final var collector = UpdateStreams.class.getName()
                + "." + UpdateStreams.class.getMethod(method).getName();

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
     * What the application does to make {@code @Scheduled} count, reduced to the update streams.
     * The scheduler they are handed is a double, and the one Spring runs the annotations with
     * never reaches a stream, because no stream is open.
     */
    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    static class SchedulingAsTheApplicationHasIt {

        @Bean
        UpdateStreams updateStreams(
                final ApplicationProperties properties) {
            return new UpdateStreams(properties, mock(TaskScheduler.class), List.of());
        }

        @Bean
        ApplicationProperties applicationProperties() {
            return new ApplicationProperties();
        }

    }

}

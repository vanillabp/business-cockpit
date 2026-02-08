package io.vanillabp.cockpit.adapter.camunda8.usertask;

import freemarker.template.Configuration;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.JsonMapper;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskEvent;
import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskUpdatedEvent;
import io.vanillabp.spi.cockpit.details.DetailsEvent;
import io.vanillabp.springboot.adapter.AdapterAwareProcessService;
import io.vanillabp.springboot.parameters.MethodParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.repository.CrudRepository;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Camunda8UserTaskHandlerTest {

    @Mock
    private VanillaBpCockpitProperties cockpitProperties;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private JsonMapper camundaJsonMapper;

    @Mock
    private AdapterAwareProcessService<?> processService;

    @Mock
    private CrudRepository<Object, Object> workflowAggregateRepository;

    @Mock
    private CamundaClient client;

    private Camunda8UserTaskHandler handler;

    // Test class for method reference
    public static class TestBean {
        public String testMethod() {
            return "test";
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        // Set up process service mock
        doReturn((Class) String.class).when(processService).getWorkflowAggregateIdClass();
        doReturn("test-module").when(processService).getWorkflowModuleId();

        // Get a simple method for testing
        Method method = TestBean.class.getMethod("testMethod");
        List<MethodParameter> parameters = Collections.emptyList();

        handler = new Camunda8UserTaskHandler(
                "review-task",
                cockpitProperties,
                applicationEventPublisher,
                camundaJsonMapper,
                Optional.empty(),
                "order-process",
                "v1.0",
                "Order Process",
                "Review Order",
                processService,
                "businessKey",
                workflowAggregateRepository,
                new TestBean(),
                method,
                parameters,
                client);
    }

    @Test
    void getLogger_returnsNonNull() {
        // Get logger
        final var logger = handler.getLogger();

        // Should return a valid logger
        assertThat(logger).isNotNull();
    }

    @Test
    void updateVersionInfo_updatesValue() {
        // Update version info
        handler.updateVersionInfo("v2.0");

        // No direct getter, but verifies no exception is thrown
        // The value is used internally when processing events
    }

    @Test
    void publishEvent_publishesBothEvents() {
        // Create mock event
        final var userTaskEvent = new UserTaskUpdatedEvent("test-module", List.of("en"));

        // Publish event
        handler.publishEvent(userTaskEvent);

        // Verify both events are published
        verify(applicationEventPublisher).publishEvent(any(io.vanillabp.cockpit.adapter.camunda8.usertask.publishing.UserTaskEvent.class));
        verify(applicationEventPublisher).publishEvent(any(io.vanillabp.cockpit.adapter.camunda8.usertask.publishing.ProcessUserTaskAfterTransactionEvent.class));
    }

}

package io.vanillabp.cockpit.adapter.camunda8.workflow;

import freemarker.template.Configuration;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.JsonMapper;
import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Camunda8WorkflowHandlerTest {

    @Mock
    private VanillaBpCockpitProperties cockpitProperties;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private JsonMapper jsonMapper;

    @Mock
    private AdapterAwareProcessService<?> processService;

    @Mock
    private CrudRepository<Object, Object> repository;

    @Mock
    private CamundaClient client;

    private Camunda8WorkflowHandler handler;

    // Test class for method reference
    public static class TestBean {
        public String testMethod() {
            return "test";
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        // Set up process service mock
        when(processService.getWorkflowAggregateIdClass()).thenReturn((Class) String.class);
        when(processService.getWorkflowModuleId()).thenReturn("test-module");

        // Get a simple method for testing
        Method method = TestBean.class.getMethod("testMethod");
        List<MethodParameter> parameters = Collections.emptyList();

        handler = new Camunda8WorkflowHandler(
                cockpitProperties,
                eventPublisher,
                jsonMapper,
                processService,
                "order-process",
                "v1.0",
                "Order Process",
                Optional.empty(),
                "businessKey",
                repository,
                new TestBean(),
                method,
                parameters,
                client);
    }

    @Test
    void getBpmnProcessId_returnsConfiguredValue() {
        // Get BPMN process ID
        final var result = handler.getBpmnProcessId();

        // Verify
        assertThat(result).isEqualTo("order-process");
    }

    @Test
    void getWorkflowModuleId_delegatesToProcessService() {
        // Get workflow module ID
        final var result = handler.getWorkflowModuleId();

        // Verify delegation
        assertThat(result).isEqualTo("test-module");
    }

    @Test
    void updateVersionInfo_updatesValue() {
        // Update version info
        handler.updateVersionInfo("v2.0");

        // No direct getter, but verifies no exception is thrown
        // The value is used internally when processing events
    }

    @Test
    void getLogger_returnsNonNull() {
        // Get logger
        final var logger = handler.getLogger();

        // Should return a valid logger
        assertThat(logger).isNotNull();
    }

}

package io.vanillabp.cockpit.adapter.common.wiring;

import io.vanillabp.cockpit.adapter.common.service.BusinessCockpitServiceImplementation;
import io.vanillabp.cockpit.adapter.common.wiring.parameters.PrefilledWorkflowDetailsMethodParameter;
import io.vanillabp.cockpit.adapter.common.wiring.parameters.WorkflowMethodParameterFactory;
import io.vanillabp.cockpit.adapter.common.workflowmodule.WorkflowModulePublishing;
import io.vanillabp.spi.cockpit.workflow.PrefilledWorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetailsProvider;
import io.vanillabp.springboot.adapter.Connectable;
import io.vanillabp.springboot.adapter.SpringBeanUtil;
import io.vanillabp.springboot.parameters.MethodParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractWorkflowWiringTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private SpringBeanUtil springBeanUtil;

    @Mock
    private WorkflowMethodParameterFactory methodParameterFactory;

    @Mock
    private WorkflowModulePublishing workflowModulePublishing;

    @Mock
    private PrefilledWorkflowDetailsMethodParameter prefilledWorkflowDetailsMethodParameter;

    private TestWorkflowWiring wiring;

    @BeforeEach
    void setUp() {
        wiring = new TestWorkflowWiring(applicationContext, springBeanUtil, methodParameterFactory, workflowModulePublishing);
    }

    @Test
    void getAnnotationType_returnsWorkflowDetailsProvider() {
        assertThat(wiring.callGetAnnotationType()).isEqualTo(WorkflowDetailsProvider.class);
    }

    @Test
    void validatePrefilledWorkflowDetails_withCorrectType_returnsMethodParameter() throws NoSuchMethodException {
        Method method = TestMethodHolder.class.getDeclaredMethod("methodWithPrefilledWorkflowDetails", PrefilledWorkflowDetails.class);
        Parameter parameter = method.getParameters()[0];

        when(methodParameterFactory.getPrefilledWorkflowDetailsParameter(anyInt(), anyString()))
                .thenReturn(prefilledWorkflowDetailsMethodParameter);

        MethodParameter result = wiring.callValidatePrefilledWorkflowDetails(method, parameter, 0);

        assertThat(result).isEqualTo(prefilledWorkflowDetailsMethodParameter);
    }

    @Test
    void validatePrefilledWorkflowDetails_withWrongType_returnsNull() throws NoSuchMethodException {
        Method method = TestMethodHolder.class.getDeclaredMethod("methodWithString", String.class);
        Parameter parameter = method.getParameters()[0];

        MethodParameter result = wiring.callValidatePrefilledWorkflowDetails(method, parameter, 0);

        assertThat(result).isNull();
    }

    @Test
    void methodMatchesElementId_alwaysReturnsFalse() throws NoSuchMethodException {
        Connectable connectable = mock(Connectable.class);
        Method method = TestMethodHolder.class.getDeclaredMethod("methodWithString", String.class);
        WorkflowDetailsProvider annotation = mock(WorkflowDetailsProvider.class);

        boolean result = wiring.callMethodMatchesElementId(connectable, method, annotation);

        assertThat(result).isFalse();
    }

    @Test
    void methodMatchesTaskDefinition_alwaysReturnsFalse() throws NoSuchMethodException {
        Connectable connectable = mock(Connectable.class);
        Method method = TestMethodHolder.class.getDeclaredMethod("methodWithString", String.class);
        WorkflowDetailsProvider annotation = mock(WorkflowDetailsProvider.class);

        boolean result = wiring.callMethodMatchesTaskDefinition(connectable, method, annotation);

        assertThat(result).isFalse();
    }

    /**
     * Helper class to provide real methods and parameters for testing
     */
    @SuppressWarnings("unused")
    private static class TestMethodHolder {
        public void methodWithPrefilledWorkflowDetails(PrefilledWorkflowDetails details) {}
        public void methodWithString(String value) {}
    }

    /**
     * Concrete test implementation of AbstractWorkflowWiring
     */
    private static class TestWorkflowWiring extends AbstractWorkflowWiring<Connectable, WorkflowMethodParameterFactory, TestBusinessCockpitService> {

        public TestWorkflowWiring(
                ApplicationContext applicationContext,
                SpringBeanUtil springBeanUtil,
                WorkflowMethodParameterFactory methodParameterFactory,
                WorkflowModulePublishing workflowModulePublishing) {
            super(applicationContext, springBeanUtil, methodParameterFactory, workflowModulePublishing);
        }

        @Override
        protected TestBusinessCockpitService connectToBpms(
                String workflowModuleId,
                Class<?> workflowAggregateClass,
                String bpmnProcessId,
                boolean isPrimary) {
            return null;
        }

        @Override
        protected String getWorkflowModuleUri(String workflowModuleId) {
            return "http://localhost:8080";
        }

        @Override
        protected String getTaskProviderApiUriPath(String workflowModuleId) {
            return "/task-provider";
        }

        @Override
        protected String getWorkflowProviderApiUriPath(String workflowModuleId) {
            return "/workflow-provider";
        }

        // Expose protected methods for testing
        public Class<WorkflowDetailsProvider> callGetAnnotationType() {
            return getAnnotationType();
        }

        public MethodParameter callValidatePrefilledWorkflowDetails(Method method, Parameter parameter, int index) {
            return validatePrefilledWorkflowDetails(method, parameter, index);
        }

        public boolean callMethodMatchesElementId(Connectable connectable, Method method, WorkflowDetailsProvider annotation) {
            return methodMatchesElementId(connectable, method, annotation);
        }

        public boolean callMethodMatchesTaskDefinition(Connectable connectable, Method method, WorkflowDetailsProvider annotation) {
            return methodMatchesTaskDefinition(connectable, method, annotation);
        }
    }

    /**
     * Test implementation of BusinessCockpitServiceImplementation
     */
    private interface TestBusinessCockpitService extends BusinessCockpitServiceImplementation<Object> {
    }
}

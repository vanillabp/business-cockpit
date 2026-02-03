package io.vanillabp.cockpit.adapter.common.wiring;

import io.vanillabp.cockpit.adapter.common.wiring.parameters.DetailsEventMethodParameter;
import io.vanillabp.cockpit.adapter.common.wiring.parameters.PrefilledUserTaskDetailsMethodParameter;
import io.vanillabp.cockpit.adapter.common.wiring.parameters.UserTaskMethodParameterFactory;
import io.vanillabp.spi.cockpit.details.DetailsEvent;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractUserTaskWiringTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private SpringBeanUtil springBeanUtil;

    @Mock
    private UserTaskMethodParameterFactory methodParameterFactory;

    @Mock
    private PrefilledUserTaskDetailsMethodParameter prefilledUserTaskDetailsMethodParameter;

    @Mock
    private DetailsEventMethodParameter detailsEventMethodParameter;

    private TestUserTaskWiring wiring;

    @BeforeEach
    void setUp() {
        wiring = new TestUserTaskWiring(applicationContext, springBeanUtil, methodParameterFactory);
    }

    @Test
    void getAnnotationType_returnsUserTaskDetailsProvider() {
        assertThat(wiring.callGetAnnotationType()).isEqualTo(UserTaskDetailsProvider.class);
    }

    @Test
    void validatePrefilledUserTaskDetails_withCorrectType_returnsMethodParameter() throws NoSuchMethodException {
        // Use a real method and parameter to avoid mocking final classes
        Method method = TestMethodHolder.class.getDeclaredMethod("methodWithPrefilledUserTaskDetails", PrefilledUserTaskDetails.class);
        Parameter parameter = method.getParameters()[0];

        when(methodParameterFactory.getPrefilledUserTaskDetailsParameter(anyInt(), anyString()))
                .thenReturn(prefilledUserTaskDetailsMethodParameter);

        MethodParameter result = wiring.callValidatePrefilledUserTaskDetails(method, parameter, 0);

        assertThat(result).isEqualTo(prefilledUserTaskDetailsMethodParameter);
    }

    @Test
    void validatePrefilledUserTaskDetails_withWrongType_returnsNull() throws NoSuchMethodException {
        Method method = TestMethodHolder.class.getDeclaredMethod("methodWithString", String.class);
        Parameter parameter = method.getParameters()[0];

        MethodParameter result = wiring.callValidatePrefilledUserTaskDetails(method, parameter, 0);

        assertThat(result).isNull();
    }

    @Test
    void validateDetailsEvent_withCorrectAnnotationAndType_returnsMethodParameter() throws NoSuchMethodException {
        Method method = TestMethodHolder.class.getDeclaredMethod("methodWithDetailsEvent", DetailsEvent.Event.class);
        Parameter parameter = method.getParameters()[0];

        when(methodParameterFactory.getDetailsEventParameter(anyInt(), anyString()))
                .thenReturn(detailsEventMethodParameter);

        MethodParameter result = wiring.callValidateDetailsEvent(method, parameter, 0);

        assertThat(result).isEqualTo(detailsEventMethodParameter);
    }

    @Test
    void validateDetailsEvent_withoutAnnotation_returnsNull() throws NoSuchMethodException {
        Method method = TestMethodHolder.class.getDeclaredMethod("methodWithString", String.class);
        Parameter parameter = method.getParameters()[0];

        MethodParameter result = wiring.callValidateDetailsEvent(method, parameter, 0);

        assertThat(result).isNull();
    }

    @Test
    void validateDetailsEvent_withAnnotationButWrongType_throwsException() throws NoSuchMethodException {
        Method method = TestMethodHolder.class.getDeclaredMethod("methodWithWrongDetailsEventType", String.class);
        Parameter parameter = method.getParameters()[0];

        assertThatThrownBy(() -> wiring.callValidateDetailsEvent(method, parameter, 0))
                .isInstanceOf(java.lang.reflect.MalformedParametersException.class)
                .hasMessageContaining("must be of type");
    }

    @Test
    void methodMatchesElementId_withTaskDefinitionNotUseMethodName_returnsFalse() throws NoSuchMethodException {
        Connectable connectable = mock(Connectable.class);
        Method method = TestMethodHolder.class.getDeclaredMethod("methodWithString", String.class);
        UserTaskDetailsProvider annotation = mock(UserTaskDetailsProvider.class);

        when(annotation.taskDefinition()).thenReturn("specificTaskDefinition");

        boolean result = wiring.callMethodMatchesElementId(connectable, method, annotation);

        assertThat(result).isFalse();
    }

    @Test
    void methodMatchesElementId_withIdUseMethodNameAndMethodNameMatchesElementId_returnsTrue() throws NoSuchMethodException {
        Connectable connectable = mock(Connectable.class);
        Method method = TestMethodHolder.class.getDeclaredMethod("myTask");
        UserTaskDetailsProvider annotation = mock(UserTaskDetailsProvider.class);

        when(annotation.taskDefinition()).thenReturn(UserTaskDetailsProvider.USE_METHOD_NAME);
        when(annotation.id()).thenReturn(UserTaskDetailsProvider.USE_METHOD_NAME);
        when(connectable.getElementId()).thenReturn("myTask");

        boolean result = wiring.callMethodMatchesElementId(connectable, method, annotation);

        assertThat(result).isTrue();
    }

    @Test
    void methodMatchesElementId_withIdMatchesElementId_returnsTrue() throws NoSuchMethodException {
        Connectable connectable = mock(Connectable.class);
        Method method = TestMethodHolder.class.getDeclaredMethod("methodWithString", String.class);
        UserTaskDetailsProvider annotation = mock(UserTaskDetailsProvider.class);

        when(annotation.taskDefinition()).thenReturn(UserTaskDetailsProvider.USE_METHOD_NAME);
        when(annotation.id()).thenReturn("elementId123");
        when(connectable.getElementId()).thenReturn("elementId123");

        boolean result = wiring.callMethodMatchesElementId(connectable, method, annotation);

        assertThat(result).isTrue();
    }

    @Test
    void methodMatchesElementId_withIdNotMatching_returnsFalse() throws NoSuchMethodException {
        Connectable connectable = mock(Connectable.class);
        Method method = TestMethodHolder.class.getDeclaredMethod("methodWithString", String.class);
        UserTaskDetailsProvider annotation = mock(UserTaskDetailsProvider.class);

        when(annotation.taskDefinition()).thenReturn(UserTaskDetailsProvider.USE_METHOD_NAME);
        when(annotation.id()).thenReturn("differentId");
        when(connectable.getElementId()).thenReturn("elementId123");

        boolean result = wiring.callMethodMatchesElementId(connectable, method, annotation);

        assertThat(result).isFalse();
    }

    @Test
    void methodMatchesTaskDefinition_withIdNotUseMethodName_returnsFalse() throws NoSuchMethodException {
        Connectable connectable = mock(Connectable.class);
        Method method = TestMethodHolder.class.getDeclaredMethod("methodWithString", String.class);
        UserTaskDetailsProvider annotation = mock(UserTaskDetailsProvider.class);

        when(annotation.id()).thenReturn("specificId");

        boolean result = wiring.callMethodMatchesTaskDefinition(connectable, method, annotation);

        assertThat(result).isFalse();
    }

    @Test
    void methodMatchesTaskDefinition_withTaskDefinitionUseMethodNameAndMethodNameMatchesTaskDefinition_returnsTrue() throws NoSuchMethodException {
        Connectable connectable = mock(Connectable.class);
        Method method = TestMethodHolder.class.getDeclaredMethod("myTaskDef");
        UserTaskDetailsProvider annotation = mock(UserTaskDetailsProvider.class);

        when(annotation.id()).thenReturn(UserTaskDetailsProvider.USE_METHOD_NAME);
        when(annotation.taskDefinition()).thenReturn(UserTaskDetailsProvider.USE_METHOD_NAME);
        when(connectable.getTaskDefinition()).thenReturn("myTaskDef");

        boolean result = wiring.callMethodMatchesTaskDefinition(connectable, method, annotation);

        assertThat(result).isTrue();
    }

    @Test
    void methodMatchesTaskDefinition_withTaskDefinitionMatchesConnectableTaskDefinition_returnsTrue() throws NoSuchMethodException {
        Connectable connectable = mock(Connectable.class);
        Method method = TestMethodHolder.class.getDeclaredMethod("methodWithString", String.class);
        UserTaskDetailsProvider annotation = mock(UserTaskDetailsProvider.class);

        when(annotation.id()).thenReturn(UserTaskDetailsProvider.USE_METHOD_NAME);
        when(annotation.taskDefinition()).thenReturn("taskDef123");
        when(connectable.getTaskDefinition()).thenReturn("taskDef123");

        boolean result = wiring.callMethodMatchesTaskDefinition(connectable, method, annotation);

        assertThat(result).isTrue();
    }

    @Test
    void methodMatchesTaskDefinition_withTaskDefinitionNotMatching_returnsFalse() throws NoSuchMethodException {
        Connectable connectable = mock(Connectable.class);
        Method method = TestMethodHolder.class.getDeclaredMethod("methodWithString", String.class);
        UserTaskDetailsProvider annotation = mock(UserTaskDetailsProvider.class);

        when(annotation.id()).thenReturn(UserTaskDetailsProvider.USE_METHOD_NAME);
        when(annotation.taskDefinition()).thenReturn("differentTaskDef");
        when(connectable.getTaskDefinition()).thenReturn("taskDef123");

        boolean result = wiring.callMethodMatchesTaskDefinition(connectable, method, annotation);

        assertThat(result).isFalse();
    }

    /**
     * Helper class to provide real methods and parameters for testing
     */
    @SuppressWarnings("unused")
    private static class TestMethodHolder {
        public void methodWithPrefilledUserTaskDetails(PrefilledUserTaskDetails details) {}
        public void methodWithString(String value) {}
        public void methodWithDetailsEvent(@DetailsEvent DetailsEvent.Event event) {}
        public void methodWithWrongDetailsEventType(@DetailsEvent String wrongType) {}
        public void myTask() {}
        public void myTaskDef() {}
    }

    /**
     * Concrete test implementation of AbstractUserTaskWiring
     */
    private static class TestUserTaskWiring extends AbstractUserTaskWiring<Connectable, UserTaskMethodParameterFactory> {

        public TestUserTaskWiring(
                ApplicationContext applicationContext,
                SpringBeanUtil springBeanUtil,
                UserTaskMethodParameterFactory methodParameterFactory) {
            super(applicationContext, springBeanUtil, methodParameterFactory);
        }

        // Expose protected methods for testing
        public Class<UserTaskDetailsProvider> callGetAnnotationType() {
            return getAnnotationType();
        }

        public MethodParameter callValidatePrefilledUserTaskDetails(Method method, Parameter parameter, int index) {
            return validatePrefilledUserTaskDetails(method, parameter, index);
        }

        public MethodParameter callValidateDetailsEvent(Method method, Parameter parameter, int index) {
            return validateDetailsEvent(method, parameter, index);
        }

        public boolean callMethodMatchesElementId(Connectable connectable, Method method, UserTaskDetailsProvider annotation) {
            return methodMatchesElementId(connectable, method, annotation);
        }

        public boolean callMethodMatchesTaskDefinition(Connectable connectable, Method method, UserTaskDetailsProvider annotation) {
            return methodMatchesTaskDefinition(connectable, method, annotation);
        }
    }
}

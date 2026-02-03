package io.vanillabp.cockpit.adapter.common.wiring;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;
import freemarker.template.Version;
import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.springboot.parameters.MethodParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.data.repository.CrudRepository;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplatingHandlerBaseTest {

    @Mock
    private VanillaBpCockpitProperties properties;

    @Mock
    private Configuration templatingConfig;

    @Mock
    private CrudRepository<Object, Object> repository;

    @Mock
    private Logger logger;

    private TestTemplatingHandler handler;

    // Real FreeMarker configuration for template tests
    private Configuration realConfig;

    @BeforeEach
    void setUp() throws Exception {
        Object bean = new Object();
        Method method = Object.class.getMethod("toString");
        List<MethodParameter> parameters = Collections.emptyList();

        handler = new TestTemplatingHandler(
                properties,
                Optional.of(templatingConfig),
                repository,
                bean,
                method,
                parameters,
                logger);

        // Create a real FreeMarker configuration for tests that need it
        realConfig = new Configuration(new Version(2, 3, 32));
    }

    @Test
    void renderText_withNoTemplating_returnsNull() throws Exception {
        Object bean = new Object();
        Method method = Object.class.getMethod("toString");
        List<MethodParameter> parameters = Collections.emptyList();

        TestTemplatingHandler handlerWithoutTemplating = new TestTemplatingHandler(
                properties,
                Optional.empty(),
                repository,
                bean,
                method,
                parameters,
                logger);

        String result = handlerWithoutTemplating.callRenderText(
                e -> new Object[]{"test", e},
                Locale.ENGLISH,
                Arrays.asList("templates"),
                "test.ftl",
                new HashMap<>(),
                () -> "default");

        assertThat(result).isNull();
    }

    @Test
    void renderText_withTemplateNotFound_returnsDefaultValue() throws Exception {
        when(templatingConfig.getTemplate(anyString(), any(Locale.class)))
                .thenThrow(new TemplateNotFoundException("test.ftl", null, "not found"));

        String result = handler.callRenderText(
                e -> new Object[]{"test", e},
                Locale.ENGLISH,
                Arrays.asList("templates"),
                "test.ftl",
                new HashMap<>(),
                () -> "default value");

        assertThat(result).isEqualTo("default value");
    }

    @Test
    void renderText_withTemplateException_returnsDefaultValue() throws Exception {
        when(templatingConfig.getTemplate(anyString(), any(Locale.class)))
                .thenThrow(new IOException("Template error"));

        String result = handler.callRenderText(
                e -> new Object[]{"test", e},
                Locale.ENGLISH,
                Arrays.asList("templates"),
                "test.ftl",
                new HashMap<>(),
                () -> "default value");

        assertThat(result).isEqualTo("default value");
        verify(logger).error(eq("Could not get template '{}'"), anyString(), any(IOException.class));
    }

    @Test
    void renderText_withValidTemplate_returnsRenderedText() throws Exception {
        // Use real configuration for this test
        TestTemplatingHandler handlerWithRealConfig = new TestTemplatingHandler(
                properties,
                Optional.of(realConfig),
                repository,
                new Object(),
                Object.class.getMethod("toString"),
                Collections.emptyList(),
                logger);

        Template template = new Template("test", new StringReader("Hello ${name}!"), realConfig);
        when(templatingConfig.getTemplate(eq("templates/test.ftl"), any(Locale.class)))
                .thenReturn(template);

        // Use a handler with mock that returns a real template
        Map<String, Object> context = new HashMap<>();
        context.put("name", "World");

        String result = handler.callRenderText(
                e -> new Object[]{"test", e},
                Locale.ENGLISH,
                Arrays.asList("templates"),
                "test.ftl",
                context,
                () -> "default");

        assertThat(result).isEqualTo("Hello World!");
    }

    @Test
    void renderText_withMultiplePaths_triesEachUntilFound() throws Exception {
        when(templatingConfig.getTemplate(eq("path1/test.ftl"), any(Locale.class)))
                .thenThrow(new TemplateNotFoundException("test.ftl", null, "not found"));

        Template template = new Template("test", new StringReader("Found in path2"), realConfig);
        when(templatingConfig.getTemplate(eq("path2/test.ftl"), any(Locale.class)))
                .thenReturn(template);

        String result = handler.callRenderText(
                e -> new Object[]{"test", e},
                Locale.ENGLISH,
                Arrays.asList("path1", "path2"),
                "test.ftl",
                new HashMap<>(),
                () -> "default");

        assertThat(result).isEqualTo("Found in path2");
    }

    @Test
    void setTextInEvent_withDetailsGiven_usesDetailsValue() throws Exception {
        Template template = new Template("test", new StringReader("Rendered"), realConfig);
        when(templatingConfig.getTemplate(anyString(), any(Locale.class))).thenReturn(template);

        Map<String, String> details = new HashMap<>();
        details.put("en", "custom-template.ftl");

        Map<String, String> event = new HashMap<>();

        handler.callSetTextInEvent(
                "en",
                Locale.ENGLISH,
                "title",
                () -> details,
                () -> event,
                newEvent -> {},
                "Default Title",
                Arrays.asList("templates"),
                new HashMap<>(),
                (name, e) -> new Object[]{name, e});

        assertThat(event.get("en")).isEqualTo("Rendered");
    }

    @Test
    void setTextInEvent_withoutDetails_usesDefaultValue() throws Exception {
        when(templatingConfig.getTemplate(anyString(), any(Locale.class)))
                .thenThrow(new TemplateNotFoundException("test.ftl", null, "not found"));

        Map<String, String> event = new HashMap<>();

        handler.callSetTextInEvent(
                "en",
                Locale.ENGLISH,
                "title",
                () -> null,
                () -> event,
                newEvent -> {},
                "Default Title",
                Arrays.asList("templates"),
                new HashMap<>(),
                (name, e) -> new Object[]{name, e});

        assertThat(event.get("en")).isEqualTo("Default Title");
    }

    @Test
    void setTextInEvent_withImmutableMap_callsUpdatedEventConsumer() throws Exception {
        when(templatingConfig.getTemplate(anyString(), any(Locale.class)))
                .thenThrow(new TemplateNotFoundException("test.ftl", null, "not found"));

        Map<String, String> immutableEvent = Collections.emptyMap();
        @SuppressWarnings("unchecked")
        Consumer<Map<String, String>> updatedConsumer = mock(Consumer.class);

        handler.callSetTextInEvent(
                "en",
                Locale.ENGLISH,
                "title",
                () -> null,
                () -> immutableEvent,
                updatedConsumer,
                "Default Title",
                Arrays.asList("templates"),
                new HashMap<>(),
                (name, e) -> new Object[]{name, e});

        verify(updatedConsumer).accept(any());
    }

    @Test
    void setTextInEvent_withMutableMap_doesNotCallUpdatedEventConsumer() throws Exception {
        when(templatingConfig.getTemplate(anyString(), any(Locale.class)))
                .thenThrow(new TemplateNotFoundException("test.ftl", null, "not found"));

        Map<String, String> mutableEvent = new HashMap<>();
        @SuppressWarnings("unchecked")
        Consumer<Map<String, String>> updatedConsumer = mock(Consumer.class);

        handler.callSetTextInEvent(
                "en",
                Locale.ENGLISH,
                "title",
                () -> null,
                () -> mutableEvent,
                updatedConsumer,
                "Default Title",
                Arrays.asList("templates"),
                new HashMap<>(),
                (name, e) -> new Object[]{name, e});

        verify(updatedConsumer, never()).accept(any());
        assertThat(mutableEvent.get("en")).isEqualTo("Default Title");
    }

    /**
     * Concrete test implementation of TemplatingHandlerBase
     */
    private static class TestTemplatingHandler extends TemplatingHandlerBase {

        private final Logger testLogger;

        public TestTemplatingHandler(
                VanillaBpCockpitProperties properties,
                Optional<Configuration> templating,
                CrudRepository<Object, Object> repository,
                Object bean,
                Method method,
                List<MethodParameter> parameters,
                Logger logger) {
            super(properties, templating, repository, bean, method, parameters);
            this.testLogger = logger;
        }

        @Override
        protected Logger getLogger() {
            return testLogger;
        }

        // Expose protected methods for testing
        public String callRenderText(
                java.util.function.Function<Exception, Object[]> errorLoggingContext,
                Locale locale,
                List<String> templatePaths,
                String templateName,
                Object templateContext,
                Supplier<String> defaultValue) {
            return renderText(errorLoggingContext, locale, templatePaths, templateName, templateContext, defaultValue);
        }

        public void callSetTextInEvent(
                String language,
                Locale locale,
                String name,
                Supplier<Map<String, String>> detailsSupplier,
                Supplier<Map<String, String>> eventSupplier,
                Consumer<Map<String, String>> updatedEventConsumer,
                String defaultValue,
                List<String> templatePaths,
                Object templateContext,
                BiFunction<String, Exception, Object[]> errorLoggingContext) {
            setTextInEvent(language, locale, name, detailsSupplier, eventSupplier, updatedEventConsumer,
                    defaultValue, templatePaths, templateContext, errorLoggingContext);
        }
    }
}

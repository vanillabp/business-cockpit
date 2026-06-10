package io.vanillabp.cockpit.adapter.common.wiring;

import freemarker.template.Configuration;
import freemarker.template.TemplateNotFoundException;
import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.springboot.adapter.TaskHandlerBase;
import io.vanillabp.springboot.parameters.MethodParameter;
import java.io.File;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.springframework.data.repository.CrudRepository;

public abstract class TemplatingHandlerBase extends TaskHandlerBase {

    protected final VanillaBpCockpitProperties properties;

    protected final Optional<Configuration> templating;

    public TemplatingHandlerBase(
            final VanillaBpCockpitProperties properties,
            final Optional<Configuration> templating,
            final CrudRepository<Object, Object> workflowAggregateRepository,
            final Object bean,
            final Method method,
            final List<MethodParameter> parameters) {
        super(workflowAggregateRepository, bean, method, parameters);
        this.properties = properties;
        this.templating = templating;
    }
    
    protected abstract Logger getLogger();

    protected void setTextInEvent(
            final String language,
            final Locale locale,
            final String name,
            final Supplier<Map<String, String>> detailsSupplier,
            final Supplier<Map<String, String>> eventSupplier,
            final Consumer<Map<String, String>> updatedEventConsumer,
            final String defaultValue,
            final List<String> templatePathes,
            final Object templateContext,
            final BiFunction<String, Exception, Object[]> errorLoggingContext) {

        setTextInEvent(
                language,
                locale,
                name,
                detailsSupplier,
                eventSupplier,
                updatedEventConsumer,
                defaultValue,
                templatePathes,
                templateContext,
                Map.of(),
                errorLoggingContext);

    }

    protected void setTextInEvent(
            final String language,
            final Locale locale,
            final String name,
            final Supplier<Map<String, String>> detailsSupplier,
            final Supplier<Map<String, String>> eventSupplier,
            final Consumer<Map<String, String>> updatedEventConsumer,
            final String defaultValue,
            final List<String> templatePathes,
            final Object templateContext,
            final Map<String, Object> additionalTemplateContext,
            final BiFunction<String, Exception, Object[]> errorLoggingContext) {

        final var detailsGiven =
                (detailsSupplier.get() != null)
                && detailsSupplier.get().containsKey(language);
        final var templateName = detailsGiven
                ? detailsSupplier.get().get(language)
                : name;
        final var event = eventSupplier
                .get();
        final var text = renderText(
                e -> errorLoggingContext.apply(name, e),
                locale,
                templatePathes,
                templateName,
                templateContext,
                additionalTemplateContext,
                () -> detailsGiven
                        ? detailsSupplier.get().get(language)
                        : defaultValue);
        try {
            event.put(language, text);
        } catch (UnsupportedOperationException e) { // immutable map
            final var newEvent = new HashMap<>(event);
            newEvent.put(language, text);
            updatedEventConsumer.accept(newEvent);
        }
        
    }
    
    protected String renderText(
            final Function<Exception, Object[]> errorLoggingContext,
            final Locale locale,
            final List<String> templatePathes,
            final String templateName,
            final Object templateContext,
            final Supplier<String> defaultValue) {

        return renderText(
                errorLoggingContext,
                locale,
                templatePathes,
                templateName,
                templateContext,
                Map.of(),
                defaultValue);

    }

    protected String renderText(
            final Function<Exception, Object[]> errorLoggingContext,
            final Locale locale,
            final List<String> templatePathes,
            final String templateName,
            final Object templateContext,
            final Map<String, Object> additionalTemplateContext,
            final Supplier<String> defaultValue) {

        if (templating.isEmpty()) {
            return null;
        }

        return templatePathes
                .stream()
                .map(templatePath -> {
                    final var template = templatePath + File.separator + templateName;
                    try {
                        return templating
                                .get()
                                .getTemplate(template, locale);
                    } catch (TemplateNotFoundException e) {
                        return null;
                    } catch (Exception e) {
                        getLogger().error("Could not get template '{}'", template, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .flatMap(template -> {
                    try {
                        final var writer = new StringWriter();
                        // templateContext stays an Object: FreeMarker wraps it itself
                        // using the configured ObjectWrapper (works for Map and POJO alike).
                        final var environment = template.createProcessingEnvironment(templateContext, writer);
                        final var objectWrapper = environment.getObjectWrapper();
                        for (final var additionalValue : additionalTemplateContext.entrySet()) {
                            // global variables are visible like data-model entries, but the
                            // data-model (templateContext) takes precedence on name clashes.
                            environment.setGlobalVariable(
                                    additionalValue.getKey(),
                                    objectWrapper.wrap(additionalValue.getValue()));
                        }
                        environment.process();
                        return Optional.of(writer.toString());
                    } catch (Exception e) {
                        getLogger().error(
                                "Could not render {} for user task '{}' of workflow '{}'",
                                errorLoggingContext.apply(e));
                        return Optional.empty();
                    }
                })
                .orElse(defaultValue.get());

    }

}

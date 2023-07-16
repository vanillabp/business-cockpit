package io.vanillabp.cockpit.adapter.common.wiring;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.springframework.data.repository.CrudRepository;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.util.StringUtils;

import freemarker.template.Configuration;
import freemarker.template.TemplateNotFoundException;
import io.vanillabp.cockpit.adapter.common.usertask.UserTasksProperties;
import io.vanillabp.springboot.adapter.TaskHandlerBase;
import io.vanillabp.springboot.parameters.MethodParameter;

public abstract class TemplatingHandlerBase extends TaskHandlerBase {

    protected final UserTasksProperties workflowProperties;

    protected final Optional<Configuration> templating;

    public TemplatingHandlerBase(
            final UserTasksProperties workflowProperties,
            final Optional<Configuration> templating,
            final CrudRepository<Object, Object> workflowAggregateRepository,
            final Object bean,
            final Method method,
            final List<MethodParameter> parameters) {
        super(workflowAggregateRepository, bean, method, parameters);
        this.workflowProperties = workflowProperties;
        this.templating = templating;
    }
    
    protected abstract Logger getLogger();

    protected void setTextInEvent(
            final String language,
            final Locale locale,
            final String name,
            final Supplier<Map<String, String>> detailsSupplier,
            final Supplier<Map<String, String>> eventSupplier,
            final String defaultValue,
            final Object templateContext,
            final BiFunction<String, Exception, Object[]> errorLoggingContext) {
        
        final var detailsGiven = 
                (detailsSupplier.get() != null)
                && detailsSupplier.get().containsKey(language);
        final var templateName = detailsGiven
                ? detailsSupplier.get().get(language)
                : name;
        eventSupplier
                .get()
                .put(
                        language,
                        renderText(
                                e -> errorLoggingContext.apply(name, e),
                                locale,
                                templateName,
                                templateContext,
                                () -> detailsGiven
                                        ? detailsSupplier.get().get(language)
                                        : defaultValue));
        
    }
    
    protected String renderText(
            final Function<Exception, Object[]> errorLoggingContext,
            final Locale locale,
            final String templateName,
            final Object templateContext,
            final Supplier<String> defaultValue) {
        
        final var templatePath = 
                (workflowProperties.getTemplatesPath()
                + (StringUtils.hasText(templateName)
                        ? File.separator + templateName
                        : "")
                .replace(File.separator + File.separator, File.separator));
        try {
            final var template = templating
                    .get()
                    .getTemplate(templatePath, locale);
            return FreeMarkerTemplateUtils.processTemplateIntoString(
                    template,
                    templateContext);
        } catch (Exception e) {
            if (!(e instanceof TemplateNotFoundException)) {
                getLogger().warn("Could not render {} for user task '{}' of workflow '{}'! "
                        + "Will use BPMN title",
                        errorLoggingContext.apply(e));
            } else {
                // templateName seems to be text already rendered by user code
            }
            return defaultValue.get();
        }
        
    }

}

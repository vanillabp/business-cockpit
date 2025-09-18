package io.vanillabp.cockpit.adapter.camunda8.workflow;

import freemarker.template.Configuration;
import io.camunda.client.CamundaClient;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowLifeCycleEvent;
import io.vanillabp.cockpit.adapter.camunda8.workflow.publishing.ProcessWorkflowAfterTransactionEvent;
import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.workflow.WorkflowHandlerBase;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCancelledEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCompletedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEventImpl;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowUpdatedEvent;
import io.vanillabp.spi.cockpit.details.DetailsEvent;
import io.vanillabp.spi.cockpit.workflow.PrefilledWorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetails;
import io.vanillabp.springboot.adapter.AdapterAwareProcessService;
import io.vanillabp.springboot.adapter.wiring.WorkflowAggregateCache;
import io.vanillabp.springboot.parameters.MethodParameter;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.repository.CrudRepository;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Transactional
public class Camunda8WorkflowHandler extends WorkflowHandlerBase {
    private static final Logger logger = LoggerFactory.getLogger(Camunda8WorkflowHandler.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    private final VanillaBpCockpitProperties cockpitProperties;

    private final AdapterAwareProcessService<?> processService;

    private final String bpmnProcessId;

    private final String bpmnProcessName;

    private final String aggregateIdPropertyName;

    private Function<String, Object> parseWorkflowAggregateIdFromBusinessKey;

    private String bpmnProcessVersionInfo;

    private final CamundaClient client;

    public Camunda8WorkflowHandler(
            final VanillaBpCockpitProperties cockpitProperties,
            final ApplicationEventPublisher applicationEventPublisher,
            final AdapterAwareProcessService<?> processService,
            final String bpmnProcessId,
            final String bpmnProcessVersionInfo,
            final String bpmnProcessName,
            final Optional<Configuration> templating,
            final String aggregateIdPropertyName,
            final CrudRepository<Object, Object> workflowAggregateRepository,
            final Object bean,
            final Method method,
            final List<MethodParameter> parameters,
            final CamundaClient client) {

        super(cockpitProperties, templating, workflowAggregateRepository, bean, method, parameters);
        this.applicationEventPublisher = applicationEventPublisher;
        this.aggregateIdPropertyName = aggregateIdPropertyName;
        this.cockpitProperties = cockpitProperties;
        this.processService = processService;
        this.bpmnProcessId = bpmnProcessId;
        this.bpmnProcessVersionInfo = bpmnProcessVersionInfo;
        this.bpmnProcessName = bpmnProcessName;
        this.client = client;

        determineBusinessKeyToIdMapper();
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    public String getBpmnProcessId() {
        return bpmnProcessId;
    }

    public String getWorkflowModuleId() {
        return processService.getWorkflowModuleId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(
            retryFor = { Exception.class },
            maxAttempts = 4,
            backoff = @Backoff(delay = 500, maxDelay = 1500, multiplier = 1.5))
    public void notify(
            final Camunda8WorkflowLifeCycleEvent camunda8WorkflowLifeCycleEvent) {

        final var workflowModuleId = processService.getWorkflowModuleId();
        final var i18nLanguages = properties.getI18nLanguages(workflowModuleId, bpmnProcessId);

        Camunda8WorkflowLifeCycleEvent.Intent intent = camunda8WorkflowLifeCycleEvent.getIntent();
        WorkflowEventImpl workflowEvent = switch (intent) {
            case COMPLETED -> new WorkflowCompletedEvent(workflowModuleId, i18nLanguages);
            case CANCELLED -> new WorkflowCancelledEvent(workflowModuleId, i18nLanguages);
        };

        final var camunda8WorkflowEvent = new Camunda8WorkflowEvent();
        camunda8WorkflowEvent.setJobKey(camunda8WorkflowLifeCycleEvent.getKey());
        camunda8WorkflowEvent.setBpmnProcessId(bpmnProcessId);
        camunda8WorkflowEvent.setVariables(getBusinessKeyVariablesFromProcessInstanceKey(camunda8WorkflowLifeCycleEvent.getProcessInstanceKey()));
        camunda8WorkflowEvent.setTenantId(camunda8WorkflowLifeCycleEvent.getTenantId());
        camunda8WorkflowEvent.setEvent(intent == Camunda8WorkflowLifeCycleEvent.Intent.COMPLETED ? DetailsEvent.Event.COMPLETED : DetailsEvent.Event.CANCELED);
        camunda8WorkflowEvent.setProcessInstanceKey(camunda8WorkflowLifeCycleEvent.getProcessInstanceKey());
        camunda8WorkflowEvent.setProcessDefinitionVersion(camunda8WorkflowLifeCycleEvent.getWorkflowDefinitionVersion());
        camunda8WorkflowEvent.setProcessDefinitionKey(camunda8WorkflowLifeCycleEvent.getProcessDefinitionKey());
        camunda8WorkflowEvent.setTimestamp(
                OffsetDateTime.ofInstant(Instant.ofEpochSecond(camunda8WorkflowLifeCycleEvent.getTimestamp()), ZoneOffset.UTC));
        this.fillWorkflowEvent(camunda8WorkflowEvent, workflowEvent);

        publishEvent(workflowEvent);

    }

    private Map<String, Object> getBusinessKeyVariablesFromProcessInstanceKey(
            long processInstanceKey) {

        final var businessKeyVariable = client
                .newVariableSearchRequest()
                .filter(filter -> filter
                        .processInstanceKey(processInstanceKey)
                        .name(aggregateIdPropertyName))
                .send()
                .join()
                .singleItem();
        return Map.of(businessKeyVariable.getName(), businessKeyVariable.getValue());

    }

    @Recover
    @SuppressWarnings("unused")
    public void recoverProcessLifeCycleEvent(
            final Exception exception,
            final Camunda8WorkflowLifeCycleEvent camunda8WorkflowLifeCycleEvent) {

        logger.error("Could not process workflow lifecycle event: '{}'!",
                camunda8WorkflowLifeCycleEvent,
                exception);

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(
            retryFor = { Exception.class },
            maxAttempts = 4,
            backoff = @Backoff(delay = 500, maxDelay = 1500, multiplier = 1.5))
    public void notify(
            final Camunda8WorkflowEvent camunda8WorkflowEvent) {

        final var workflowModuleId = processService.getWorkflowModuleId();
        final var i18nLanguages = properties.getI18nLanguages(workflowModuleId, bpmnProcessId);

        WorkflowEventImpl workflowEvent = switch (camunda8WorkflowEvent.getEvent()) {
            case CREATED -> new WorkflowCreatedEvent(workflowModuleId, i18nLanguages);
            case UPDATED -> new WorkflowUpdatedEvent(workflowModuleId, i18nLanguages);
            case COMPLETED -> new WorkflowCompletedEvent(workflowModuleId, i18nLanguages);
            case CANCELED -> new WorkflowCancelledEvent(workflowModuleId, i18nLanguages);
        };
        this.fillWorkflowEvent(camunda8WorkflowEvent, workflowEvent);

        publishEvent(workflowEvent);

        client.newCompleteCommand(camunda8WorkflowEvent.getJobKey()).send().join();

    }

    @Recover
    @SuppressWarnings("unused")
    public void recoverProcessWorkflowEvent(
            final Exception exception,
            final Camunda8WorkflowEvent camunda8WorkflowEvent) {

        logger.error("Could not process workflow event: '{}'!",
                camunda8WorkflowEvent,
                exception);

    }

    private void publishEvent(WorkflowEvent workflowEvent) {

        applicationEventPublisher.publishEvent(
                new io.vanillabp.cockpit.adapter.camunda8.workflow.publishing.WorkflowEvent(
                        Camunda8WorkflowHandler.class,
                        workflowEvent));

        applicationEventPublisher.publishEvent(
                new ProcessWorkflowAfterTransactionEvent(Camunda8WorkflowHandler.class));

    }

    public void updateVersionInfo(
            final String versionInfo) {

        this.bpmnProcessVersionInfo = versionInfo;

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(
            retryFor = { Exception.class },
            maxAttempts = 4,
            backoff = @Backoff(delay = 500, maxDelay = 1500, multiplier = 1.5))
    public void notify(
            final Camunda8WorkflowCreatedEvent camunda8WorkflowCreatedEvent) {

        final var workflowModuleId = processService.getWorkflowModuleId();
        final var i18nLanguages = properties.getI18nLanguages(workflowModuleId, bpmnProcessId);
        final var workflowCreatedEvent = new WorkflowCreatedEvent(
                workflowModuleId,
                i18nLanguages
        );

        final var camunda8WorkflowEvent = new Camunda8WorkflowEvent();
        camunda8WorkflowEvent.setJobKey(camunda8WorkflowCreatedEvent.getKey());
        camunda8WorkflowEvent.setBpmnProcessId(bpmnProcessId);
        camunda8WorkflowEvent.setVariables(Map.of(aggregateIdPropertyName, camunda8WorkflowCreatedEvent.getBusinessKey()));
        camunda8WorkflowEvent.setTenantId(camunda8WorkflowCreatedEvent.getTenantId());
        camunda8WorkflowEvent.setEvent(DetailsEvent.Event.CREATED);
        camunda8WorkflowEvent.setProcessInstanceKey(camunda8WorkflowCreatedEvent.getProcessInstanceKey());
        camunda8WorkflowEvent.setProcessDefinitionVersion(camunda8WorkflowCreatedEvent.getWorkflowDefinitionVersion());
        camunda8WorkflowEvent.setProcessDefinitionKey(camunda8WorkflowCreatedEvent.getProcessDefinitionKey());
        camunda8WorkflowEvent.setTimestamp(
                OffsetDateTime.ofInstant(Instant.ofEpochSecond(camunda8WorkflowCreatedEvent.getTimestamp()), ZoneOffset.UTC));
        fillWorkflowEvent(camunda8WorkflowEvent, workflowCreatedEvent);

        publishEvent(workflowCreatedEvent);
        
    }

    @Recover
    @SuppressWarnings("unused")
    public void recoverProcessCreatedEvent(
            final Exception exception,
            final Camunda8WorkflowCreatedEvent camunda8WorkflowCreatedEvent) {

        logger.error("Could not process workflow created event: '{}'!",
                camunda8WorkflowCreatedEvent,
                exception);

    }

    public void fillWorkflowEvent(
            Camunda8WorkflowEvent camunda8WorkflowEvent,
            WorkflowEventImpl workflowEvent
    ) {

        final var rawBusinessKey = camunda8WorkflowEvent.getVariables().get(aggregateIdPropertyName);
        if (rawBusinessKey == null) {
            logger.error("Could not find process variable '{}' in event for type '{}'! Will ignore this event.",
                    aggregateIdPropertyName, camunda8WorkflowEvent.getBpmnProcessId());
            return;
        }
        final var parsedBusinessKey = parseWorkflowAggregateIdFromBusinessKey.apply(rawBusinessKey.toString());

        prefillWorkflowDetails(
                camunda8WorkflowEvent,
                workflowEvent,
                parsedBusinessKey);

        String processInstanceKey = String.valueOf(camunda8WorkflowEvent.getProcessInstanceKey());

        final WorkflowDetails details = callWorkflowDetailsProviderMethod(
                processInstanceKey,
                parsedBusinessKey,
                workflowEvent);

        fillWorkflowDetailsByCustomDetails(
                workflowEvent,
                details == null
                        ? workflowEvent
                        : details);
    }

    private void prefillWorkflowDetails(
            final Camunda8WorkflowEvent camunda8WorkflowEvent,
            final WorkflowEventImpl workflowEvent,
            final Object businessKey) {

        workflowEvent.setEventId(
                System.nanoTime() + "@" + camunda8WorkflowEvent.getJobKey() + "#" + camunda8WorkflowEvent.getEvent());

        workflowEvent.setTimestamp(camunda8WorkflowEvent.getTimestamp());
        workflowEvent.setBpmnProcessId(camunda8WorkflowEvent.getBpmnProcessId());
        workflowEvent.setBpmnProcessVersion(bpmnProcessVersionInfo);
        workflowEvent.setWorkflowId(
                Long.toString(camunda8WorkflowEvent.getProcessInstanceKey()));
        workflowEvent.setBusinessId(businessKey.toString());

        workflowEvent.setTitle(new HashMap<>());

    }

    @SuppressWarnings("unchecked")
    private WorkflowDetails callWorkflowDetailsProviderMethod(
            final String processInstanceId,
            final Object businessKey,
            final PrefilledWorkflowDetails prefilledWorkflowDetails) {

        try {
            logger.trace("Will handle workflow '{}' ('{}')",
                    processInstanceId,
                    bpmnProcessId);

            final var workflowAggregateCache = new WorkflowAggregateCache();

            return super.execute(
                    workflowAggregateCache,
                    businessKey,
                    true,
                    (args, param) -> processPrefilledWorkflowDetailsParameter(
                            args,
                            param,
                            () -> prefilledWorkflowDetails)
            );
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void fillWorkflowDetailsByCustomDetails(
            WorkflowEventImpl event,
            WorkflowDetails details
    ) {
        // a different object was returned then provided
        if (details != event) {
            event.setInitiator(
                    details.getInitiator());
            event.setComment(
                    details.getComment());
            event.setDetails(
                    details.getDetails());
            event.setDetailsFulltextSearch(
                    details.getDetailsFulltextSearch());
            event.setI18nLanguages(
                    details.getI18nLanguages());
            event.setTitle(
                    details.getTitle());
            event.setUiUriPath(
                    details.getUiUriPath());
        }

        if ((event.getI18nLanguages() == null)
                || event.getI18nLanguages().isEmpty()){
            event.setI18nLanguages(
                    cockpitProperties.getI18nLanguages(processService.getWorkflowModuleId(), bpmnProcessId));
        }

        if (templating.isEmpty()) {

            final var language = properties.getBpmnDescriptionLanguage(processService.getWorkflowModuleId(), bpmnProcessId);

            if ((details.getTitle() == null)
                    || details.getTitle().isEmpty()) {
                event.setTitle(Map.of(
                        language,
                        bpmnProcessName));
            }
            if ((details.getDetailsFulltextSearch() == null)
                    || !StringUtils.hasText(details.getDetailsFulltextSearch())) {
                event.setDetailsFulltextSearch(
                        bpmnProcessName);
            }

        } else {
            final var templatesPathes = List.of(properties.getTemplatePath(processService.getWorkflowModuleId(), bpmnProcessId));

            event
                    .getI18nLanguages()
                    .forEach(language -> {
                        final var locale = Locale.forLanguageTag(language);

                        final BiFunction<String, Exception, Object[]> errorLoggingContext
                                = (name, e) -> new Object[] {
                                name,
                                event.getWorkflowId(),
                                e
                        };

                        setTextInEvent(
                                language,
                                locale,
                                "workflow-title.ftl",
                                details::getTitle,
                                event::getTitle,
                                event::setTitle,
                                bpmnProcessName,
                                templatesPathes,
                                details.getTemplateContext(),
                                errorLoggingContext);

                        event.setDetailsFulltextSearch(
                                renderText(
                                        e -> errorLoggingContext.apply(
                                                "details-fulltext-search.ftl",
                                                e),
                                        locale,
                                        templatesPathes,
                                        "details-fulltext-search.ftl",
                                        details.getTemplateContext(),
                                        () -> bpmnProcessName));

                    });

        }

    }

    // TODO GWI - refactor, copy of Camunda7UserTaskHandler
    private void determineBusinessKeyToIdMapper() {

        final var workflowAggregateIdClass = processService.getWorkflowAggregateIdClass();

        if (String.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = businessKey -> businessKey;
        } else if (int.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = Integer::valueOf;
        } else if (long.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = Long::valueOf;
        } else if (float.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = Float::valueOf;
        } else if (double.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = Double::valueOf;
        } else if (byte.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = Byte::valueOf;
        } else if (BigInteger.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = BigInteger::new;
        } else {
            try {
                final var valueOfMethod = workflowAggregateIdClass.getMethod("valueOf", String.class);
                parseWorkflowAggregateIdFromBusinessKey = businessKey -> {
                    try {
                        return valueOfMethod.invoke(null, businessKey);
                    } catch (Exception e) {
                        throw new RuntimeException("Could not determine the workflow's aggregate id!", e);
                    }
                };
            } catch (Exception e) {
                throw new RuntimeException(
                        String.format(
                                "The id's class '%s' of the workflow-aggregate '%s' does not implement a method 'public static %s valueOf(String businessKey)'! Please add this method required by VanillaBP 'camunda7' Business Cockpit adapter.",
                                workflowAggregateIdClass.getName(),
                                processService.getWorkflowAggregateClass(),
                                workflowAggregateIdClass.getSimpleName()));
            }
        }

    }
}

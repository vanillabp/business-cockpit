package io.vanillabp.cockpit.adapter.camunda8.workflow;

import freemarker.template.Configuration;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowLifeCycleEvent;
import io.vanillabp.cockpit.adapter.camunda8.workflow.persistence.ProcessInstanceEntity;
import io.vanillabp.cockpit.adapter.camunda8.workflow.persistence.ProcessInstanceRepository;
import io.vanillabp.cockpit.adapter.common.CockpitProperties;
import io.vanillabp.cockpit.adapter.common.usertask.UserTasksProperties;
import io.vanillabp.cockpit.adapter.common.workflow.WorkflowHandlerBase;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCancelledEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCompletedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowLifecycleEvent;
import io.vanillabp.spi.cockpit.workflow.PrefilledWorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetails;
import io.vanillabp.springboot.adapter.AdapterAwareProcessService;
import io.vanillabp.springboot.adapter.wiring.WorkflowAggregateCache;
import io.vanillabp.springboot.parameters.MethodParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Camunda8WorkflowHandler extends WorkflowHandlerBase {
    private static final Logger logger = LoggerFactory.getLogger(Camunda8WorkflowHandler.class);

    private final CockpitProperties cockpitProperties;

    private final AdapterAwareProcessService<?> processService;

    private final String bpmnProcessId;

    private final String bpmnProcessName;

    private final ProcessInstanceRepository processInstanceRepository;

    private Function<String, Object> parseWorkflowAggregateIdFromBusinessKey;


    public Camunda8WorkflowHandler(
            final CockpitProperties cockpitProperties,
            final UserTasksProperties workflowProperties,
            final AdapterAwareProcessService<?> processService,
            final String bpmnProcessId,
            final String bpmnProcessName,
            final Optional<Configuration> templating,
            final ProcessInstanceRepository processInstanceRepository,
            final CrudRepository<Object, Object> workflowAggregateRepository,
            final Object bean,
            final Method method,
            final List<MethodParameter> parameters) {

        super(workflowProperties, templating, workflowAggregateRepository, bean, method, parameters);
        this.cockpitProperties = cockpitProperties;
        this.processService = processService;
        this.bpmnProcessId = bpmnProcessId;
        this.bpmnProcessName = bpmnProcessName;
        this.processInstanceRepository = processInstanceRepository;

        determineBusinessKeyToIdMapper();
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }


    public WorkflowEvent processLifeCycleEvent(
            final Camunda8WorkflowLifeCycleEvent camunda8WorkflowLifeCycleEvent) {

        WorkflowLifecycleEvent workflowCreatedEvent =
                switch (camunda8WorkflowLifeCycleEvent.getIntent()) {
                    case CANCELLED -> new WorkflowCancelledEvent();
                    case COMPLETED -> new WorkflowCompletedEvent();
                };
        fillLifecycleEvent(camunda8WorkflowLifeCycleEvent, workflowCreatedEvent);

        return workflowCreatedEvent;
    }


    private void fillLifecycleEvent(
            final Camunda8WorkflowLifeCycleEvent workflowLifeCycleEvent,
            final WorkflowLifecycleEvent event) {

        event.setId(System.nanoTime() + "@" +
                workflowLifeCycleEvent.getProcessInstanceKey() + "#" +
                workflowLifeCycleEvent.getKey());

        event.setComment(
                workflowLifeCycleEvent.getDeleteReason());
        event.setTimestamp(
                OffsetDateTime.now());
        event.setWorkflowId(
                String.valueOf(workflowLifeCycleEvent.getProcessInstanceKey()));

        event.setInitiator(null); // TODO
        event.setBpmnProcessId(workflowLifeCycleEvent.getBpmnProcessId());
        event.setBpmnProcessVersion(workflowLifeCycleEvent.getBpmnProcessVersion());
    }

    public WorkflowEvent processCreatedEvent(
            final Camunda8WorkflowCreatedEvent camunda8WorkflowCreatedEvent) {

        WorkflowCreatedEvent workflowCreatedEvent = new WorkflowCreatedEvent(
                camunda8WorkflowCreatedEvent.getTenantId(),
                List.of(workflowProperties.getBpmnDescriptionLanguage())
        );
        fillWorkflowCreatedEvent(camunda8WorkflowCreatedEvent, workflowCreatedEvent);
        saveBusinessKeyProcessInstanceConnection(camunda8WorkflowCreatedEvent);

        return workflowCreatedEvent;
    }

    public void fillWorkflowCreatedEvent(
            Camunda8WorkflowCreatedEvent camunda8WorkflowCreatedEvent,
            WorkflowCreatedEvent workflowCreatedEvent
    ) {

        prefillWorkflowDetails(
                camunda8WorkflowCreatedEvent,
                workflowCreatedEvent);

        String processInstanceKey = String.valueOf(camunda8WorkflowCreatedEvent.getProcessInstanceKey());

        final WorkflowDetails details = callWorkflowDetailsProviderMethod(
                processInstanceKey,
                camunda8WorkflowCreatedEvent.getBusinessKey(),
                workflowCreatedEvent);

        fillWorkflowDetailsByCustomDetails(
                workflowCreatedEvent,
                details == null
                        ? workflowCreatedEvent
                        : details);
    }


    private void prefillWorkflowDetails(
            final Camunda8WorkflowCreatedEvent workflowCreatedEvent,
            final WorkflowCreatedEvent event) {

        event.setId(System.nanoTime() + "@" +
                workflowCreatedEvent.getProcessInstanceKey() + "#" +
                workflowCreatedEvent.getKey());

        event.setTimestamp(
                OffsetDateTime.now());
        event.setBpmnProcessId(
                workflowCreatedEvent.getBpmnProcessId());
        event.setBpmnProcessVersion(
                String.valueOf(workflowCreatedEvent.getVersion()));
        event.setWorkflowId(
                String.valueOf(workflowCreatedEvent.getProcessInstanceKey()));
        event.setBusinessId(
                workflowCreatedEvent.getBusinessKey());
        event.setTitle(new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    private WorkflowDetails callWorkflowDetailsProviderMethod(
            final String processInstanceId,
            final String businessKey,
            final PrefilledWorkflowDetails prefilledWorkflowDetails) {

        try {
            logger.trace("Will handle workflow '{}' ('{}')",
                    processInstanceId,
                    bpmnProcessId);

            final var workflowAggregateId = parseWorkflowAggregateIdFromBusinessKey.apply(businessKey);
            final var workflowAggregateCache = new WorkflowAggregateCache();

            return super.execute(
                    workflowAggregateCache,
                    workflowAggregateId,
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
            WorkflowCreatedEvent event,
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
                    cockpitProperties.getI18nLanguages());
        }

        if (templating.isEmpty()) {

            final var language = workflowProperties.getBpmnDescriptionLanguage();

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
                                bpmnProcessName,
                                details.getTemplateContext(),
                                errorLoggingContext);

                        event.setDetailsFulltextSearch(
                                renderText(
                                        e -> errorLoggingContext.apply(
                                                "details-fulltext-search.ftl",
                                                e),
                                        locale,
                                        details.getDetailsFulltextSearch(),
                                        details.getTemplateContext(),
                                        () -> bpmnProcessName));

                    });

        }

    }

    private void saveBusinessKeyProcessInstanceConnection(Camunda8WorkflowCreatedEvent camunda8WorkflowCreatedEvent) {
        ProcessInstanceEntity processInstanceEntity =
                new ProcessInstanceEntity(
                        camunda8WorkflowCreatedEvent.getProcessInstanceKey(),
                        camunda8WorkflowCreatedEvent.getBusinessKey());
        processInstanceRepository.save(processInstanceEntity);
    }


    // TODO GWI - refactor, copy of Camunda7UserTaskHandler
    private void determineBusinessKeyToIdMapper() {

        final var workflowAggregateIdClass = processService.getWorkflowAggregateIdClass();

        if (String.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = businessKey -> businessKey;
        } else if (int.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = businessKey -> Integer.valueOf(businessKey);
        } else if (long.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = businessKey -> Long.valueOf(businessKey);
        } else if (float.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = businessKey -> Float.valueOf(businessKey);
        } else if (double.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = businessKey -> Double.valueOf(businessKey);
        } else if (byte.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = businessKey -> Byte.valueOf(businessKey);
        } else if (BigInteger.class.isAssignableFrom(workflowAggregateIdClass)) {
            parseWorkflowAggregateIdFromBusinessKey = businessKey -> new BigInteger(businessKey);
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

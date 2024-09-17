package io.vanillabp.cockpit.adapter.camunda8.workflow;

import freemarker.template.Configuration;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowLifeCycleEvent;
import io.vanillabp.cockpit.adapter.camunda8.workflow.persistence.ProcessInstanceEntity;
import io.vanillabp.cockpit.adapter.camunda8.workflow.persistence.ProcessInstanceMapper;
import io.vanillabp.cockpit.adapter.camunda8.workflow.persistence.ProcessInstanceRepository;
import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.workflow.WorkflowHandlerBase;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCancelledEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCompletedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowLifecycleEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowUpdatedEvent;
import io.vanillabp.spi.cockpit.workflow.PrefilledWorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetails;
import io.vanillabp.springboot.adapter.AdapterAwareProcessService;
import io.vanillabp.springboot.adapter.wiring.WorkflowAggregateCache;
import io.vanillabp.springboot.parameters.MethodParameter;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Transactional
public class Camunda8WorkflowHandler extends WorkflowHandlerBase {
    private static final Logger logger = LoggerFactory.getLogger(Camunda8WorkflowHandler.class);

    private final VanillaBpCockpitProperties cockpitProperties;

    private final AdapterAwareProcessService<?> processService;

    private final String bpmnProcessId;

    private final String bpmnProcessName;

    private final ProcessInstanceRepository processInstanceRepository;

    private Function<String, Object> parseWorkflowAggregateIdFromBusinessKey;


    public Camunda8WorkflowHandler(
            final VanillaBpCockpitProperties cockpitProperties,
            final AdapterAwareProcessService<?> processService,
            final String bpmnProcessId,
            final String bpmnProcessName,
            final Optional<Configuration> templating,
            final ProcessInstanceRepository processInstanceRepository,
            final CrudRepository<Object, Object> workflowAggregateRepository,
            final Object bean,
            final Method method,
            final List<MethodParameter> parameters) {

        super(cockpitProperties, templating, workflowAggregateRepository, bean, method, parameters);
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

        event.setEventId(System.nanoTime() + "@" +
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

        final var language = properties.getBpmnDescriptionLanguage(processService.getWorkflowModuleId(), bpmnProcessId);

        WorkflowCreatedEvent workflowCreatedEvent = new WorkflowCreatedEvent(
                camunda8WorkflowCreatedEvent.getTenantId(),
                List.of(language)
        );
        fillWorkflowCreatedEvent(camunda8WorkflowCreatedEvent, workflowCreatedEvent);
        saveBusinessKeyProcessInstanceConnection(camunda8WorkflowCreatedEvent);

        return workflowCreatedEvent;
    }


    public WorkflowEvent processUpdatedEvent(
            final Camunda8WorkflowCreatedEvent camunda8WorkflowCreatedEvent) {

        final var language = properties.getBpmnDescriptionLanguage(processService.getWorkflowModuleId(), bpmnProcessId);

        WorkflowUpdatedEvent workflowUpdatedEvent = new WorkflowUpdatedEvent(
                camunda8WorkflowCreatedEvent.getTenantId(),
                List.of(language)
        );
        fillWorkflowCreatedEvent(camunda8WorkflowCreatedEvent, workflowUpdatedEvent);

        return workflowUpdatedEvent;
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

        event.setEventId(System.nanoTime() + "@" +
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

    private void saveBusinessKeyProcessInstanceConnection(Camunda8WorkflowCreatedEvent camunda8WorkflowCreatedEvent) {
        ProcessInstanceEntity processInstanceEntity = ProcessInstanceMapper.map(camunda8WorkflowCreatedEvent);
        processInstanceRepository.save(processInstanceEntity);
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

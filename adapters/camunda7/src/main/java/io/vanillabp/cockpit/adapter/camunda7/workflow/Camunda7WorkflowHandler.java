package io.vanillabp.cockpit.adapter.camunda7.workflow;

import freemarker.template.Configuration;
import io.vanillabp.cockpit.adapter.common.CockpitProperties;
import io.vanillabp.cockpit.adapter.common.usertask.UserTasksProperties;
import io.vanillabp.cockpit.adapter.common.workflow.WorkflowHandlerBase;
import io.vanillabp.cockpit.adapter.common.workflow.events.*;
import io.vanillabp.spi.cockpit.workflow.PrefilledWorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetails;
import io.vanillabp.springboot.adapter.AdapterAwareProcessService;
import io.vanillabp.springboot.adapter.wiring.WorkflowAggregateCache;
import io.vanillabp.springboot.parameters.MethodParameter;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.impl.history.event.HistoricProcessInstanceEventEntity;
import org.camunda.bpm.engine.impl.history.event.HistoryEventTypes;
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

public class Camunda7WorkflowHandler extends WorkflowHandlerBase {

    private static final Logger logger = LoggerFactory.getLogger(Camunda7WorkflowHandler.class);

    private final CockpitProperties cockpitProperties;


    private final AdapterAwareProcessService<?> processService;

    private final String bpmnProcessId;

    private Function<String, Object> parseWorkflowAggregateIdFromBusinessKey;
    
    public Camunda7WorkflowHandler(
            final CockpitProperties cockpitProperties,
            final UserTasksProperties workflowProperties,
            final AdapterAwareProcessService<?> processService,
            final String bpmnProcessId,
            final Optional<Configuration> templating,
            final CrudRepository<Object, Object> workflowAggregateRepository,
            final Object bean,
            final Method method,
            final List<MethodParameter> parameters) {

        super(workflowProperties, templating, workflowAggregateRepository, bean, method, parameters);
        this.cockpitProperties = cockpitProperties;
        this.processService = processService;
        this.bpmnProcessId = bpmnProcessId;

        determineBusinessKeyToIdMapper();

    }

    @Override
    protected Logger getLogger() {

        return logger;
        
    }
    
    public WorkflowEvent wrapProcessInstance(
            final HistoricProcessInstance processInstance,
            final String bpmnProcessName,
            final String bpmnProcessVersion) {

        final String workflowModuleId = processInstance.getTenantId();
        
        WorkflowEvent workflowEvent = null;
        if (processInstance.getEndTime() != null) {
            WorkflowCompletedEvent workflowCompletedEvent = new WorkflowCompletedEvent();
            fillLifecycleEvent(bpmnProcessId, bpmnProcessVersion, processInstance, workflowCompletedEvent);
            workflowEvent = workflowCompletedEvent;
        } else {
            WorkflowUpdatedEvent workflowUpdatedEvent = new WorkflowUpdatedEvent(
                    workflowModuleId,
                    cockpitProperties.getI18nLanguages());
            fillWorkflowCreatedEvent(processInstance, bpmnProcessName, bpmnProcessVersion, workflowUpdatedEvent);
            workflowEvent = workflowUpdatedEvent;
        }
        return workflowEvent;

    }


    public WorkflowEvent wrapProcessInstanceEvent(
            HistoricProcessInstanceEventEntity processInstanceEvent,
            String bpmnProcessName,
            String bpmnProcessVersion
    ) {

        final String workflowModuleId = processInstanceEvent.getTenantId();

        WorkflowEvent workflowEvent;

        if (processInstanceEvent.isEventOfType(HistoryEventTypes.PROCESS_INSTANCE_START)) {

            WorkflowCreatedEvent workflowCreatedEvent = new WorkflowCreatedEvent(
                    workflowModuleId,
                    cockpitProperties.getI18nLanguages()
            );

            fillWorkflowCreatedEvent(processInstanceEvent, bpmnProcessName, bpmnProcessVersion, workflowCreatedEvent);
            workflowEvent = workflowCreatedEvent;

        } else if (processInstanceEvent.isEventOfType(HistoryEventTypes.PROCESS_INSTANCE_END)) {

            WorkflowLifecycleEvent workflowLifecycleEvent = processInstanceEvent.getDeleteReason() != null ?
                    new WorkflowCancelledEvent() :
                    new WorkflowCompletedEvent();

            fillLifecycleEvent(bpmnProcessId, bpmnProcessVersion, processInstanceEvent, workflowLifecycleEvent);
            workflowEvent = workflowLifecycleEvent;

        } else {

            WorkflowUpdatedEvent workflowUpdatedEvent = new WorkflowUpdatedEvent(
                    workflowModuleId,
                    cockpitProperties.getI18nLanguages());
            fillWorkflowCreatedEvent(processInstanceEvent, bpmnProcessName, bpmnProcessVersion, workflowUpdatedEvent);
            workflowEvent = workflowUpdatedEvent;
        }

        return workflowEvent;

    }


    private void fillWorkflowCreatedEvent(HistoricProcessInstance processInstance, String bpmnProcessName, String bpmnProcessVersion, WorkflowCreatedEvent workflowCreatedEvent) {
        final var bpmnProcessId = processInstance.getProcessDefinitionKey();

        final var prefilledWorkflowDetails = prefillWorkflowDetails(
                bpmnProcessId,
                bpmnProcessVersion,
                bpmnProcessName,
                processInstance,
                workflowCreatedEvent);

        final var details = callWorkflowDetailsProviderMethod(
                processInstance.getId(),
                processInstance.getId(),
                processInstance.getBusinessKey(),
                (PrefilledWorkflowDetails) prefilledWorkflowDetails);

        fillWorkflowDetailsByCustomDetails(
                bpmnProcessId,
                bpmnProcessVersion,
                bpmnProcessName,
                workflowCreatedEvent,
                details == null
                        ? prefilledWorkflowDetails
                        : details);
    }

    private void fillWorkflowCreatedEvent(
            HistoricProcessInstanceEventEntity processInstanceEvent,
            String bpmnProcessName,
            String bpmnProcessVersion,
            WorkflowCreatedEvent workflowCreatedEvent
    ) {


        final var bpmnProcessId = processInstanceEvent.getProcessDefinitionKey();

        final var prefilledWorkflowDetails = prefillWorkflowDetails(
                bpmnProcessId,
                bpmnProcessVersion,
                bpmnProcessName,
                processInstanceEvent,
                workflowCreatedEvent);

        final var details = callWorkflowDetailsProviderMethod(
                processInstanceEvent.getProcessInstanceId(),
                processInstanceEvent.getExecutionId(),
                processInstanceEvent.getBusinessKey(),
                (PrefilledWorkflowDetails) prefilledWorkflowDetails);

        fillWorkflowDetailsByCustomDetails(
                bpmnProcessId,
                bpmnProcessVersion,
                bpmnProcessName,
                workflowCreatedEvent,
                details == null
                        ? prefilledWorkflowDetails
                        : details);
    }

    private void fillWorkflowDetailsByCustomDetails(
            final String bpmnProcessId,
            final String bpmnProcessVersion,
            final String bpmnProcessName,
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
            event.setTitle(details.getTitle());
        }
        
        event.setUiUriPath(
                details.getUiUriPath());
        
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
                        () -> details.getTitle(),
                        () -> event.getTitle(),
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

    private WorkflowCreatedEvent prefillWorkflowDetails(
            final String bpmnProcessId,
            final String bpmnProcessVersion,
            final String bpmnProcessName,
            final HistoricProcessInstanceEventEntity processInstanceEvent,
            final WorkflowCreatedEvent event) {

        final var prefilledWorkflowDetails = event;

        prefilledWorkflowDetails.setId(
                System.nanoTime()
                        + "@"
                        + processInstanceEvent.getProcessInstanceId()
                        + "#"
                        + processInstanceEvent.getId());
        prefilledWorkflowDetails.setTimestamp(OffsetDateTime.now());
        prefilledWorkflowDetails.setBpmnProcessId(bpmnProcessId);
        prefilledWorkflowDetails.setBpmnProcessVersion(bpmnProcessVersion);
        prefilledWorkflowDetails.setWorkflowId(processInstanceEvent.getProcessInstanceId());
        prefilledWorkflowDetails.setBusinessId(processInstanceEvent.getBusinessKey());
        prefilledWorkflowDetails.setTitle(new HashMap<>());

        return prefilledWorkflowDetails;

    }

    private WorkflowCreatedEvent prefillWorkflowDetails(
            final String bpmnProcessId,
            final String bpmnProcessVersion,
            final String bpmnProcessName,
            final HistoricProcessInstance processInstance,
            final WorkflowCreatedEvent event) {

        final var prefilledWorkflowDetails = event;

        prefilledWorkflowDetails.setId(
                System.nanoTime()
                        + "@"
                        + processInstance.getId());
        prefilledWorkflowDetails.setTimestamp(OffsetDateTime.now());
        prefilledWorkflowDetails.setBpmnProcessId(bpmnProcessId);
        prefilledWorkflowDetails.setBpmnProcessVersion(bpmnProcessVersion);
        prefilledWorkflowDetails.setWorkflowId(processInstance.getId());
        prefilledWorkflowDetails.setBusinessId(processInstance.getBusinessKey());
        prefilledWorkflowDetails.setTitle(new HashMap<>());

        return prefilledWorkflowDetails;

    }

    private void fillLifecycleEvent(
            final String bpmnProcessId,
            final String bpmnProcessVersion,
            final HistoricProcessInstanceEventEntity processInstanceEvent,
            final WorkflowLifecycleEvent event) {

        event.setId(
                System.nanoTime()
                        + "@"
                        + processInstanceEvent.getProcessInstanceId()
                        + "#"
                        + processInstanceEvent.getId());
        event.setComment(processInstanceEvent.getDeleteReason());
        event.setTimestamp(OffsetDateTime.now());
        event.setWorkflowId(processInstanceEvent.getProcessInstanceId());
        event.setInitiator(null); // TODO
        event.setBpmnProcessId(bpmnProcessId);
        event.setBpmnProcessVersion(bpmnProcessVersion);

    }

    private void fillLifecycleEvent(
            final String bpmnProcessId,
            final String bpmnProcessVersion,
            final HistoricProcessInstance processInstance,
            final WorkflowLifecycleEvent event) {

        event.setId(
                System.nanoTime()
                        + "@"
                        + processInstance.getId());
        event.setComment(processInstance.getDeleteReason());
        event.setTimestamp(OffsetDateTime.now());
        event.setWorkflowId(processInstance.getId());
        event.setInitiator(null); // TODO
        event.setBpmnProcessId(bpmnProcessId);
        event.setBpmnProcessVersion(bpmnProcessVersion);

    }

    @SuppressWarnings("unchecked")
    private WorkflowDetails callWorkflowDetailsProviderMethod(
            final String processInstanceId,
            final String executionId,
            final String businessKey,
            final PrefilledWorkflowDetails prefilledWorkflowDetails) {
        
        try {
   
            logger.trace("Will handle workflow '{}' ('{}') by execution '{}'",
                    processInstanceId,
                    bpmnProcessId,
                    executionId);
            
            final var workflowAggregateId = parseWorkflowAggregateIdFromBusinessKey
                    .apply(businessKey);
            
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

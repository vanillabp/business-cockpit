package io.vanillabp.cockpit.adapter.camunda7.workflow;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.vanillabp.cockpit.adapter.camunda7.usertask.Camunda7UserTaskHandler;
import io.vanillabp.cockpit.adapter.camunda7.workflow.events.WorkflowCompleted;
import io.vanillabp.cockpit.adapter.camunda7.workflow.events.WorkflowCreated;
import io.vanillabp.cockpit.adapter.camunda7.workflow.events.WorkflowUpdated;
import io.vanillabp.cockpit.adapter.camunda7.workflow.publishing.ProcessWorkflowEvent;
import io.vanillabp.cockpit.adapter.camunda7.workflow.publishing.WorkflowEvent;
import io.vanillabp.cockpit.adapter.common.CockpitProperties;
import io.vanillabp.cockpit.adapter.common.workflow.WorkflowHandlerBase;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCompletedEvent;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.commons.rest.adapter.versioning.ApiVersionAware;
import io.vanillabp.cockpit.commons.utils.DateTimeUtil;
import io.vanillabp.spi.cockpit.workflow.PrefilledWorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetails;
import io.vanillabp.springboot.adapter.AdapterAwareProcessService;
import io.vanillabp.springboot.adapter.wiring.WorkflowAggregateCache;
import io.vanillabp.springboot.parameters.MethodParameter;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.impl.history.event.HistoricProcessInstanceEventEntity;
import org.camunda.bpm.engine.impl.history.event.HistoryEventTypes;
import org.camunda.bpm.engine.task.IdentityLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.repository.CrudRepository;

public class Camunda7WorkflowHandler extends WorkflowHandlerBase {


    private static final Logger logger = LoggerFactory.getLogger(Camunda7WorkflowHandler.class);

    private final CockpitProperties cockpitProperties;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final ApiVersionAware bpmsApiVersionAware;

    private final AdapterAwareProcessService<?> processService;

    private final String bpmnProcessId;

    private Function<String, Object> parseWorkflowAggregateIdFromBusinessKey;


    public Camunda7WorkflowHandler(
            final CockpitProperties cockpitProperties,
            final ApplicationEventPublisher applicationEventPublisher,
            final ApiVersionAware bpmsApiVersionAware,
            final AdapterAwareProcessService<?> processService,
            final String bpmnProcessId,
            final CrudRepository<Object, Object> workflowAggregateRepository,
            final Object bean,
            final Method method,
            final List<MethodParameter> parameters) {
        
        super(workflowAggregateRepository, bean, method, parameters);
        this.cockpitProperties = cockpitProperties;
        this.applicationEventPublisher = applicationEventPublisher;
        this.processService = processService;
        this.bpmnProcessId = bpmnProcessId;
        this.bpmsApiVersionAware = bpmsApiVersionAware;

        determineBusinessKeyToIdMapper();
        
    }

    @Override
    protected Logger getLogger() {
        
        return logger;
        
    }




    public void listenProcessInstanceEvent(HistoricProcessInstanceEventEntity processInstanceEvent,
                                           String bpmnProcessName,
                                           String bpmnProcessVersion,
                                           String bpmnProcessVersionTag) {

        final String workflowModuleId = processInstanceEvent.getTenantId();

        io.vanillabp.cockpit.adapter.common.workflow.EventWrapper eventWrapper = null;
        if (processInstanceEvent.isEventOfType(HistoryEventTypes.PROCESS_INSTANCE_START)) {
            eventWrapper = new WorkflowCreated(
                    new WorkflowCreatedOrUpdatedEvent(),
                    workflowModuleId,
                    cockpitProperties.getI18nLanguages()
            );
        }

        if (processInstanceEvent.isEventOfType(HistoryEventTypes.PROCESS_INSTANCE_UPDATE)) {
            eventWrapper = new WorkflowUpdated(
                    new WorkflowCreatedOrUpdatedEvent(),
                    workflowModuleId,
                    cockpitProperties.getI18nLanguages()
            );
        }

        // migrate

        if (processInstanceEvent.isEventOfType(HistoryEventTypes.PROCESS_INSTANCE_END)) {
            eventWrapper = new WorkflowCompleted(
                    new WorkflowCompletedEvent()
            );
        }
        if (eventWrapper == null) {
            throw new RuntimeException(
                    "Unsupported process instance event type '"
                            + processInstanceEvent.getEventType()
                            + "'!");
        }

        if (eventWrapper instanceof WorkflowCreated) {

            final var workflowCreatedEvent = (WorkflowCreated) eventWrapper;


            final var bpmnProcessId = processInstanceEvent.getProcessDefinitionKey();

            //final ProcessDefinition processDefinition = repositoryService.getProcessDefinition(
            //      processInstanceEvent.getProcessDefinitionId());
            //final String bpmnProcessVersionTag = processDefinition.getVersionTag();
            //final String bpmnProcessVersion = Integer.toString(processDefinition.getVersion());
            //final var bpmnProcessName = StringUtils.hasText(processDefinition.getName())
            //        ? processDefinition.getName()
            //        : processDefinition.getKey();

            final var prefilledWorkflowDetails = prefillWorkflowDetails(
                    bpmnProcessId,
                    bpmnProcessVersionTag,
                    bpmnProcessVersion,
                    bpmnProcessName,
                    processInstanceEvent,
                    workflowCreatedEvent);

            final var details = callWorkflowDetailsProviderMethod(
                    processInstanceEvent,
                    (PrefilledWorkflowDetails) prefilledWorkflowDetails);

            fillWorkflowDetailsByCustomDetails(
                    workflowCreatedEvent,
                    details == null
                            ? prefilledWorkflowDetails
                            : details);

        } else {

            fillLifecycleEvent(processInstanceEvent, eventWrapper);

        }

        applicationEventPublisher.publishEvent(
                new WorkflowEvent(
                        Camunda7UserTaskHandler.class,
                        eventWrapper.getEvent(),
                        bpmsApiVersionAware.getApiVersion()));
        applicationEventPublisher.publishEvent(
                new ProcessWorkflowEvent(
                        Camunda7WorkflowEventSpringListener.class));

    }

    private void fillWorkflowDetailsByCustomDetails(
            WorkflowCreated event,
            WorkflowDetails details
    ) {
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
            event.setWorkflowTitle(details.getWorkflowTitle());
            event.setTitle(details.getTitle());
        }
    }

    private WorkflowCreated prefillWorkflowDetails(
            final String bpmnProcessId,
            final String bpmnProcessVersionTag,
            final String bpmnProcessVersion,
            final String bpmnProcessName,
            final HistoricProcessInstanceEventEntity processInstanceEvent,
            final WorkflowCreated event) {

        final var prefilledWorkflowDetails = event;

        // TODO: bpmnDescriptionLanguage leveled to workflows
        final var language = "de";

        prefilledWorkflowDetails.setId(
                System.nanoTime()
                        + "@"
                        + processInstanceEvent.getProcessInstanceId()
                        + "#"
                        + processInstanceEvent.getId());
        prefilledWorkflowDetails.setTimestamp(
                DateTimeUtil.fromDate(processInstanceEvent.getStartTime()));
        prefilledWorkflowDetails.setBpmnProcessId(bpmnProcessId);
        prefilledWorkflowDetails.setBpmnProcessVersionTag(bpmnProcessVersionTag);
        prefilledWorkflowDetails.setBpmnProcessVersion(bpmnProcessVersion);
        prefilledWorkflowDetails.setWorkflowId(processInstanceEvent.getProcessInstanceId());
        prefilledWorkflowDetails.setWorkflowAggregateId(processInstanceEvent.getBusinessKey());
        prefilledWorkflowDetails.setTitle(Map.of(language, bpmnProcessName));
        prefilledWorkflowDetails.setWorkflowTitle(Map.of(language, bpmnProcessName));

        return prefilledWorkflowDetails;

    }

    private void fillLifecycleEvent(
            final HistoricProcessInstanceEventEntity processInstanceEvent,
            final io.vanillabp.cockpit.adapter.common.workflow.EventWrapper event) {

        event.setId(
                System.nanoTime()
                        + "@"
                        + processInstanceEvent.getProcessInstanceId()
                        + "#"
                        + processInstanceEvent.getId());
        event.setComment(
                processInstanceEvent.getDeleteReason());
        event.setTimestamp(
                // TODO GWI
                DateTimeUtil.fromDate(processInstanceEvent.getStartTime()));

    }

    @SuppressWarnings("unchecked")
    private WorkflowDetails callWorkflowDetailsProviderMethod(
            final HistoricProcessInstanceEventEntity processInstanceEvent,
            final PrefilledWorkflowDetails prefilledWorkflowDetails) {
        
        // final var multiInstanceCache = new Map[] { null };
   
        try {
   
            logger.trace("Will handle workflow '{}' ('{}') by execution '{}'",
                    processInstanceEvent.getProcessInstanceId(),
                    bpmnProcessId,
                    processInstanceEvent.getExecutionId());
            
            // final var execution = delegateTask.getExecution();
            
            /*final Function<String, Object> multiInstanceSupplier = multiInstanceActivity -> {
                if (multiInstanceCache[0] == null) {
                    multiInstanceCache[0] = Camunda7WorkflowHandler.getMultiInstanceContext(execution);
                }
                return multiInstanceCache[0].get(multiInstanceActivity);
            };*/
            
            final var workflowAggregateId = parseWorkflowAggregateIdFromBusinessKey
                    .apply(processInstanceEvent.getBusinessKey());
            
            final var workflowAggregateCache = new WorkflowAggregateCache();
            
            return super.execute(
                    workflowAggregateCache,
                    workflowAggregateId,
                    true,

                    (args, param) -> processPrefilledWorkflowDetailsParameter(
                            args,
                            param,
                            () -> prefilledWorkflowDetails)
                    //(args, param) -> processMultiInstanceIndexParameter(
                    //        args,
                    //        param,
                    //        multiInstanceSupplier),
                    //(args, param) -> processMultiInstanceTotalParameter(
                    //        args,
                    //        param,
                    //        multiInstanceSupplier),
                    //(args, param) -> processMultiInstanceElementParameter(
                    //        args,
                    //        param,
                    //        multiInstanceSupplier),
                    //(args, param) -> processMultiInstanceResolverParameter(
                    //        args,
                    //        param,
                    //        () -> {
                    //            if (workflowAggregateCache.workflowAggregate == null) {
                    //                workflowAggregateCache.workflowAggregate = workflowAggregateRepository
                    //                        .findById(workflowAggregateId)
                    //                        .orElseThrow();
                    //            }
                    //            return workflowAggregateCache.workflowAggregate;
                    //        }, multiInstanceSupplier)
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
    
    private Map.Entry<List<String>, List<String>> readCandidates(
            final DelegateTask delegateTask) {

        final var groups = new LinkedList<String>();
        final var users = delegateTask
                .getCandidates()
                .stream()
                .filter(candidate -> {
                    if (candidate.getGroupId() != null) {
                        groups.add(candidate.getGroupId());
                        return false;
                    }
                    return true;
                })
                .filter(candidate -> candidate.getUserId() != null)
                .map(IdentityLink::getUserId)
                .collect(Collectors.toList());
        
        return Map.entry(users, groups);

    }

}

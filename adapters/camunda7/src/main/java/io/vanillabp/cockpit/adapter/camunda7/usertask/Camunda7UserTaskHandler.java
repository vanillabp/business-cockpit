package io.vanillabp.cockpit.adapter.camunda7.usertask;

import freemarker.template.Configuration;
import io.vanillabp.cockpit.adapter.camunda7.service.UserTaskImpl;
import io.vanillabp.cockpit.adapter.camunda7.usertask.publishing.ProcessUserTaskEvent;
import io.vanillabp.cockpit.adapter.camunda7.usertask.publishing.UserTaskEvent;
import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.usertask.UserTaskHandlerBase;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCancelledEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCompletedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskEventImpl;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskLifecycleEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskUpdatedEvent;
import io.vanillabp.cockpit.commons.utils.DateTimeUtil;
import io.vanillabp.spi.cockpit.details.DetailsEvent;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTask;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.spi.service.MultiInstanceElementResolver;
import io.vanillabp.springboot.adapter.AdapterAwareProcessService;
import io.vanillabp.springboot.adapter.MultiInstance;
import io.vanillabp.springboot.adapter.wiring.WorkflowAggregateCache;
import io.vanillabp.springboot.parameters.MethodParameter;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;
import org.camunda.bpm.engine.task.IdentityLink;
import org.camunda.bpm.model.bpmn.instance.Activity;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.MultiInstanceLoopCharacteristics;
import org.camunda.bpm.model.xml.ModelInstance;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.repository.CrudRepository;
import org.springframework.util.StringUtils;

public class Camunda7UserTaskHandler extends UserTaskHandlerBase {

    private static final Logger logger = LoggerFactory.getLogger(Camunda7UserTaskHandler.class);
    
    private final ApplicationEventPublisher applicationEventPublisher;

    private final AdapterAwareProcessService<?> processService;
    
    private final String bpmnProcessId;
    
    private Function<String, Object> parseWorkflowAggregateIdFromBusinessKey;
    
    private final String taskDefinition;
    
    public Camunda7UserTaskHandler(
            final String taskDefinition,
            final VanillaBpCockpitProperties properties,
            final ApplicationEventPublisher applicationEventPublisher,
            final Optional<Configuration> templating,
            final String bpmnProcessId,
            final AdapterAwareProcessService<?> processService,
            final CrudRepository<Object, Object> workflowAggregateRepository,
            final Object bean,
            final Method method,
            final List<MethodParameter> parameters) {
        
        super(properties, templating, workflowAggregateRepository, bean, method, parameters);
        this.taskDefinition = taskDefinition;
        this.applicationEventPublisher = applicationEventPublisher;
        this.processService = processService;
        this.bpmnProcessId = bpmnProcessId;

        if (this.templating.isEmpty()) {
            try {
                properties
                        .getBpmnDescriptionLanguage(processService.getWorkflowModuleId(), bpmnProcessId);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Since templating is inactive the language used for texts in BPMN needs to be defined!", e);
            }
        }

        determineBusinessKeyToIdMapper();
        
    }

    @Override
    protected Logger getLogger() {
        
        return logger;
        
    }

    public void notify(
            final TaskEntity task,
            final String eventName) {

        final var workflowModuleId = processService.getWorkflowModuleId();
        final var i18nLanguages = properties.getI18nLanguages(workflowModuleId, bpmnProcessId);
        final io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskEvent userTaskEvent =
                switch (eventName) {
                    case TaskListener.EVENTNAME_CREATE:
                        UserTaskEventImpl userTaskCreatedEvent = new UserTaskEventImpl(workflowModuleId, i18nLanguages);
                        fillUserTaskCreatedEvent(task, userTaskCreatedEvent);
                        yield userTaskCreatedEvent;

                    case TaskListener.EVENTNAME_UPDATE:
                        UserTaskUpdatedEvent userTaskUpdatedEvent = new UserTaskUpdatedEvent(workflowModuleId, i18nLanguages);
                        fillUserTaskCreatedEvent(task, userTaskUpdatedEvent);
                        yield userTaskUpdatedEvent;

                    case TaskListener.EVENTNAME_COMPLETE:
                        UserTaskCompletedEvent userTaskCompletedEvent = new UserTaskCompletedEvent(workflowModuleId, i18nLanguages);
                        fillUserTaskCreatedEvent(task, userTaskCompletedEvent);
                        yield userTaskCompletedEvent;

                    case TaskListener.EVENTNAME_DELETE:
                        UserTaskCancelledEvent userTaskCancelledEvent = new UserTaskCancelledEvent(workflowModuleId, i18nLanguages);
                        fillUserTaskCreatedEvent(task, userTaskCancelledEvent);
                        yield userTaskCancelledEvent;

                    default: throw new RuntimeException(
                            "Unsupported event type '"
                            + task.getEventName()
                            + "'!");
                    };

        applicationEventPublisher.publishEvent(
                new UserTaskEvent(
                        Camunda7UserTaskHandler.class,
                        userTaskEvent));
        applicationEventPublisher.publishEvent(
                new ProcessUserTaskEvent(
                        Camunda7UserTaskHandler.class));

    }

    public UserTask getUserTask(
            final TaskEntity task) {

        final var userTaskCreatedEvent = new UserTaskEventImpl(task.getTenantId(), properties.getI18nLanguages(
                processService.getWorkflowModuleId(), bpmnProcessId));
        fillUserTaskCreatedEvent(task, userTaskCreatedEvent);
        return new UserTaskImpl(userTaskCreatedEvent);

    }

    private void fillUserTaskCreatedEvent(TaskEntity task, UserTaskEventImpl userTaskCreatedEvent) {

        final var execution = (ExecutionEntity) task.getExecution();
        final var processDefinition = execution
                .getProcessDefinition();

        final var bpmnProcessId = processDefinition
                .getKey();
        final String bpmnProcessVersion;
        if (StringUtils.hasText(processDefinition.getVersionTag())) {
            bpmnProcessVersion = processDefinition.getVersionTag()
                    + ":"
                    + Integer.toString(processDefinition.getVersion());
        } else {
            bpmnProcessVersion = Integer.toString(processDefinition.getVersion());
        }
        final var bpmnProcessName = StringUtils.hasText(processDefinition.getName())
                ? processDefinition.getName()
                : processDefinition.getKey();

        final var prefilledUserTaskDetails = prefillUserTaskDetails(
                bpmnProcessId,
                bpmnProcessVersion,
                task,
                userTaskCreatedEvent);

        final var details = callUserTaskDetailsProviderMethod(
                task,
                (PrefilledUserTaskDetails) prefilledUserTaskDetails);

        fillUserTaskDetailsByCustomDetails(
                bpmnProcessId,
                bpmnProcessVersion,
                bpmnProcessName,
                task,
                userTaskCreatedEvent,
                details == null
                        ? prefilledUserTaskDetails
                        : details);
    }

    private void fillUserTaskDetailsByCustomDetails(
            final String bpmnProcessId,
            final String bpmnProcessVersion,
            final String bpmnProcessName,
            final TaskEntity delegateTask,
            final UserTaskEventImpl event,
            final UserTaskDetails details) {
        
        // a different object was returned then provided
        if (details != event) {
            
            event.setInitiator(
                    details.getInitiator());
            event.setComment(
                    details.getComment());
            event.setAssignee(
                    details.getAssignee());
            event.setCandidateUsers(
                    details.getCandidateUsers());
            event.setCandidateGroups(
                    details.getCandidateGroups());
            event.setDueDate(
                    details.getDueDate());
            event.setFollowUpDate(
                    details.getFollowUpDate());
            event.setDetails(
                    details.getDetails());
            event.setI18nLanguages(
                    details.getI18nLanguages());
            
        }
        
        event.setUiUriPath(
                details.getUiUriPath());
        
        if ((event.getI18nLanguages() == null)
                || event.getI18nLanguages().isEmpty()){
            event.setI18nLanguages(
                     properties.getI18nLanguages(processService.getWorkflowModuleId(), bpmnProcessId));
        }
        
        if (templating.isEmpty()) {

            final var language = properties.getBpmnDescriptionLanguage(processService.getWorkflowModuleId(), bpmnProcessId);

            if ((details.getWorkflowTitle() == null)
                    || details.getWorkflowTitle().isEmpty()) {
                event.setWorkflowTitle(Map.of(
                        language,
                        bpmnProcessName));
            }
            if ((details.getTitle() == null)
                    || details.getTitle().isEmpty()) {
                event.setTitle(Map.of(
                        language,
                        delegateTask.getName()));
            }
            if ((details.getTaskDefinitionTitle() == null)
                    || details.getTaskDefinitionTitle().isEmpty()) {
                event.setTaskDefinitionTitle(Map.of(
                        language,
                        delegateTask.getName()));
            }
            if ((details.getDetailsFulltextSearch() == null)
                    || !StringUtils.hasText(details.getDetailsFulltextSearch())) {
                event.setDetailsFulltextSearch(
                        delegateTask.getName());
            }
            
        } else {

            final var templatesPathes = List.of(
                    properties.getTemplatePath(processService.getWorkflowModuleId(), bpmnProcessId, taskDefinition),
                    properties.getTemplatePath(processService.getWorkflowModuleId(), bpmnProcessId),
                    properties.getTemplatePath(processService.getWorkflowModuleId())
                );

            event
                    .getI18nLanguages()
                    .forEach(language -> {
                        final var locale = Locale.forLanguageTag(language);

                        final BiFunction<String, Exception, Object[]> errorLoggingContext
                                = (name, e) -> new Object[] {
                                    name,
                                    event.getTaskDefinition(),
                                    event.getWorkflowId(),
                                    e
                                };
                        
                        setTextInEvent(
                                language,
                                locale,
                                "workflow-title.ftl",
                                details::getWorkflowTitle,
                                event::getWorkflowTitle,
                                event::setWorkflowTitle,
                                bpmnProcessName,
                                templatesPathes,
                                details.getTemplateContext(),
                                errorLoggingContext);

                        setTextInEvent(
                                language,
                                locale,
                                "title.ftl",
                                details::getTitle,
                                event::getTitle,
                                event::setTitle,
                                delegateTask.getName(),
                                templatesPathes,
                                details.getTemplateContext(),
                                errorLoggingContext);
                        
                        setTextInEvent(
                                language,
                                locale,
                                "task-definition-title.ftl",
                                details::getTaskDefinitionTitle,
                                event::getTaskDefinitionTitle,
                                event::setTaskDefinitionTitle,
                                delegateTask.getName(),
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
                                        delegateTask::getName));
                        
                    });

        }
        
    }
            
    private UserTaskEventImpl prefillUserTaskDetails(
            final String bpmnProcessId,
            final String bpmnProcessVersion,
            final DelegateTask delegateTask,
            final UserTaskEventImpl event) {
        
        final var prefilledUserTaskDetails = event;

        prefilledUserTaskDetails.setEventId(
                System.nanoTime()
                + "@"
                + delegateTask.getProcessInstanceId()
                + "#"
                + delegateTask.getId());
        prefilledUserTaskDetails.setUserTaskId(
                delegateTask.getId());
        prefilledUserTaskDetails.setTimestamp(OffsetDateTime.now());
        prefilledUserTaskDetails.setBpmnProcessId(
                bpmnProcessId);
        prefilledUserTaskDetails.setBpmnProcessVersion(
                bpmnProcessVersion);
        prefilledUserTaskDetails.setTaskDefinition(
                taskDefinition);
        prefilledUserTaskDetails.setBusinessId(
                delegateTask.getExecution().getBusinessKey());
        prefilledUserTaskDetails.setBpmnTaskId(
                delegateTask.getTaskDefinitionKey());
        
        var superExecution = (ExecutionEntity) delegateTask.getExecution();
        while ((superExecution.getParentId() != null)
                || (superExecution.getSuperExecution() != null)) {
            if (superExecution.getSuperExecution() != null) {
                superExecution = superExecution.getSuperExecution();
            } else {
                superExecution = superExecution.getParent();
            }
        }
        prefilledUserTaskDetails.setWorkflowId(
                superExecution.getProcessInstanceId());
        prefilledUserTaskDetails.setSubWorkflowId(
                delegateTask.getProcessInstanceId());
        
        prefilledUserTaskDetails.setTitle(new HashMap<>());
        prefilledUserTaskDetails.setWorkflowTitle(new HashMap<>());
        prefilledUserTaskDetails.setTaskDefinitionTitle(new HashMap<>());

        prefilledUserTaskDetails.setAssignee(
                delegateTask.getAssignee());
        final var candidates = readCandidates(delegateTask);
        prefilledUserTaskDetails.setCandidateUsers(
                candidates.getKey());
        prefilledUserTaskDetails.setCandidateGroups(
                candidates.getValue());
        prefilledUserTaskDetails.setDueDate(
                DateTimeUtil.fromDate(delegateTask.getDueDate()));
        prefilledUserTaskDetails.setFollowUpDate(
                DateTimeUtil.fromDate(delegateTask.getFollowUpDate()));
        
        return prefilledUserTaskDetails;
        
    }

    private void fillLifecycleEvent(
            final String workflowModuleId,
            final DelegateTask delegateTask,
            final UserTaskLifecycleEvent event) {
        
        event.setEventId(
                System.nanoTime()
                + "@"
                + delegateTask.getProcessInstanceId()
                + "#"
                + delegateTask.getId());
        event.setWorkflowModuleId(workflowModuleId);
        event.setComment(
                delegateTask.getDeleteReason());
        event.setTimestamp(OffsetDateTime.now());
        event.setUserTaskId(
                delegateTask.getId());
    }

    @SuppressWarnings("unchecked")
    private UserTaskDetails callUserTaskDetailsProviderMethod(
            final DelegateTask delegateTask,
            final PrefilledUserTaskDetails prefilledUserTaskDetails) {
        
        final var multiInstanceCache = new Map[] { null };
   
        try {
   
            logger.trace("Will handle user-task '{}' of workflow '{}' ('{}') by execution '{}'",
                    delegateTask.getBpmnModelElementInstance().getId(),
                    delegateTask.getProcessInstanceId(),
                    bpmnProcessId,
                    delegateTask.getExecutionId());
            
            final var execution = delegateTask.getExecution();
            
            final Function<String, Object> multiInstanceSupplier = multiInstanceActivity -> {
                if (multiInstanceCache[0] == null) {
                    multiInstanceCache[0] = Camunda7UserTaskHandler.getMultiInstanceContext(execution);
                }
                return multiInstanceCache[0].get(multiInstanceActivity);
            };
            
            final var workflowAggregateId = parseWorkflowAggregateIdFromBusinessKey
                    .apply(execution.getBusinessKey());
            
            final var workflowAggregateCache = new WorkflowAggregateCache();

            final var detailsEvent = switch (delegateTask.getEventName()) {
                case TaskListener.EVENTNAME_CREATE -> DetailsEvent.Event.CREATED;
                case TaskListener.EVENTNAME_COMPLETE -> DetailsEvent.Event.COMPLETED;
                case TaskListener.EVENTNAME_DELETE -> DetailsEvent.Event.CANCELED;
                default -> DetailsEvent.Event.UPDATED;
            };
            
            return super.execute(
                    workflowAggregateCache,
                    workflowAggregateId,
                    true,
                    (args, param) -> processTaskParameter(
                            args,
                            param,
                            execution::getVariableLocal),
                    (args, param) -> processTaskIdParameter(
                            args,
                            param,
                            delegateTask::getId),
                    (args, param) -> processDetailsEventParameter(
                            args,
                            param,
                            () -> detailsEvent),
                    (args, param) -> processPrefilledUserTaskDetailsParameter(
                            args,
                            param,
                            () -> prefilledUserTaskDetails),
                    (args, param) -> processMultiInstanceIndexParameter(
                            args,
                            param,
                            multiInstanceSupplier),
                    (args, param) -> processMultiInstanceTotalParameter(
                            args,
                            param,
                            multiInstanceSupplier),
                    (args, param) -> processMultiInstanceElementParameter(
                            args,
                            param,
                            multiInstanceSupplier),
                    (args, param) -> processMultiInstanceResolverParameter(
                            args,
                            param,
                            () -> {
                                if (workflowAggregateCache.workflowAggregate == null) {
                                    workflowAggregateCache.workflowAggregate = workflowAggregateRepository
                                            .findById(workflowAggregateId)
                                            .orElseThrow();
                                }
                                return workflowAggregateCache.workflowAggregate;
                            }, multiInstanceSupplier));
            
        } catch (RuntimeException e) {
   
            throw e;
   
        } catch (Exception e) {
   
            throw new RuntimeException(e);
   
        }
        
    }
    
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
    
    static Map<String, MultiInstanceElementResolver.MultiInstance<Object>> getMultiInstanceContext(
            final DelegateExecution execution) {

        final var result = new LinkedHashMap<String, MultiInstanceElementResolver.MultiInstance<Object>>();

        final var model = execution.getBpmnModelElementInstance().getModelInstance();

        DelegateExecution miExecution = execution;
        MultiInstanceLoopCharacteristics loopCharacteristics = null;
        // find multi-instance element from current element up to the root of the
        // process-hierarchy
        while (loopCharacteristics == null) {

            // check current element for multi-instance
            final var bpmnElement = getCurrentElement(model, miExecution);
            if (bpmnElement instanceof Activity) {
                loopCharacteristics = (MultiInstanceLoopCharacteristics) ((Activity) bpmnElement)
                        .getLoopCharacteristics();
            }

            // if still not found then check parent
            if (loopCharacteristics == null) {
                miExecution = miExecution.getParentId() != null
                        ? ((ExecutionEntity) miExecution).getParent()
                        : miExecution.getSuperExecution();
            }
            // multi-instance found
            else {
                final var itemNo = (Integer) miExecution.getVariable("loopCounter");
                final var totalCount = (Integer) miExecution.getVariable("nrOfInstances");
                final var currentItem = loopCharacteristics.getCamundaElementVariable() == null ? null
                        : miExecution.getVariable(loopCharacteristics.getCamundaElementVariable());

                result.put(((BaseElement) bpmnElement).getId(),
                        new MultiInstance<Object>(currentItem, totalCount, itemNo));

            }

            // if there is no parent then multi-instance task was used in a
            // non-multi-instance environment
            if ((miExecution == null) && (loopCharacteristics == null)) {
                throw new RuntimeException(
                        "No multi-instance context found for element '"
                        + execution.getBpmnModelElementInstance().getId()
                        + "' or its parents!");
            }

        }

        return result;

    }

    private static ModelElementInstance getCurrentElement(final ModelInstance model, DelegateExecution miExecution) {

        // if current element is known then simply use it
        if (miExecution.getBpmnModelElementInstance() != null) {
            return miExecution.getBpmnModelElementInstance();
        }

        // if execution belongs to an activity (e.g. embedded subprocess) then
        // parse activity-instance-id which looks like "[element-id]:[instance-id]"
        // (e.g. "Activity_14fom0j:29d7e405-9605-11ec-bc62-0242700b16f6")
        final var activityInstanceId = miExecution.getActivityInstanceId();
        final var elementMarker = activityInstanceId.indexOf(':');

        // if there is no marker then the execution does not belong to a specific
        // element
        if (elementMarker == -1) {
            return null;
        }

        return model.getModelElementById(activityInstanceId.substring(0, elementMarker));

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
                .map(IdentityLink::getUserId)
                .filter(Objects::nonNull)
                .toList();
        
        return Map.entry(users, groups);

    }

}

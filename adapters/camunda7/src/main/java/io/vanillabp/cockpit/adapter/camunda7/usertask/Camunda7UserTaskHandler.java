package io.vanillabp.cockpit.adapter.camunda7.usertask;

import java.io.File;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
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

import freemarker.template.Configuration;
import io.vanillabp.cockpit.adapter.camunda7.usertask.events.UserTaskCompleted;
import io.vanillabp.cockpit.adapter.camunda7.usertask.events.UserTaskCreated;
import io.vanillabp.cockpit.adapter.camunda7.usertask.events.UserTaskDeleted;
import io.vanillabp.cockpit.adapter.camunda7.usertask.events.UserTaskUpdated;
import io.vanillabp.cockpit.adapter.camunda7.usertask.publishing.ProcessUserTaskEvent;
import io.vanillabp.cockpit.adapter.camunda7.usertask.publishing.UserTaskEvent;
import io.vanillabp.cockpit.adapter.common.CockpitProperties;
import io.vanillabp.cockpit.adapter.common.usertask.EventWrapper;
import io.vanillabp.cockpit.adapter.common.usertask.UserTaskHandlerBase;
import io.vanillabp.cockpit.adapter.common.usertask.UserTaskProperties;
import io.vanillabp.cockpit.adapter.common.usertask.UserTasksProperties;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCancelledEvent;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCompletedEvent;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.commons.rest.adapter.versioning.ApiVersionAware;
import io.vanillabp.cockpit.commons.utils.DateTimeUtil;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.spi.service.MultiInstanceElementResolver;
import io.vanillabp.springboot.adapter.AdapterAwareProcessService;
import io.vanillabp.springboot.adapter.MultiInstance;
import io.vanillabp.springboot.adapter.VanillaBpProperties;
import io.vanillabp.springboot.adapter.wiring.WorkflowAggregateCache;
import io.vanillabp.springboot.parameters.MethodParameter;

public class Camunda7UserTaskHandler extends UserTaskHandlerBase {

    private static final Logger logger = LoggerFactory.getLogger(Camunda7UserTaskHandler.class);
    
    private final CockpitProperties properties;
    
    private final UserTaskProperties userTaskProperties;
    
    private final ApplicationEventPublisher applicationEventPublisher;
    
    private final ApiVersionAware bpmsApiVersionAware;
    
    private final AdapterAwareProcessService<?> processService;
    
    private final String bpmnProcessId;
    
    private Function<String, Object> parseWorkflowAggregateIdFromBusinessKey;
    
    private final String taskDefinition;
    
    public Camunda7UserTaskHandler(
            final String taskDefinition,
            final CockpitProperties properties,
            final UserTasksProperties workflowProperties,
            final UserTaskProperties userTaskProperties,
            final ApplicationEventPublisher applicationEventPublisher,
            final Optional<Configuration> templating,
            final ApiVersionAware bpmsApiVersionAware,
            final String bpmnProcessId,
            final AdapterAwareProcessService<?> processService,
            final CrudRepository<Object, Object> workflowAggregateRepository,
            final Object bean,
            final Method method,
            final List<MethodParameter> parameters) {
        
        super(workflowProperties, templating, workflowAggregateRepository, bean, method, parameters);
        this.taskDefinition = taskDefinition;
        this.properties = properties;
        this.userTaskProperties = userTaskProperties != null
                ? userTaskProperties
                : new UserTaskProperties();
        this.applicationEventPublisher = applicationEventPublisher;
        this.processService = processService;
        this.bpmnProcessId = bpmnProcessId;
        this.bpmsApiVersionAware = bpmsApiVersionAware;
        
        if (this.templating.isEmpty()
                && !StringUtils.hasText(workflowProperties.getBpmnDescriptionLanguage())
                && ((userTaskProperties == null)
                        || !StringUtils.hasText(workflowProperties.getBpmnDescriptionLanguage()))) {
                    
            throw new RuntimeException(
                    "Templating is inactive because property '"
                    + CockpitProperties.PREFIX
                    + ".template-loader-path' has no value. It is mandatory to set either '"
                    + VanillaBpProperties.PREFIX
                    + ".workflows[workflow-module-id="
                    + workflowProperties.getWorkflowModuleId()
                    + ", bpmn-process-id="
                    + workflowProperties.getBpmnProcessId()
                    + "].bpmn-description-language' or '"
                    + VanillaBpProperties.PREFIX
                    + ".workflows[workflow-module-id="
                    + workflowProperties.getWorkflowModuleId()
                    + ", bpmn-process-id="
                    + workflowProperties.getBpmnProcessId()
                    + "].user-tasks["
                    + taskDefinition
                    + "].bpmn-description-language'!");
        }
        
        determineBusinessKeyToIdMapper();
        
    }

    @Override
    protected Logger getLogger() {
        
        return logger;
        
    }

    public void notify(
            final DelegateTask delegateTask) {

        final EventWrapper eventWrapper =
                switch (delegateTask.getEventName()) {
                case TaskListener.EVENTNAME_CREATE -> new UserTaskCreated(
                        new UserTaskCreatedOrUpdatedEvent(),
                        delegateTask.getTenantId(),
                        properties.getI18nLanguages());
                case TaskListener.EVENTNAME_UPDATE -> new UserTaskUpdated(
                        new UserTaskCreatedOrUpdatedEvent(),
                        delegateTask.getTenantId(),
                        properties.getI18nLanguages());
                case TaskListener.EVENTNAME_COMPLETE -> new UserTaskCompleted(
                        new UserTaskCompletedEvent());
                case TaskListener.EVENTNAME_DELETE -> new UserTaskDeleted(
                        new UserTaskCancelledEvent());
                default -> throw new RuntimeException(
                        "Unsupported event type '"
                        + delegateTask.getEventName()
                        + "'!");
                };
        
        if (eventWrapper instanceof UserTaskCreated) {

            final var userTaskCreatedEvent = (UserTaskCreated) eventWrapper;

            final var execution = (ExecutionEntity) delegateTask.getExecution();
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
                    delegateTask,
                    userTaskCreatedEvent);
        
            final var details = callUserTaskDetailsProviderMethod(
                    delegateTask,
                    (PrefilledUserTaskDetails) prefilledUserTaskDetails);
            
            fillUserTaskDetailsByCustomDetails(
                    bpmnProcessId,
                    bpmnProcessVersion,
                    bpmnProcessName,
                    delegateTask,
                    userTaskCreatedEvent,
                    details == null
                            ? prefilledUserTaskDetails
                            : details);
            
        } else {
            
            fillLifecycleEvent(delegateTask, eventWrapper);
            
        }

        applicationEventPublisher.publishEvent(
                new UserTaskEvent(
                        Camunda7UserTaskHandler.class,
                        eventWrapper.getEvent(),
                        bpmsApiVersionAware.getApiVersion()));
        applicationEventPublisher.publishEvent(
                new ProcessUserTaskEvent(
                        Camunda7UserTaskHandler.class));

    }

    private void fillUserTaskDetailsByCustomDetails(
            final String bpmnProcessId,
            final String bpmnProcessVersion,
            final String bpmnProcessName,
            final DelegateTask delegateTask,
            final UserTaskCreated event,
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
                     properties.getI18nLanguages());
        }
        
        if (templating.isEmpty()) {

            final var language = StringUtils.hasText(userTaskProperties.getBpmnDescriptionLanguage())
                    ? userTaskProperties.getBpmnDescriptionLanguage()
                    : workflowProperties.getBpmnDescriptionLanguage();

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
                                () -> details.getWorkflowTitle(),
                                () -> event.getWorkflowTitle(),
                                bpmnProcessName,
                                details.getTemplateContext(),
                                errorLoggingContext);

                        setTextInEvent(
                                language,
                                locale,
                                StringUtils.hasText(userTaskProperties.getTemplatesPath())
                                        ? userTaskProperties.getTemplatesPath()
                                                + File.separator
                                                + "title.ftl"
                                        : "title.ftl",
                                () -> details.getTitle(),
                                () -> event.getTitle(),
                                delegateTask.getName(),
                                details.getTemplateContext(),
                                errorLoggingContext);
                        
                        setTextInEvent(
                                language,
                                locale,
                                StringUtils.hasText(userTaskProperties.getTemplatesPath())
                                ? userTaskProperties.getTemplatesPath()
                                        + File.separator
                                        + "task-definition-title.ftl"
                                : "task-definition-title.ftl",
                                () -> details.getTaskDefinitionTitle(),
                                () -> event.getTaskDefinitionTitle(),
                                delegateTask.getName(),
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
                                        () -> delegateTask.getName()));
                        
                    });

        }
        
    }
            
    private UserTaskCreated prefillUserTaskDetails(
            final String bpmnProcessId,
            final String bpmnProcessVersion,
            final DelegateTask delegateTask,
            final UserTaskCreated event) {
        
        final var prefilledUserTaskDetails = event;
        
        prefilledUserTaskDetails.setId(
                System.nanoTime()
                + "@"
                + delegateTask.getProcessInstanceId()
                + "#"
                + delegateTask.getId());
        prefilledUserTaskDetails.setUserTaskId(
                delegateTask.getId());
        prefilledUserTaskDetails.setTimestamp(
                DateTimeUtil.fromDate(delegateTask.getCreateTime()));
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
            final DelegateTask delegateTask,
            final EventWrapper event) {
        
        event.setId(
                System.nanoTime()
                + "@"
                + delegateTask.getProcessInstanceId()
                + "#"
                + delegateTask.getId());
        event.setComment(
                delegateTask.getDeleteReason());
        event.setTimestamp(
                DateTimeUtil.fromDate(delegateTask.getCreateTime()));
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
            
            return super.execute(
                    workflowAggregateCache,
                    workflowAggregateId,
                    true,
                    (args, param) -> processTaskParameter(
                            args,
                            param,
                            taskParameter -> execution.getVariableLocal(taskParameter)),
                    (args, param) -> processTaskIdParameter(
                            args,
                            param,
                            () -> delegateTask.getId()),
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
                .filter(candidate -> candidate.getUserId() != null)
                .map(IdentityLink::getUserId)
                .collect(Collectors.toList());
        
        return Map.entry(users, groups);

    }

}

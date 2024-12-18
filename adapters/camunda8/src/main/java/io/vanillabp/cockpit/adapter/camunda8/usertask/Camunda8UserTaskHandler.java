package io.vanillabp.cockpit.adapter.camunda8.usertask;

import freemarker.template.Configuration;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskLifecycleEvent;
import io.vanillabp.cockpit.adapter.camunda8.usertask.publishing.ProcessUserTaskEvent;
import io.vanillabp.cockpit.adapter.camunda8.workflow.persistence.ProcessInstanceEntity;
import io.vanillabp.cockpit.adapter.camunda8.workflow.persistence.ProcessInstanceRepository;
import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.usertask.UserTaskHandlerBase;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCancelledEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCompletedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskLifecycleEvent;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.springboot.adapter.AdapterAwareProcessService;
import io.vanillabp.springboot.adapter.wiring.WorkflowAggregateCache;
import io.vanillabp.springboot.parameters.MethodParameter;
import java.lang.reflect.Method;
import java.math.BigInteger;
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
import org.springframework.util.StringUtils;

public class Camunda8UserTaskHandler extends UserTaskHandlerBase {

    private static final Logger logger = LoggerFactory.getLogger(Camunda8UserTaskHandler.class);
    private final ApplicationEventPublisher applicationEventPublisher;
    private Function<String, Object> parseWorkflowAggregateIdFromBusinessKey;
    private final AdapterAwareProcessService<?> processService;
    private final ProcessInstanceRepository processInstanceRepository;
    private final String taskDefinition;
    private final String taskTitle;
    private final String bpmnProcessId;

    @Override
    protected Logger getLogger() {
        return logger;
    }

    public Camunda8UserTaskHandler(
            String taskDefinition,
            VanillaBpCockpitProperties cockpitProperties,
            ApplicationEventPublisher applicationEventPublisher,
            Optional<Configuration> templating,
            String bpmnProcessId,
            String taskTitle,
            AdapterAwareProcessService<?> processService,
            ProcessInstanceRepository processInstanceRepository,
            CrudRepository<Object, Object> workflowAggregateRepository,
            Object bean,
            Method method,
            List<MethodParameter> parameters) {

        super(cockpitProperties, templating, workflowAggregateRepository, bean, method, parameters);
        this.bpmnProcessId = bpmnProcessId;
        this.taskDefinition = taskDefinition;
        this.taskTitle = taskTitle;
        this.applicationEventPublisher = applicationEventPublisher;
        this.processService = processService;
        this.processInstanceRepository = processInstanceRepository;

        determineBusinessKeyToIdMapper();
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



    public void notify(Camunda8UserTaskLifecycleEvent camunda8UserTaskLifecycleEvent) {

        Camunda8UserTaskLifecycleEvent.Intent intent = camunda8UserTaskLifecycleEvent.getIntent();
        UserTaskLifecycleEvent userTaskEvent = switch (intent) {
            case COMPLETED -> new UserTaskCompletedEvent();
            case CANCELED -> new UserTaskCancelledEvent();
        };

        fillLifecycleEvent(userTaskEvent, camunda8UserTaskLifecycleEvent);
        publishEvent(userTaskEvent);
    }

    private void fillLifecycleEvent(UserTaskLifecycleEvent userTaskLifecycleEvent,
                                    Camunda8UserTaskLifecycleEvent camunda8UserTaskLifecycleEvent) {

        userTaskLifecycleEvent.setEventId(
                System.nanoTime()
                        + "@"
                        + camunda8UserTaskLifecycleEvent.getProcessInstanceKey()
                        + "#"
                        + camunda8UserTaskLifecycleEvent.getElementId());

        userTaskLifecycleEvent.setTimestamp(camunda8UserTaskLifecycleEvent.getTimestamp());

        userTaskLifecycleEvent.setUserTaskId(
                String.valueOf(camunda8UserTaskLifecycleEvent.getKey()));

//        userTaskLifecycleEvent.setComment(
//                camunda8UserTaskLifecycleEvent.getDeleteReason()
//        );
    }

    public void notify(Camunda8UserTaskCreatedEvent camunda8UserTaskCreatedEvent) {

        String workflowModuleId = processService.getWorkflowModuleId();
        final var i18nLanguages = properties.getI18nLanguages(workflowModuleId, bpmnProcessId);
        UserTaskCreatedEvent userTaskCreatedEvent = new UserTaskCreatedEvent(
                workflowModuleId,
                i18nLanguages
        );
        this.fillUserTaskCreatedEvent(camunda8UserTaskCreatedEvent, userTaskCreatedEvent);
        publishEvent(userTaskCreatedEvent);
    }

    private String getBusinessKeyFromProcessInstanceKey(
            long processInstanceKey){

        return processInstanceRepository
                .findById(processInstanceKey)
                .map(ProcessInstanceEntity::getBusinessKey)
                .orElseThrow();
    }

    private void fillUserTaskCreatedEvent(
            Camunda8UserTaskCreatedEvent camunda8UserTaskCreatedEvent,
            UserTaskCreatedEvent userTaskCreatedEvent) {

        String businessKey = this.getBusinessKeyFromProcessInstanceKey(
                camunda8UserTaskCreatedEvent.getProcessInstanceKey());

        prefillCreatedEvent(userTaskCreatedEvent, camunda8UserTaskCreatedEvent, businessKey);

        UserTaskDetails prefilledUserTaskDetails = this.callUserTaskDetailsProviderMethod(
                camunda8UserTaskCreatedEvent,
                userTaskCreatedEvent,
                businessKey
        );

        if(prefilledUserTaskDetails == null){
            prefilledUserTaskDetails = userTaskCreatedEvent;
        } else if(prefilledUserTaskDetails != userTaskCreatedEvent){
            addDataToPrefilledEvent(userTaskCreatedEvent, prefilledUserTaskDetails);
        }

        if ((userTaskCreatedEvent.getI18nLanguages() == null)
                || userTaskCreatedEvent.getI18nLanguages().isEmpty()){
            userTaskCreatedEvent.setI18nLanguages(
                    properties.getI18nLanguages(processService.getWorkflowModuleId(), bpmnProcessId)
            );
        }

        fillUserTaskDetailsByCustomDetails(
                camunda8UserTaskCreatedEvent,
                userTaskCreatedEvent,
                prefilledUserTaskDetails);
    }




    private void prefillCreatedEvent(UserTaskCreatedEvent userTaskCreatedEvent,
                                     Camunda8UserTaskCreatedEvent camunda8UserTaskCreatedEvent,
                                     String businessKey
        ) {

        // TODO: process instance key correct?
        userTaskCreatedEvent.setEventId(
                System.nanoTime() +
                        "@" +
                        camunda8UserTaskCreatedEvent.getProcessInstanceKey());

        userTaskCreatedEvent.setUserTaskId(
                String.valueOf(camunda8UserTaskCreatedEvent.getKey()));

        userTaskCreatedEvent.setTimestamp(
                camunda8UserTaskCreatedEvent.getTimestamp());

        userTaskCreatedEvent.setBpmnProcessId(
                camunda8UserTaskCreatedEvent.getBpmnProcessId());

        userTaskCreatedEvent.setBpmnProcessVersion(
                String.valueOf(camunda8UserTaskCreatedEvent.getWorkflowDefinitionVersion()));

        userTaskCreatedEvent.setTaskDefinition(taskDefinition);

        userTaskCreatedEvent.setBusinessId(businessKey);
        userTaskCreatedEvent.setBpmnTaskId(camunda8UserTaskCreatedEvent.getElementId());

        userTaskCreatedEvent.setWorkflowId(
                String.valueOf(camunda8UserTaskCreatedEvent.getProcessInstanceKey()));

        userTaskCreatedEvent.setSubWorkflowId(
                String.valueOf(camunda8UserTaskCreatedEvent.getProcessInstanceKey()));

        userTaskCreatedEvent.setTitle(new HashMap<>());
        userTaskCreatedEvent.setWorkflowTitle(new HashMap<>());
        userTaskCreatedEvent.setTaskDefinitionTitle(new HashMap<>());
    }


    @SuppressWarnings("unchecked")
    private UserTaskDetails callUserTaskDetailsProviderMethod(
            final Camunda8UserTaskCreatedEvent delegateTask,
            final PrefilledUserTaskDetails prefilledUserTaskDetails,
            final String businessKey) {
        try {

            Object parsedBusinessKey = parseWorkflowAggregateIdFromBusinessKey.apply(businessKey);

            logger.trace("Will handle user-task '{}' of workflow '{}' ('{}') by execution '{}'",
                    delegateTask.getElementId(),
                    delegateTask.getProcessInstanceKey(),
                    delegateTask.getBpmnProcessId(),
                    delegateTask.getElementInstanceKey());

            final var workflowAggregateCache = new WorkflowAggregateCache();

            return super.execute(
                    workflowAggregateCache,
                    parsedBusinessKey,
                    true,
                    (args, param) -> processTaskIdParameter(
                            args,
                            param,
                            () -> String.valueOf(delegateTask.getKey())),
                    (args, param) -> processPrefilledUserTaskDetailsParameter(
                            args,
                            param,
                            () -> prefilledUserTaskDetails));

        } catch (RuntimeException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(e);

        }

    }

    private void addDataToPrefilledEvent(UserTaskCreatedEvent event,
                                         UserTaskDetails details) {

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
        event.setExcludedCandidateUsers(
                details.getExcludedCandidateUsers());
        event.setDueDate(
                details.getDueDate());
        event.setFollowUpDate(
                details.getFollowUpDate());
        event.setDetails(
                details.getDetails());
        event.setI18nLanguages(
                details.getI18nLanguages());
        event.setUiUriPath(
                details.getUiUriPath());
    }

    private void fillUserTaskDetailsByCustomDetails(Camunda8UserTaskCreatedEvent camunda8UserTaskCreatedEvent, UserTaskCreatedEvent userTaskCreatedEvent, UserTaskDetails prefilledUserTaskDetails) {
        if (templating.isEmpty()) {
            fillUserTaskWithTemplatingDeactivated(
                    camunda8UserTaskCreatedEvent.getBpmnProcessId(),
                    taskTitle,
                    userTaskCreatedEvent,
                    prefilledUserTaskDetails
            );
        } else {
            fillUserTaskWithTemplatingActivated(
                    camunda8UserTaskCreatedEvent.getBpmnProcessId(),
                    taskTitle,
                    userTaskCreatedEvent,
                    prefilledUserTaskDetails
            );
        }
    }

    private void fillUserTaskWithTemplatingDeactivated(
            String bpmnProcessName,
            String taskName,
            UserTaskCreatedEvent event,
            UserTaskDetails details) {

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
                    taskName));
        }
        if ((details.getTaskDefinitionTitle() == null)
                || details.getTaskDefinitionTitle().isEmpty()) {
            event.setTaskDefinitionTitle(Map.of(
                    language,
                    taskName));
        }
        if ((details.getDetailsFulltextSearch() == null)
                || !StringUtils.hasText(details.getDetailsFulltextSearch())) {
            event.setDetailsFulltextSearch(
                    taskName);
        }
    }


    private void fillUserTaskWithTemplatingActivated(
            String bpmnProcessName,
            String taskName,
            UserTaskCreatedEvent event,
            UserTaskDetails details) {

        final var templatesPaths = List.of(
                properties.getTemplatePath(processService.getWorkflowModuleId(), bpmnProcessId, taskDefinition),
                properties.getTemplatePath(processService.getWorkflowModuleId(), bpmnProcessId));
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
                            taskName,
                            templatesPaths,
                            details.getTemplateContext(),
                            errorLoggingContext);

                    setTextInEvent(
                            language,
                            locale,
                            "title.ftl",
                            details::getTitle,
                            event::getTitle,
                            event::setTitle,
                            taskName,
                            templatesPaths,
                            details.getTemplateContext(),
                            errorLoggingContext);

                    setTextInEvent(
                            language,
                            locale,
                            "task-definition-title.ftl",
                            details::getTaskDefinitionTitle,
                            event::getTaskDefinitionTitle,
                            event::setTaskDefinitionTitle,
                            taskName,
                            templatesPaths,
                            details.getTemplateContext(),
                            errorLoggingContext);

                    event.setDetailsFulltextSearch(
                            renderText(
                                    e -> errorLoggingContext.apply(
                                            "details-fulltext-search.ftl",
                                            e),
                                    locale,
                                    templatesPaths,
                                    "details-fulltext-search.ftl",
                                    details.getTemplateContext(),
                                    () -> taskName));

                });
    }


    public void publishEvent(UserTaskEvent userTaskEvent){

        applicationEventPublisher.publishEvent(
                new io.vanillabp.cockpit.adapter.camunda8.usertask.publishing.UserTaskEvent(
                        Camunda8UserTaskHandler.class,
                        userTaskEvent));

        applicationEventPublisher.publishEvent(
                new ProcessUserTaskEvent(Camunda8UserTaskHandler.class));
    }


}
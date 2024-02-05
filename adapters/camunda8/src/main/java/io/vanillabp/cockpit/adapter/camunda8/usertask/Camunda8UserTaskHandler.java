package io.vanillabp.cockpit.adapter.camunda8.usertask;

import freemarker.template.Configuration;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskLifecycleEvent;
import io.vanillabp.cockpit.adapter.camunda8.usertask.publishing.ProcessUserTaskEvent;
import io.vanillabp.cockpit.adapter.camunda8.workflow.persistence.ProcessInstanceEntity;
import io.vanillabp.cockpit.adapter.camunda8.workflow.persistence.ProcessInstanceRepository;
import io.vanillabp.cockpit.adapter.common.CockpitProperties;
import io.vanillabp.cockpit.adapter.common.usertask.*;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCancelledEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCompletedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskLifecycleEvent;
import io.vanillabp.cockpit.commons.utils.DateTimeUtil;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.springboot.adapter.AdapterAwareProcessService;
import io.vanillabp.springboot.adapter.wiring.WorkflowAggregateCache;
import io.vanillabp.springboot.parameters.MethodParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.repository.CrudRepository;
import org.springframework.util.StringUtils;

import java.io.File;
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

public class Camunda8UserTaskHandler extends UserTaskHandlerBase {

    private static final Logger logger = LoggerFactory.getLogger(Camunda8UserTaskHandler.class);

    private final CockpitProperties cockpitProperties;
    private final UserTaskProperties userTaskProperties;
    private final ApplicationEventPublisher applicationEventPublisher;
    private Function<String, Object> parseWorkflowAggregateIdFromBusinessKey;
    private final AdapterAwareProcessService<?> processService;
    private final ProcessInstanceRepository processInstanceRepository;
    private final String taskDefinition;
    private final String taskTitle;

    @Override
    protected Logger getLogger() {
        return logger;
    }

    public Camunda8UserTaskHandler(
            String taskDefinition,
            CockpitProperties cockpitProperties,
            UserTasksProperties workflowProperties,
            UserTaskProperties userTaskProperties,
            ApplicationEventPublisher applicationEventPublisher,
            Optional<Configuration> templating,
            String taskTitle,
            AdapterAwareProcessService<?> processService,
            ProcessInstanceRepository processInstanceRepository,
            CrudRepository<Object, Object> workflowAggregateRepository,
            Object bean,
            Method method,
            List<MethodParameter> parameters) {

        super(workflowProperties, templating, workflowAggregateRepository, bean, method, parameters);
        this.taskDefinition = taskDefinition;
        this.taskTitle = taskTitle;
        this.cockpitProperties = cockpitProperties;
        this.userTaskProperties = userTaskProperties;
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

        userTaskLifecycleEvent.setTimestamp(
                DateTimeUtil.fromMilliseconds(camunda8UserTaskLifecycleEvent.getTimestamp()));

        userTaskLifecycleEvent.setUserTaskId(
                String.valueOf(camunda8UserTaskLifecycleEvent.getElementInstanceKey()));

//        userTaskLifecycleEvent.setComment(
//                camunda8UserTaskLifecycleEvent.getDeleteReason()
//        );
    }

    public void notify(Camunda8UserTaskCreatedEvent camunda8UserTaskCreatedEvent) {
        UserTaskCreatedEvent userTaskCreatedEvent = new UserTaskCreatedEvent(
                "demo",
                cockpitProperties.getI18nLanguages()
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

    private UserTaskCreatedEvent fillUserTaskCreatedEvent(
            Camunda8UserTaskCreatedEvent camunda8UserTaskCreatedEvent,
            UserTaskCreatedEvent userTaskCreatedEvent) {

        String businessKey = this.getBusinessKeyFromProcessInstanceKey(
                camunda8UserTaskCreatedEvent.getProcessInstanceKey());

        prefillCreatedEvent(userTaskCreatedEvent, camunda8UserTaskCreatedEvent, businessKey);

//        UserTaskDetails prefilledUserTaskDetails = this.callUserTaskDetailsProviderMethod(
//                camunda8UserTaskCreatedEvent,
//                userTaskCreatedEvent
//        );

//        if(prefilledUserTaskDetails != null && prefilledUserTaskDetails != userTaskCreatedEvent){
//            addDataToPrefilledEvent(userTaskCreatedEvent, prefilledUserTaskDetails);
//        }

        if ((userTaskCreatedEvent.getI18nLanguages() == null)
                || userTaskCreatedEvent.getI18nLanguages().isEmpty()){
            userTaskCreatedEvent.setI18nLanguages(cockpitProperties.getI18nLanguages());
        }

//        if (templating.isEmpty()) {
//            fillUserTaskWithTemplatingDeactivated(
//                    camunda8UserTaskCreatedEvent.getBpmnProcessId(),
//                    taskTitle,
//                    userTaskCreatedEvent,
//                    prefilledUserTaskDetails
//            );
//        } else {
//            fillUserTaskWithTemplatingActivated(
//                    camunda8UserTaskCreatedEvent.getBpmnProcessId(),
//                    taskTitle,
//                    userTaskCreatedEvent,
//                    prefilledUserTaskDetails
//            );
//        }

        if (templating.isEmpty()) {
            fillUserTaskWithTemplatingDeactivated(
                    camunda8UserTaskCreatedEvent.getBpmnProcessId(),
                    taskTitle,
                    userTaskCreatedEvent,
                    userTaskCreatedEvent
            );
        } else {
            fillUserTaskWithTemplatingActivated(
                    camunda8UserTaskCreatedEvent.getBpmnProcessId(),
                    taskTitle,
                    userTaskCreatedEvent,
                    userTaskCreatedEvent
            );
        }


        return userTaskCreatedEvent;
    }


    private void prefillCreatedEvent(UserTaskCreatedEvent userTaskCreatedEvent,
                                     Camunda8UserTaskCreatedEvent jobRecord,
                                     String businessKey
        ) {

        // TODO: process instance key correct?
        userTaskCreatedEvent.setEventId(
                System.nanoTime() +
                        "@" +
                        jobRecord.getProcessInstanceKey());

        userTaskCreatedEvent.setUserTaskId(
                String.valueOf(jobRecord.getElementInstanceKey()));

        // TODO use timestamp from event
        userTaskCreatedEvent.setTimestamp(
                OffsetDateTime.now());

        userTaskCreatedEvent.setBpmnProcessId(
                jobRecord.getBpmnProcessId());

        userTaskCreatedEvent.setBpmnProcessVersion(
                String.valueOf(jobRecord.getWorkflowDefinitionVersion()));

        userTaskCreatedEvent.setTaskDefinition(taskDefinition);

        userTaskCreatedEvent.setBusinessId(businessKey);
        userTaskCreatedEvent.setBpmnTaskId(jobRecord.getElementId());

        userTaskCreatedEvent.setWorkflowId(
                String.valueOf(jobRecord.getProcessInstanceKey()));

        userTaskCreatedEvent.setSubWorkflowId(
                String.valueOf(jobRecord.getProcessInstanceKey()));

        userTaskCreatedEvent.setTitle(new HashMap<>());
        userTaskCreatedEvent.setWorkflowTitle(new HashMap<>());
        userTaskCreatedEvent.setTaskDefinitionTitle(new HashMap<>());

        userTaskCreatedEvent.setAssignee(userTaskCreatedEvent.getAssignee());
        userTaskCreatedEvent.setCandidateUsers(userTaskCreatedEvent.getCandidateUsers());
        userTaskCreatedEvent.setCandidateGroups(userTaskCreatedEvent.getCandidateGroups());
        userTaskCreatedEvent.setDueDate(userTaskCreatedEvent.getDueDate());
        userTaskCreatedEvent.setFollowUpDate(userTaskCreatedEvent.getFollowUpDate());

    }

//
//    @SuppressWarnings("unchecked")
//    private UserTaskDetails callUserTaskDetailsProviderMethod(
//            final Camunda8UserTaskCreatedEvent delegateTask,
//            final PrefilledUserTaskDetails prefilledUserTaskDetails) {
//
//
//        try {
//
//            logger.trace("Will handle user-task '{}' of workflow '{}' ('{}') by execution '{}'",
//                    delegateTask.getElementId(),
//                    delegateTask.getProcessInstanceKey(),
//                    delegateTask.getBpmnProcessId(),
//                    delegateTask.getElementInstanceKey());
//
//            final var workflowAggregateId = parseWorkflowAggregateIdFromBusinessKey
//                    .apply(delegateTask.getBusinessKey());
//
//            final var workflowAggregateCache = new WorkflowAggregateCache();
//
//            return super.execute(
//                    workflowAggregateCache,
//                    workflowAggregateId,
//                    true,
//                    (args, param) -> processTaskParameter(
//                            args,
//                            param,
//                            taskParameter -> execution.getVariableLocal(taskParameter)),
//                    (args, param) -> processTaskIdParameter(
//                            args,
//                            param,
//                            () -> delegateTask.getId()),
//                    (args, param) -> processPrefilledUserTaskDetailsParameter(
//                            args,
//                            param,
//                            () -> prefilledUserTaskDetails))
//
//        } catch (RuntimeException e) {
//
//            throw e;
//
//        } catch (Exception e) {
//
//            throw new RuntimeException(e);
//
//        }
//
//    }

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


    private void fillUserTaskWithTemplatingDeactivated(
            String bpmnProcessName,
            String taskName,
            UserTaskCreatedEvent event,
            UserTaskDetails details) {

        final String language =
                userTaskProperties != null && StringUtils.hasText(userTaskProperties.getBpmnDescriptionLanguage())
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
                            bpmnProcessName,
                            details.getTemplateContext(),
                            errorLoggingContext);

                    setTextInEvent(
                            language,
                            locale,
                            userTaskProperties != null && StringUtils.hasText(userTaskProperties.getTemplatesPath())
                                    ? userTaskProperties.getTemplatesPath()
                                    + File.separator
                                    + "title.ftl"
                                    : "title.ftl",
                            details::getTitle,
                            event::getTitle,
                            taskName,
                            details.getTemplateContext(),
                            errorLoggingContext);

                    setTextInEvent(
                            language,
                            locale,
                            userTaskProperties != null && StringUtils.hasText(userTaskProperties.getTemplatesPath())
                                    ? userTaskProperties.getTemplatesPath()
                                    + File.separator
                                    + "task-definition-title.ftl"
                                    : "task-definition-title.ftl",
                            details::getTaskDefinitionTitle,
                            event::getTaskDefinitionTitle,
                            taskName,
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
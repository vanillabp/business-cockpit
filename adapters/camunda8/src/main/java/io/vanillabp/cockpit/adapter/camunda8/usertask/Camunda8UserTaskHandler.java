package io.vanillabp.cockpit.adapter.camunda8.usertask;

import freemarker.template.Configuration;
import io.camunda.client.CamundaClient;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskEvent;
import io.vanillabp.cockpit.adapter.camunda8.usertask.publishing.ProcessUserTaskEvent;
import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.usertask.UserTaskHandlerBase;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCancelledEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCompletedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskEventImpl;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskUpdatedEvent;
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
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

public class Camunda8UserTaskHandler extends UserTaskHandlerBase {

    private static final Logger logger = LoggerFactory.getLogger(Camunda8UserTaskHandler.class);
    private final ApplicationEventPublisher applicationEventPublisher;
    private Function<String, Object> parseWorkflowAggregateIdFromBusinessKey;
    private final AdapterAwareProcessService<?> processService;
    private final String aggregateIdPropertyName;
    private final String taskDefinition;
    private final String taskTitle;
    private final String processTitle;
    private final String bpmnProcessId;
    private String bpmnProcessVersionInfo;
    private final CamundaClient client;

    @Override
    protected Logger getLogger() {
        return logger;
    }

    public Camunda8UserTaskHandler(
            final String taskDefinition,
            final VanillaBpCockpitProperties cockpitProperties,
            final ApplicationEventPublisher applicationEventPublisher,
            final Optional<Configuration> templating,
            final String bpmnProcessId,
            final String bpmnProcessVersionInfo,
            final String processTitle,
            final String taskTitle,
            final AdapterAwareProcessService<?> processService,
            final String aggregateIdPropertyName,
            final CrudRepository<Object, Object> workflowAggregateRepository,
            final Object bean,
            final Method method,
            final List<MethodParameter> parameters,
            final CamundaClient client) {

        super(cockpitProperties, templating, workflowAggregateRepository, bean, method, parameters);
        this.bpmnProcessId = bpmnProcessId;
        this.taskDefinition = taskDefinition;
        this.taskTitle = taskTitle;
        this.processTitle = processTitle;
        this.applicationEventPublisher = applicationEventPublisher;
        this.processService = processService;
        this.aggregateIdPropertyName = aggregateIdPropertyName;
        this.client = client;
        this.bpmnProcessVersionInfo = bpmnProcessVersionInfo;

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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(
            retryFor = { Exception.class },
            maxAttempts = 4,
            backoff = @Backoff(delay = 500, maxDelay = 1500, multiplier = 1.5))
    public void notify(
            final Camunda8UserTaskEvent camunda8UserTaskEvent) {

        final var workflowModuleId = processService.getWorkflowModuleId();
        final var i18nLanguages = properties.getI18nLanguages(workflowModuleId, bpmnProcessId);

        UserTaskEventImpl userTaskEvent = switch (camunda8UserTaskEvent.getEvent()) {
            case CREATED -> new UserTaskCreatedEvent(workflowModuleId, i18nLanguages);
            case UPDATED -> new UserTaskUpdatedEvent(workflowModuleId, i18nLanguages);
            case COMPLETED -> new UserTaskCompletedEvent(workflowModuleId, i18nLanguages);
            case CANCELED -> new UserTaskCancelledEvent(workflowModuleId, i18nLanguages);
        };
        this.fillUserTaskEvent(camunda8UserTaskEvent, userTaskEvent);

        publishEvent(userTaskEvent);

        client.newCompleteCommand(camunda8UserTaskEvent.getJobKey()).send().join();

    }

    @Recover
    public void recoverNotify(
            final Exception exception,
            final Camunda8UserTaskEvent camunda8UserTaskEvent) {

        logger.error("Could not process user task created/updated event: '{}'!",
                camunda8UserTaskEvent,
                exception);

    }

    private void fillUserTaskEvent(
            Camunda8UserTaskEvent camunda8UserTaskEvent,
            UserTaskEventImpl userTaskEvent) {

        final var rawBusinessKey = camunda8UserTaskEvent.getVariables().get(aggregateIdPropertyName);
        if (rawBusinessKey == null) {
            logger.error("Could not find process variable '{}' in event for type '{}'! Will ignore this event.",
                    aggregateIdPropertyName, camunda8UserTaskEvent.getTaskDefinition());
            return;
        }
        final var parsedBusinessKey = parseWorkflowAggregateIdFromBusinessKey.apply(rawBusinessKey.toString());

        prefillEvent(userTaskEvent, camunda8UserTaskEvent, parsedBusinessKey);

        UserTaskDetails prefilledUserTaskDetails = this.callUserTaskDetailsProviderMethod(
                camunda8UserTaskEvent,
                userTaskEvent,
                parsedBusinessKey
        );

        if(prefilledUserTaskDetails == null){
            prefilledUserTaskDetails = userTaskEvent;
        } else if(prefilledUserTaskDetails != userTaskEvent){
            addDataToPrefilledEvent(userTaskEvent, prefilledUserTaskDetails);
        }

        if ((userTaskEvent.getI18nLanguages() == null)
                || userTaskEvent.getI18nLanguages().isEmpty()){
            userTaskEvent.setI18nLanguages(
                    properties.getI18nLanguages(processService.getWorkflowModuleId(), bpmnProcessId)
            );
        }

        filluserTaskDetailsByCustomDetails(
                userTaskEvent,
                prefilledUserTaskDetails);
    }

    private void prefillEvent(final UserTaskEventImpl userTaskEvent,
                              final Camunda8UserTaskEvent camunda8TaskEvent,
                              final Object businessKey) {

        userTaskEvent.setEventId(
                System.nanoTime() + "@" + camunda8TaskEvent.getJobKey() + "#" + camunda8TaskEvent.getEvent());

        userTaskEvent.setUserTaskId(
                String.valueOf(camunda8TaskEvent.getUserTaskKey()));

        userTaskEvent.setTimestamp(camunda8TaskEvent.getTimestamp());
        userTaskEvent.setEventType(camunda8TaskEvent.getEvent());

        userTaskEvent.setBpmnProcessId(
                camunda8TaskEvent.getBpmnProcessId());
        userTaskEvent.setBpmnProcessVersion(bpmnProcessVersionInfo);

        userTaskEvent.setTaskDefinition(taskDefinition);

        userTaskEvent.setBusinessId(businessKey.toString());
        userTaskEvent.setBpmnTaskId(camunda8TaskEvent.getElementId());

        userTaskEvent.setWorkflowId(
                String.valueOf(camunda8TaskEvent.getProcessInstanceKey()));

        userTaskEvent.setSubWorkflowId(
                String.valueOf(camunda8TaskEvent.getProcessInstanceKey()));

        userTaskEvent.setTitle(new HashMap<>());
        userTaskEvent.setWorkflowTitle(new HashMap<>());
        userTaskEvent.setTaskDefinitionTitle(new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    private UserTaskDetails callUserTaskDetailsProviderMethod(
            final Camunda8UserTaskEvent delegateTask,
            final PrefilledUserTaskDetails prefilledUserTaskDetails,
            final Object businessKey) {
        try {

            logger.trace("Will handle user-task '{}' of workflow '{}' ('{}') of job '{}' for event '{}'",
                    delegateTask.getElementId(),
                    delegateTask.getProcessInstanceKey(),
                    delegateTask.getBpmnProcessId(),
                    delegateTask.getJobKey(),
                    delegateTask.getEvent());

            final var workflowAggregateCache = new WorkflowAggregateCache();

            return super.execute(
                    workflowAggregateCache,
                    businessKey,
                    true,
                    (args, param) -> processTaskIdParameter(
                            args,
                            param,
                            () -> String.valueOf(delegateTask.getUserTaskKey())),
                    (args, param) -> processDetailsEventParameter(
                            args,
                            param,
                            delegateTask::getEvent),
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

    private void addDataToPrefilledEvent(UserTaskEventImpl event,
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

    private void filluserTaskDetailsByCustomDetails(UserTaskEventImpl userTaskCreatedEvent, UserTaskDetails prefilledUserTaskDetails) {
        if (templating.isEmpty()) {
            fillUserTaskWithTemplatingDeactivated(
                    userTaskCreatedEvent,
                    prefilledUserTaskDetails
            );
        } else {
            fillUserTaskWithTemplatingActivated(
                    userTaskCreatedEvent,
                    prefilledUserTaskDetails
            );
        }
    }

    private void fillUserTaskWithTemplatingDeactivated(
            UserTaskEventImpl event,
            UserTaskDetails details) {

        final var language = properties.getBpmnDescriptionLanguage(processService.getWorkflowModuleId(), bpmnProcessId);

        if ((details.getWorkflowTitle() == null)
                || details.getWorkflowTitle().isEmpty()) {
            event.setWorkflowTitle(Map.of(
                    language,
                    processTitle));
        }
        if ((details.getTitle() == null)
                || details.getTitle().isEmpty()) {
            event.setTitle(Map.of(
                    language,
                    taskTitle));
        }
        if ((details.getTaskDefinitionTitle() == null)
                || details.getTaskDefinitionTitle().isEmpty()) {
            event.setTaskDefinitionTitle(Map.of(
                    language,
                    taskDefinition));
        }
        if ((details.getDetailsFulltextSearch() == null)
                || !StringUtils.hasText(details.getDetailsFulltextSearch())) {
            event.setDetailsFulltextSearch(
                    taskTitle);
        }
    }


    private void fillUserTaskWithTemplatingActivated(
            UserTaskEventImpl event,
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
                            processTitle,
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
                            taskTitle,
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
                            taskDefinition,
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
                                    () -> taskTitle));

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


    public void updateVersionInfo(
            final String versionInfo) {

        this.bpmnProcessVersionInfo = versionInfo;

    }

}
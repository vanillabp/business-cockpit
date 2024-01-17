package io.vanillabp.cockpit.adapter.camunda8.usertask;

import freemarker.template.Configuration;
import io.camunda.zeebe.protocol.record.intent.JobIntent;
import io.vanillabp.cockpit.adapter.camunda8.usertask.publishing.ProcessUserTaskEvent;
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
import io.vanillabp.springboot.parameters.MethodParameter;
import io.zeebe.exporter.proto.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.repository.CrudRepository;
import org.springframework.util.StringUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Camunda8UserTaskHandler extends UserTaskHandlerBase {

    private static final Logger logger = LoggerFactory.getLogger(Camunda8UserTaskHandler.class);

    private final CockpitProperties properties;
    private final UserTaskProperties userTaskProperties;
    private final ApplicationEventPublisher applicationEventPublisher;
    private Function<String, Object> parseWorkflowAggregateIdFromBusinessKey;
    private final AdapterAwareProcessService<?> processService;

    @Override
    protected Logger getLogger() {
        return logger;
    }


    /*
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
     */

    public Camunda8UserTaskHandler(
            String taskDefinition,
            CockpitProperties properties,
            UserTasksProperties workflowProperties,
            UserTaskProperties userTaskProperties,
            ApplicationEventPublisher applicationEventPublisher,
            Optional<Configuration> templating,
            String bpmnProcessId,
            AdapterAwareProcessService<?> processService,
            CrudRepository<Object, Object> workflowAggregateRepository,
            Object bean,
            Method method,
            List<MethodParameter> parameters) {

        super(workflowProperties, templating, workflowAggregateRepository, bean, method, parameters);

        this.properties = properties;
        this.userTaskProperties = userTaskProperties;
        this.applicationEventPublisher = applicationEventPublisher;
        this.processService = processService;

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

    public void notify(Schema.JobRecord jobRecord) {
        String intent = jobRecord.getMetadata().getIntent();

        UserTaskEvent userTaskEvent = switch (JobIntent.valueOf(intent)) {
            case COMPLETED -> this.getUserTaskCompletedEvent(jobRecord);
            case CANCELED -> this.getUserTaskCancelledEvent(jobRecord);
            case CREATED -> this.getUserTaskCreatedEvent(jobRecord);
            default -> throw new RuntimeException("Unsupported event type '" + intent + "'!");
        };

        applicationEventPublisher.publishEvent(userTaskEvent);

        applicationEventPublisher.publishEvent(
                new ProcessUserTaskEvent(Camunda8UserTaskHandler.class));
    }

    /*
     * Lifecycle message objects such as "completed" or "cancelled" need to be filled the same way,
     * but the classes are created by openAPI and do not share an interface.
     * We therefore make use of wrapper classes to provide a single interface.
     */

    private UserTaskCompletedEvent getUserTaskCompletedEvent(Schema.JobRecord jobRecord) {
        UserTaskCompletedEvent userTaskCompletedEvent = new UserTaskCompletedEvent();
        fillLifecycleEvent(userTaskCompletedEvent, jobRecord);
        return userTaskCompletedEvent;
    }

    private UserTaskCancelledEvent getUserTaskCancelledEvent(Schema.JobRecord jobRecord) {
        UserTaskCancelledEvent userTaskCancelledEvent = new UserTaskCancelledEvent();
        fillLifecycleEvent(userTaskCancelledEvent, jobRecord);
        return userTaskCancelledEvent;
    }


    private void fillLifecycleEvent(UserTaskLifecycleEvent eventWrapper,
                                    Schema.JobRecord jobRecord) {
        eventWrapper.setId(
                System.nanoTime()
                        + "@"
                        + jobRecord.getProcessInstanceKey()
                        + "#"
                        + jobRecord.getElementId());
        eventWrapper.setTimestamp(DateTimeUtil.fromMilliseconds(jobRecord.getMetadata().getTimestamp()));
        eventWrapper.setUserTaskId(jobRecord.getElementId());
    }

    private UserTaskCreatedEvent getUserTaskCreatedEvent(Schema.JobRecord jobRecord) {
        UserTaskCreatedEvent userTaskCreatedEvent = new UserTaskCreatedEvent(
                jobRecord.getWorker(),  // TODO: correct string
                properties.getI18nLanguages()
        );
        userTaskCreatedEvent.setTimestamp(OffsetDateTime.now());

        // TODO: use timestamp from job record
        //   jobRecord.getMetadata().getTimestamp();

        prefillCreatedEvent(userTaskCreatedEvent, jobRecord);


        UserTaskDetails prefilledUserTaskDetails = this.callUserTaskDetailsProviderMethod(
                jobRecord,
                userTaskCreatedEvent
        );

        if(prefilledUserTaskDetails != null && prefilledUserTaskDetails != userTaskCreatedEvent){
            addDataToPrefilledEvent(userTaskCreatedEvent, prefilledUserTaskDetails);
        }

        if ((userTaskCreatedEvent.getI18nLanguages() == null)
                || userTaskCreatedEvent.getI18nLanguages().isEmpty()){
            userTaskCreatedEvent.setI18nLanguages(properties.getI18nLanguages());
        }

        return userTaskCreatedEvent;
    }


    private void prefillCreatedEvent(UserTaskCreatedEvent eventWrapper,
                                     Schema.JobRecord jobRecord) {

        eventWrapper.setBpmnProcessId(jobRecord.getBpmnProcessId());
        eventWrapper.setBpmnProcessVersion(
                String.valueOf(jobRecord.getWorkflowDefinitionVersion()));
        eventWrapper.setBpmnTaskId(
                jobRecord.getElementId());
    }


    @SuppressWarnings("unchecked")
    private UserTaskDetails callUserTaskDetailsProviderMethod(
            final Schema.JobRecord jobRecord,
            final PrefilledUserTaskDetails prefilledUserTaskDetails) {

        final var multiInstanceCache = new Map[] { null };

        String bpmnProcessId;

        jobRecord.getBpmnProcessId();
        jobRecord.getProcessInstanceKey();
        jobRecord.getElementId();
        jobRecord.getMetadata().getKey();
        jobRecord.getMetadata().getIntent();
//
//        try {
//
//            logger.trace("Will handle user-task '{}' of workflow '{}' ('{}') by execution '{}'",
//                    delegateTask.getBpmnModelElementInstance().getId(),
//                    delegateTask.getProcessInstanceId(),
//                    bpmnProcessId,
//                    delegateTask.getExecutionId());
//
//            String businessKey = execution.getBusinessKey();
//            String taskId = delegateTask.getId();
//
//
//            final var workflowAggregateId = parseWorkflowAggregateIdFromBusinessKey
//                    .apply(businessKey);
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
//                            () -> taskId),
//                    (args, param) -> processPrefilledUserTaskDetailsParameter(
//                            args,
//                            param,
//                            () -> prefilledUserTaskDetails)
//                    ,
//                    (args, param) -> processMultiInstanceIndexParameter(
//                            args,
//                            param,
//                            multiInstanceSupplier),
//                    (args, param) -> processMultiInstanceTotalParameter(
//                            args,
//                            param,
//                            multiInstanceSupplier),
//                    (args, param) -> processMultiInstanceElementParameter(
//                            args,
//                            param,
//                            multiInstanceSupplier),
//                    (args, param) -> processMultiInstanceResolverParameter(
//                            args,
//                            param,
//                            () -> {
//                                if (workflowAggregateCache.workflowAggregate == null) {
//                                    workflowAggregateCache.workflowAggregate = workflowAggregateRepository
//                                            .findById(workflowAggregateId)
//                                            .orElseThrow();
//                                }
//                                return workflowAggregateCache.workflowAggregate;
//                            },
//                            multiInstanceSupplier)
//            );
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

        return prefilledUserTaskDetails;
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


    private void fillUserTaskWithTemplatingActivated(
            String bpmnProcessName,
            String taskName,
            UserTaskCreatedEvent event,
            UserTaskDetails details) {

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


    private void fillUserTaskWithTemplatingDeactivated(
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
                            StringUtils.hasText(userTaskProperties.getTemplatesPath())
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
                            StringUtils.hasText(userTaskProperties.getTemplatesPath())
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

}
package io.vanillabp.cockpit.extension.handler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import io.vanillabp.cockpit.extension.config.ConfigurationKeys;
import io.vanillabp.cockpit.extension.event.UserTaskEvent;
import io.vanillabp.integration.extension.spi.handler.CoreHandlerParameter;
import io.vanillabp.integration.extension.spi.handler.HandlerContext;
import io.vanillabp.integration.extension.spi.handler.HandlerContract;
import io.vanillabp.integration.extension.spi.handler.HandlerParameter;
import io.vanillabp.integration.extension.spi.handler.HandlerValueSource;
import io.vanillabp.spi.cockpit.details.DetailsEvent;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider;
import io.vanillabp.spi.cockpit.workflow.PrefilledWorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetailsProvider;

/**
 * The two contracts telling VanillaBP how to find and invoke the Business Cockpit's details
 * providers, so that the machinery behind <code>&#64;WorkflowTask</code> serves them too.
 * <p>
 * A user-task provider is matched by the BPMN element id or by the task definition, whichever
 * the annotation names, and by the method's own name where it names neither - the convention
 * every VanillaBP annotation follows. A workflow provider exists once per BPMN process and
 * therefore serves every key of it.
 * <p>
 * No matching method is a legal answer, and the prefilled details then pass through unchanged:
 * a workflow module which reports nothing of its own still shows up in the cockpit, with the
 * titles the BPMN carries.
 */
public final class BusinessCockpitHandlers {

  private BusinessCockpitHandlers() {
  }

  /**
   * @return The contract of <code>&#64;UserTaskDetailsProvider</code>
   */
  public static HandlerContract userTaskContract() {

    return HandlerContract
        .of(ConfigurationKeys.EXTENSION_ID, UserTaskDetailsProvider.class)
        .lookupKeys(BusinessCockpitHandlers::userTaskLookupKeys)
        .coreParameters(
            CoreHandlerParameter.WORKFLOW_AGGREGATE,
            CoreHandlerParameter.TASK_PARAM,
            CoreHandlerParameter.MULTI_INSTANCE)
        .parameterBinder(BusinessCockpitHandlers::bindPrefilledUserTaskDetails)
        .parameterBinder(BusinessCockpitHandlers::bindDetailsEvent)
        .validatingAnnotation(BusinessCockpitHandlers::rejectReservedVersionAttribute)
        .deliversReturnValue()
        .build();

  }

  /**
   * @return The contract of <code>&#64;WorkflowDetailsProvider</code>
   */
  public static HandlerContract workflowContract() {

    return HandlerContract
        .of(ConfigurationKeys.EXTENSION_ID, WorkflowDetailsProvider.class)
        .lookupKeys(annotation -> List.of(HandlerContract.EVERY_KEY))
        .coreParameters(CoreHandlerParameter.WORKFLOW_AGGREGATE)
        .parameterBinder(BusinessCockpitHandlers::bindPrefilledWorkflowDetails)
        .deliversReturnValue()
        .build();

  }

  /**
   * Which BPMN elements one occurrence of <code>&#64;UserTaskDetailsProvider</code> serves.
   *
   * @param annotation The occurrence
   * @return The element id and the task definition it names, empty for "the method's own name"
   */
  private static List<String> userTaskLookupKeys(
      final Annotation annotation) {

    final var provider = (UserTaskDetailsProvider) annotation;
    final var keys = new LinkedList<String>();
    if (!UserTaskDetailsProvider.USE_METHOD_NAME.equals(provider.id())) {
      keys.add(provider.id());
    }
    if (!UserTaskDetailsProvider.USE_METHOD_NAME.equals(provider.taskDefinition())) {
      keys.add(provider.taskDefinition());
    }
    return keys;

  }

  /**
   * The <code>version</code> attribute of <code>&#64;UserTaskDetailsProvider</code> is reserved
   * and has to stay unset - see decision 7 in the repository's DECISIONS.md.
   * <p>
   * Version-aware matching means picking a different method per deployed version of a process,
   * and the events this extension reacts to carry no process version: a task listener says
   * which task fired, not which version of the model it came from. Nothing would therefore
   * distinguish two methods, and a value which is silently ignored is worse than one which is
   * refused - version 1 documented the attribute and never read it, and applications wrote it
   * believing it worked.
   * <p>
   * VanillaBP runs this while it scans the annotation, holding the method which carries it, and
   * puts the annotation, the class, the method and this extension in front of what is said
   * here.
   *
   * @param annotation One occurrence of <code>&#64;UserTaskDetailsProvider</code>
   * @param method The method carrying it, which VanillaBP names in its refusal
   * @throws IllegalStateException If it names a version - the message says what to write instead
   */
  private static void rejectReservedVersionAttribute(
      final Annotation annotation,
      final Method method) {

    final var version = ((UserTaskDetailsProvider) annotation).version();
    if ((version.length == 0) || Arrays
        .stream(version)
        .allMatch(UserTaskDetailsProvider.ALL::equals)) {
      return;
    }
    throw new IllegalStateException(
        """
            it names version '%s'. The attribute is reserved and has to stay unset: the Business \
            Cockpit reacts to events which do not say which version of the process they came \
            from, so a version cannot decide which method runs. Remove the attribute and match \
            by 'id' or 'taskDefinition' instead."""
            .formatted(String.join(", ", version)));

  }

  private static Optional<HandlerValueSource> bindPrefilledUserTaskDetails(
      final HandlerParameter parameter) {

    return parameter.getType().equals(PrefilledUserTaskDetails.class)
        ? Optional.of(HandlerContext::getPayload)
        : Optional.empty();

  }

  private static Optional<HandlerValueSource> bindPrefilledWorkflowDetails(
      final HandlerParameter parameter) {

    return parameter.getType().equals(PrefilledWorkflowDetails.class)
        ? Optional.of(HandlerContext::getPayload)
        : Optional.empty();

  }

  private static Optional<HandlerValueSource> bindDetailsEvent(
      final HandlerParameter parameter) {

    if (!parameter.isAnnotationPresent(DetailsEvent.class)) {
      return Optional.empty();
    }
    if (!parameter.getType().equals(DetailsEvent.Event.class)) {
      throw new IllegalStateException(
          """
              The %s is annotated with @DetailsEvent but is not of type \
              io.vanillabp.spi.cockpit.details.DetailsEvent.Event! Change the parameter's type."""
              .formatted(parameter.describe()));
    }
    return Optional
        .of(context -> detailsEventOf(context.payload(UserTaskEvent.class)));

  }

  /**
   * @param event The event a details provider is invoked for
   * @return The same thing in the vocabulary the SPI hands to business code
   */
  public static DetailsEvent.Event detailsEventOf(
      final UserTaskEvent event) {

    return switch (event.getEventKind()) {
      case CREATED -> DetailsEvent.Event.CREATED;
      case UPDATED -> DetailsEvent.Event.UPDATED;
      case COMPLETED -> DetailsEvent.Event.COMPLETED;
      case CANCELED -> DetailsEvent.Event.CANCELED;
    };

  }

  /**
   * The keys an invocation for one user task accepts, most specific first: a method naming the
   * task definition or the element id wins over one serving every task of the process.
   *
   * @param taskDefinition The task's form reference, may be <code>null</code>
   * @param bpmnTaskId The task's BPMN element id, may be <code>null</code>
   * @return The keys
   */
  public static List<String> lookupKeysOf(
      final String taskDefinition,
      final String bpmnTaskId) {

    final var keys = new LinkedList<String>();
    if ((taskDefinition != null) && !taskDefinition.isBlank()) {
      keys.add(taskDefinition);
    }
    if ((bpmnTaskId != null) && !bpmnTaskId.isBlank()) {
      keys.add(bpmnTaskId);
    }
    return keys;

  }

}

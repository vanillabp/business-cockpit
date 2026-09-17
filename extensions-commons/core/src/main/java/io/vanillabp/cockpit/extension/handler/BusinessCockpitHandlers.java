package io.vanillabp.cockpit.extension.handler;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
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
 * the annotation names. Where it names neither, the method's own name is the match, which is
 * the convention every VanillaBP annotation follows. A method writing
 * {@link UserTaskDetailsProvider#ALL} instead of a name serves every user task of every BPMN
 * process its workflow service declares, and it runs where no method of that process names the
 * task. A workflow provider exists once per BPMN process and therefore serves every key of it.
 * <p>
 * The version of the deployed BPMN process narrows both of them further. A method naming
 * versions runs for those versions only, a method naming none runs for every version, and two
 * methods serving one key are told apart by their versions. Both contracts say that their calls
 * carry the version, so a method VanillaBP could never reach is named while the application
 * starts. See decision 24 in the repository's DECISIONS.md.
 * <p>
 * No matching method is a legal answer, and that holds for a version nobody serves too. The
 * prefilled details then pass through unchanged, so a workflow module which reports nothing of
 * its own still shows up in the cockpit, with the titles the BPMN carries.
 * <p>
 * Both contracts say that VanillaBP never saves the workflow aggregate after one of these
 * methods ran. A details provider answers what the cockpit should show, and answering a
 * question does not change a case. See decision 17 in the repository's DECISIONS.md. It is said
 * here and not at every call, because VanillaBP wires the methods long before anybody calls one.
 * A start which cannot read it warns about a writer this extension does not have.
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
        .versions(annotation -> versionsOf(((UserTaskDetailsProvider) annotation).version()))
        .callsCarryTheProcessVersion()
        .coreParameters(
            CoreHandlerParameter.WORKFLOW_AGGREGATE,
            CoreHandlerParameter.TASK_PARAM,
            CoreHandlerParameter.MULTI_INSTANCE)
        .parameterBinder(BusinessCockpitHandlers::bindPrefilledUserTaskDetails)
        .parameterBinder(BusinessCockpitHandlers::bindDetailsEvent)
        .deliversReturnValue()
        .neverSavesTheWorkflowAggregate()
        .build();

  }

  /**
   * @return The contract of <code>&#64;WorkflowDetailsProvider</code>
   */
  public static HandlerContract workflowContract() {

    return HandlerContract
        .of(ConfigurationKeys.EXTENSION_ID, WorkflowDetailsProvider.class)
        .lookupKeys(annotation -> List.of(HandlerContract.EVERY_KEY))
        .versions(annotation -> versionsOf(((WorkflowDetailsProvider) annotation).version()))
        .callsCarryTheProcessVersion()
        .coreParameters(CoreHandlerParameter.WORKFLOW_AGGREGATE)
        .parameterBinder(BusinessCockpitHandlers::bindPrefilledWorkflowDetails)
        .deliversReturnValue()
        .neverSavesTheWorkflowAggregate()
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
    addLookupKey(keys, provider.id());
    addLookupKey(keys, provider.taskDefinition());
    return keys;

  }

  /**
   * Adds what one attribute of the annotation names to the keys a method serves.
   * <p>
   * {@link UserTaskDetailsProvider#ALL} becomes {@link HandlerContract#EVERY_KEY}. That is how
   * a method claiming every user task reaches VanillaBP. To VanillaBP it looks like a
   * <code>&#64;WorkflowDetailsProvider</code> or like a start method which names no start event,
   * so the rule those follow holds here as well. A method naming one task wins over it, and two
   * methods claiming every task of one workflow service end the boot.
   * <p>
   * Both constants read '*' today. The translation happens all the same, so that a method
   * claiming every task keeps doing so if one of them ever changes.
   *
   * @param keys What the method serves so far
   * @param attribute What <code>id</code> or <code>taskDefinition</code> says
   */
  private static void addLookupKey(
      final List<String> keys,
      final String attribute) {

    if (UserTaskDetailsProvider.USE_METHOD_NAME.equals(attribute)) {
      return;
    }
    final var key = UserTaskDetailsProvider.ALL.equals(attribute)
        ? HandlerContract.EVERY_KEY
        : attribute;
    // writing the star in both attributes is one claim, not two
    if (!keys.contains(key)) {
      keys.add(key);
    }

  }

  /**
   * What one occurrence of a <code>version</code> attribute names, handed to VanillaBP as the
   * versions of the BPMN process the method serves.
   * <p>
   * The specifications are passed on unread. They are VanillaBP's language, the same one
   * <code>&#64;WorkflowTask</code> is written in, and reading them here would be a second opinion
   * about what a range means. Only what says nothing at all is dropped: an empty attribute and a
   * blank entry in it, which VanillaBP would otherwise have to refuse as a range it cannot parse.
   * The star both annotations default to travels like any other specification and means every
   * version, exactly as it does on a <code>&#64;WorkflowTask</code>.
   *
   * @param specifications What the attribute of one occurrence names
   * @return The specifications, empty where the attribute names nothing
   */
  private static List<String> versionsOf(
      final String[] specifications) {

    if (specifications == null) {
      return List.of();
    }
    return Arrays
        .stream(specifications)
        .filter(Objects::nonNull)
        .filter(specification -> !specification.isBlank())
        .toList();

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
   * The keys an invocation for one user task accepts: a method naming the element id or the task
   * definition runs, and where none does, the method claiming every task runs. A method carrying
   * neither attribute is registered under its own name and is therefore reached by the same two
   * keys.
   * <p>
   * The element id comes first, so a method naming it wins over a method naming the task
   * definition of the same task. The platform takes the first offered key some method serves.
   * The element id is the name VanillaBP is moving towards, because a BPMN file always has one.
   * The task definition is an attribute an adapter reads off the model today and will stop
   * needing. Once it is gone this list simply loses its second entry and nothing else changes.
   * <p>
   * Which method wins is asserted where a user sees it, by
   * <code>OneProviderForEveryUserTaskTest</code> on both platforms.
   *
   * @param taskDefinition The task's form reference, may be <code>null</code>
   * @param bpmnTaskId The task's BPMN element id, may be <code>null</code>
   * @return The keys
   */
  public static List<String> lookupKeysOf(
      final String taskDefinition,
      final String bpmnTaskId) {

    final var keys = new LinkedList<String>();
    if ((bpmnTaskId != null) && !bpmnTaskId.isBlank()) {
      keys.add(bpmnTaskId);
    }
    if ((taskDefinition != null) && !taskDefinition.isBlank()) {
      keys.add(taskDefinition);
    }
    return keys;

  }

}

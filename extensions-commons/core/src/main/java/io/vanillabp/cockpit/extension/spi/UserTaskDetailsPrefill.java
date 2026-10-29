package io.vanillabp.cockpit.extension.spi;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.vanillabp.integration.extension.spi.handler.HandlerMultiInstance;

/**
 * What a BPMS knows about a user task before the application's
 * <code>&#64;UserTaskDetailsProvider</code> method had its say.
 * <p>
 * Everything here is optional. A BPMS which cannot answer a question leaves the value
 * <code>null</code> or empty, and the Business Cockpit reports what it has. The two name fields
 * are the fallback the cockpit shows where neither a template nor a details provider produced a
 * title, which is why they hold the BPMN <em>names</em> and not the ids.
 *
 * @param bpmnProcessVersion The version of the deployed process the workflow runs on, as a
 *          person reads it. This is what the cockpit shows, so a BPMS which has a version tag may
 *          spell the tag and the counted version together here. What picks the details provider
 *          is the plain version on {@link UserTaskReference} instead
 * @param workflowId The BPMS' identifier of the workflow, where it differs from what the
 *          reference carried (a call activity, say)
 * @param subWorkflowId The identifier of the called workflow the task lives in, if any
 * @param businessId The business key the workflow was started with
 * @param bpmnTaskName The BPMN name of the user task element
 * @param bpmnProcessName The BPMN name of the process
 * @param initiator Who caused this event, where the BPMS records it. No engine records who
 *          changed a user task, so this is usually empty and the application answers it in
 *          its <code>&#64;UserTaskDetailsProvider</code> method. Leaving it empty is right
 *          for a BPMS which knows nothing: a report which names nobody after the provider
 *          ran is answered by <code>vanillabp.cockpit.initiator-source</code>, either with
 *          the constant <code>Initiator.SYSTEM</code> or by failing the report
 * @param assignee The user the task is assigned to
 * @param candidateUsers The users who may claim the task
 * @param candidateGroups The groups whose members may claim the task
 * @param dueDate When the task is due
 * @param followUpDate When somebody wants to be reminded of the task
 * @param variables The process variables a <code>&#64;TaskParam</code> parameter of a details
 *          provider is bound from. A BPMS which does not deliver variables with its user-task
 *          events passes an empty map, and such a parameter then receives <code>null</code>. A
 *          variable which the engine holds as <code>null</code> is passed on as such and
 *          reaches the parameter as <code>null</code>. An engine which lets a variable be set
 *          to nothing must not be forced to leave it out
 * @param multiInstances The multi-instance context of the task, keyed by BPMN element id and
 *          outermost first, empty where the BPMS reports none
 * @param createdAt When the BPMS created the task. It travels only with the report of an end,
 *          where the cockpit needs it if the end arrives before the creation. Leave it empty
 *          where the BPMS could tell it only by being asked, and the cockpit then takes the time
 *          of the end instead
 */
public record UserTaskDetailsPrefill(
                                     String bpmnProcessVersion,
                                     String workflowId,
                                     String subWorkflowId,
                                     String businessId,
                                     String bpmnTaskName,
                                     String bpmnProcessName,
                                     String initiator,
                                     String assignee,
                                     List<String> candidateUsers,
                                     List<String> candidateGroups,
                                     OffsetDateTime dueDate,
                                     OffsetDateTime followUpDate,
                                     Map<String, Object> variables,
                                     Map<String, HandlerMultiInstance> multiInstances,
                                     OffsetDateTime createdAt) {

  /**
   * The values without the time the task was created, which is how a BPMS half built them before
   * that time was asked for. The time is then empty.
   *
   * @param bpmnProcessVersion See the record
   * @param workflowId See the record
   * @param subWorkflowId See the record
   * @param businessId See the record
   * @param bpmnTaskName See the record
   * @param bpmnProcessName See the record
   * @param initiator See the record
   * @param assignee See the record
   * @param candidateUsers See the record
   * @param candidateGroups See the record
   * @param dueDate See the record
   * @param followUpDate See the record
   * @param variables See the record
   * @param multiInstances See the record
   */
  public UserTaskDetailsPrefill(
      final String bpmnProcessVersion,
      final String workflowId,
      final String subWorkflowId,
      final String businessId,
      final String bpmnTaskName,
      final String bpmnProcessName,
      final String initiator,
      final String assignee,
      final List<String> candidateUsers,
      final List<String> candidateGroups,
      final OffsetDateTime dueDate,
      final OffsetDateTime followUpDate,
      final Map<String, Object> variables,
      final Map<String, HandlerMultiInstance> multiInstances) {

    this(
        bpmnProcessVersion, workflowId, subWorkflowId, businessId, bpmnTaskName, bpmnProcessName, initiator, assignee, candidateUsers, candidateGroups, dueDate, followUpDate, variables, multiInstances, null);

  }

  /**
   * Takes a copy of the lists and maps, and an empty one where none was given. A process variable
   * which is <code>null</code> is kept.
   */
  public UserTaskDetailsPrefill {
    candidateUsers = candidateUsers == null ? List.of() : List.copyOf(candidateUsers);
    candidateGroups = candidateGroups == null ? List.of() : List.copyOf(candidateGroups);
    // not Map.copyOf: a process variable an engine holds as null is a value like any other,
    // and a copy which refuses it would make every BPMS half filter its own variables first
    variables = variables == null
        ? Map.of()
        : Collections.unmodifiableMap(new LinkedHashMap<>(variables));
    multiInstances = multiInstances == null ? Map.of() : Map.copyOf(multiInstances);
  }

  /**
   * Starts a prefill which names only the fields the BPMS can answer.
   *
   * @return A builder, because a BPMS half answers a handful of these fields and leaves the
   *         rest alone
   */
  public static Builder builder() {

    return new Builder();

  }

  /** Collects the fields a BPMS can answer. */
  public static final class Builder {

    private String bpmnProcessVersion;

    private String workflowId;

    private String subWorkflowId;

    private String businessId;

    private String bpmnTaskName;

    private String bpmnProcessName;

    private String initiator;

    private String assignee;

    private List<String> candidateUsers;

    private List<String> candidateGroups;

    private OffsetDateTime dueDate;

    private OffsetDateTime followUpDate;

    private Map<String, Object> variables;

    private Map<String, HandlerMultiInstance> multiInstances;

    private OffsetDateTime createdAt;

    private Builder() {
    }

    /**
     * Sets the version of the deployed process, as a person reads it.
     *
     * @param bpmnProcessVersion The version
     * @return The builder
     */
    public Builder bpmnProcessVersion(
        final String bpmnProcessVersion) {

      this.bpmnProcessVersion = bpmnProcessVersion;
      return this;

    }

    /**
     * Sets the BPMS' id of the workflow, where it differs from what the reference carried.
     *
     * @param workflowId The id of the workflow
     * @return The builder
     */
    public Builder workflowId(
        final String workflowId) {

      this.workflowId = workflowId;
      return this;

    }

    /**
     * Sets the id of the called workflow the task lives in.
     *
     * @param subWorkflowId The id of the called workflow
     * @return The builder
     */
    public Builder subWorkflowId(
        final String subWorkflowId) {

      this.subWorkflowId = subWorkflowId;
      return this;

    }

    /**
     * Sets the business key the workflow was started with.
     *
     * @param businessId The business key
     * @return The builder
     */
    public Builder businessId(
        final String businessId) {

      this.businessId = businessId;
      return this;

    }

    /**
     * Sets the BPMN name of the user task element.
     *
     * @param bpmnTaskName The name
     * @return The builder
     */
    public Builder bpmnTaskName(
        final String bpmnTaskName) {

      this.bpmnTaskName = bpmnTaskName;
      return this;

    }

    /**
     * Sets the BPMN name of the process.
     *
     * @param bpmnProcessName The name
     * @return The builder
     */
    public Builder bpmnProcessName(
        final String bpmnProcessName) {

      this.bpmnProcessName = bpmnProcessName;
      return this;

    }

    /**
     * Sets who caused this event.
     *
     * @param initiator Who caused this event, where the BPMS records it. Prefilling
     *          <code>system</code> instead of leaving it empty would be wrong: the application's
     *          provider would then read a value nobody said
     * @return The builder
     */
    public Builder initiator(
        final String initiator) {

      this.initiator = initiator;
      return this;

    }

    /**
     * Sets the user the task is assigned to.
     *
     * @param assignee The user
     * @return The builder
     */
    public Builder assignee(
        final String assignee) {

      this.assignee = assignee;
      return this;

    }

    /**
     * Sets the users who may claim the task.
     *
     * @param candidateUsers The users
     * @return The builder
     */
    public Builder candidateUsers(
        final List<String> candidateUsers) {

      this.candidateUsers = candidateUsers;
      return this;

    }

    /**
     * Sets the groups whose members may claim the task.
     *
     * @param candidateGroups The groups
     * @return The builder
     */
    public Builder candidateGroups(
        final List<String> candidateGroups) {

      this.candidateGroups = candidateGroups;
      return this;

    }

    /**
     * Sets when the task is due.
     *
     * @param dueDate The due date
     * @return The builder
     */
    public Builder dueDate(
        final OffsetDateTime dueDate) {

      this.dueDate = dueDate;
      return this;

    }

    /**
     * Sets when somebody wants to be reminded of the task.
     *
     * @param followUpDate The follow-up date
     * @return The builder
     */
    public Builder followUpDate(
        final OffsetDateTime followUpDate) {

      this.followUpDate = followUpDate;
      return this;

    }

    /**
     * Sets the process variables a <code>&#64;TaskParam</code> parameter is bound from.
     *
     * @param variables The variables, by name
     * @return The builder
     */
    public Builder variables(
        final Map<String, Object> variables) {

      this.variables = variables;
      return this;

    }

    /**
     * Sets the multi-instance context of the task.
     *
     * @param multiInstances The context, by BPMN element id and outermost first
     * @return The builder
     */
    public Builder multiInstances(
        final Map<String, HandlerMultiInstance> multiInstances) {

      this.multiInstances = multiInstances;
      return this;

    }

    /**
     * Sets when the BPMS created the task.
     *
     * @param createdAt When the BPMS created the task, where the event at hand tells it
     * @return The builder
     */
    public Builder createdAt(
        final OffsetDateTime createdAt) {

      this.createdAt = createdAt;
      return this;

    }

    /**
     * Builds the prefill. Fields which were not set stay empty.
     *
     * @return The prefill
     */
    public UserTaskDetailsPrefill build() {

      return new UserTaskDetailsPrefill(
          bpmnProcessVersion, workflowId, subWorkflowId, businessId, bpmnTaskName, bpmnProcessName, initiator, assignee, candidateUsers, candidateGroups, dueDate, followUpDate, variables, multiInstances, createdAt);

    }

  }

}

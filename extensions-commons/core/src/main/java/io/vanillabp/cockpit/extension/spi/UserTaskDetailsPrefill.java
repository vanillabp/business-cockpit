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
 * Everything here is optional: a BPMS which cannot answer a question leaves the value
 * <code>null</code> respectively empty, and the Business Cockpit reports what it has. The two
 * name fields are the fallback the cockpit shows where neither a template nor a details
 * provider produced a title, which is why they are the BPMN <em>names</em> rather than ids.
 *
 * @param bpmnProcessVersion The version of the deployed process the workflow runs on, as the
 *          BPMS counts it
 * @param workflowId The BPMS' identifier of the workflow, where it differs from what the
 *          reference carried (a call activity, say)
 * @param subWorkflowId The identifier of the called workflow the task lives in, if any
 * @param businessId The business key the workflow was started with
 * @param bpmnTaskName The BPMN name of the user task element
 * @param bpmnProcessName The BPMN name of the process
 * @param initiator Who caused this event, if the BPMS records it
 * @param assignee The user the task is assigned to
 * @param candidateUsers The users who may claim the task
 * @param candidateGroups The groups whose members may claim the task
 * @param dueDate When the task is due
 * @param followUpDate When somebody wants to be reminded of the task
 * @param variables The process variables a <code>&#64;TaskParam</code> parameter of a details
 *          provider is bound from. A BPMS which does not deliver variables with its user-task
 *          events passes an empty map, and such a parameter then receives <code>null</code>. A
 *          variable which the engine holds as <code>null</code> is passed as such and reaches
 *          the parameter as <code>null</code>: an engine which lets a variable be set to
 *          nothing must not be forced to leave it out
 * @param multiInstances The multi-instance context of the task, keyed by BPMN element id and
 *          outermost first, empty where the BPMS reports none
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
                                     Map<String, HandlerMultiInstance> multiInstances) {

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

    private Builder() {
    }

    public Builder bpmnProcessVersion(
        final String bpmnProcessVersion) {

      this.bpmnProcessVersion = bpmnProcessVersion;
      return this;

    }

    public Builder workflowId(
        final String workflowId) {

      this.workflowId = workflowId;
      return this;

    }

    public Builder subWorkflowId(
        final String subWorkflowId) {

      this.subWorkflowId = subWorkflowId;
      return this;

    }

    public Builder businessId(
        final String businessId) {

      this.businessId = businessId;
      return this;

    }

    public Builder bpmnTaskName(
        final String bpmnTaskName) {

      this.bpmnTaskName = bpmnTaskName;
      return this;

    }

    public Builder bpmnProcessName(
        final String bpmnProcessName) {

      this.bpmnProcessName = bpmnProcessName;
      return this;

    }

    public Builder initiator(
        final String initiator) {

      this.initiator = initiator;
      return this;

    }

    public Builder assignee(
        final String assignee) {

      this.assignee = assignee;
      return this;

    }

    public Builder candidateUsers(
        final List<String> candidateUsers) {

      this.candidateUsers = candidateUsers;
      return this;

    }

    public Builder candidateGroups(
        final List<String> candidateGroups) {

      this.candidateGroups = candidateGroups;
      return this;

    }

    public Builder dueDate(
        final OffsetDateTime dueDate) {

      this.dueDate = dueDate;
      return this;

    }

    public Builder followUpDate(
        final OffsetDateTime followUpDate) {

      this.followUpDate = followUpDate;
      return this;

    }

    public Builder variables(
        final Map<String, Object> variables) {

      this.variables = variables;
      return this;

    }

    public Builder multiInstances(
        final Map<String, HandlerMultiInstance> multiInstances) {

      this.multiInstances = multiInstances;
      return this;

    }

    public UserTaskDetailsPrefill build() {

      return new UserTaskDetailsPrefill(
          bpmnProcessVersion, workflowId, subWorkflowId, businessId, bpmnTaskName, bpmnProcessName, initiator, assignee, candidateUsers, candidateGroups, dueDate, followUpDate, variables, multiInstances);

    }

  }

}

package io.vanillabp.cockpit.extension.spi;

import java.time.OffsetDateTime;

/**
 * What a BPMS knows about a workflow before the application's
 * <code>&#64;WorkflowDetailsProvider</code> method had its say. Everything is optional, the
 * same way {@link UserTaskDetailsPrefill} is.
 *
 * @param bpmnProcessVersion The version of the deployed process, as a person reads it. This is
 *          what the cockpit shows, so a BPMS which has a version tag may spell the tag and the
 *          counted version together here. What picks the details provider is the plain version on
 *          {@link WorkflowReference} instead
 * @param businessId The business key the workflow was started with
 * @param bpmnProcessName The BPMN name of the process, the fallback title the cockpit shows
 * @param initiator Who started the workflow, if the BPMS records it
 * @param createdAt When the workflow was started. It travels only with the report of an end,
 *          where the cockpit needs it if the end arrives before the creation. Leave it empty
 *          where the BPMS could tell it only by being asked, and the cockpit then takes the time
 *          of the end instead
 * @param suspendedAt When the process instance which is the workflow was suspended, where the
 *          BPMS tells it with the event at hand, and empty while the instance is active. A details
 *          provider reads it. The cockpit does not: only the reports of a suspension and an
 *          activation change what it shows
 * @param suspendedBy Who suspended or activated the process instance the last time, where the
 *          BPMS tells it with the event at hand. Read the same way as <code>suspendedAt</code>
 */
public record WorkflowDetailsPrefill(
                                     String bpmnProcessVersion,
                                     String businessId,
                                     String bpmnProcessName,
                                     String initiator,
                                     OffsetDateTime createdAt,
                                     OffsetDateTime suspendedAt,
                                     String suspendedBy) {

  /**
   * The values without the suspension of the process instance, which is how a BPMS half built
   * them before the suspension was asked for. Both values are then empty.
   *
   * @param bpmnProcessVersion See the record
   * @param businessId See the record
   * @param bpmnProcessName See the record
   * @param initiator See the record
   * @param createdAt See the record
   */
  public WorkflowDetailsPrefill(
      final String bpmnProcessVersion,
      final String businessId,
      final String bpmnProcessName,
      final String initiator,
      final OffsetDateTime createdAt) {

    this(bpmnProcessVersion, businessId, bpmnProcessName, initiator, createdAt, null, null);

  }

  /**
   * The values without the time the workflow was started, which is how a BPMS half built them
   * before that time was asked for. The time is then empty.
   *
   * @param bpmnProcessVersion See the record
   * @param businessId See the record
   * @param bpmnProcessName See the record
   * @param initiator See the record
   */
  public WorkflowDetailsPrefill(
      final String bpmnProcessVersion,
      final String businessId,
      final String bpmnProcessName,
      final String initiator) {

    this(bpmnProcessVersion, businessId, bpmnProcessName, initiator, null);

  }

}

package io.vanillabp.cockpit.commons.mongo.updateinfo;

import java.time.OffsetDateTime;

/**
 * A document which records who changed it last and when. {@link UpdateInformationEventListener}
 * sets both values before each write.
 */
public interface UpdateInformationAware {

  /**
   * The user name recorded where no user triggered the update, a cron job for example.
   *
   * <p>The cockpit's SPI holds the same text as <code>Initiator.SYSTEM</code>, in the artifact
   * <code>io.vanillabp.businesscockpit:spi-for-java</code>, where a workflow module sets it on
   * a report. The two modules do not see each other, so there are two constants of one value,
   * and neither may be changed alone.
   */
  String SYSTEM_USER = "system";

  /**
   * The initiator recorded where the Business Cockpit itself caused the update, a job of its
   * own for example. It is kept apart from {@link #SYSTEM_USER} on purpose, which marks an
   * update the workflow system reported.
   */
  String COCKPIT_USER = "cockpit";

  /**
   * Records who changed the document last.
   *
   * @param userId The id of the user logged in, or {@link #SYSTEM_USER} if there is none
   */
  void setUpdatedBy(
      String userId);

  /**
   * Records when the document was changed last.
   *
   * @param timestamp The time of the change
   */
  void setUpdatedAt(
      OffsetDateTime timestamp);

}

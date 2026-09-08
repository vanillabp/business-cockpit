package io.vanillabp.cockpit.extension.springboot.test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import io.vanillabp.spi.cockpit.usertask.NotificationDelivery;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;

/**
 * The details a provider builds itself instead of enriching what it was given. Everything the
 * SPI lets business code set is answered from here; everything else is answered from the
 * prefilled object, the way an application would do it.
 */
public class OwnUserTaskDetails implements UserTaskDetails {

  /** What this object reports as the task's comment, so a test can recognize it. */
  public static final String COMMENT = "built by the application";

  private final PrefilledUserTaskDetails prefilled;

  public OwnUserTaskDetails(
      final PrefilledUserTaskDetails prefilled) {

    this.prefilled = prefilled;

  }

  @Override
  public String getId() {

    return prefilled.getId();

  }

  @Override
  public String getInitiator() {

    return "the-application";

  }

  @Override
  public String getComment() {

    return COMMENT;

  }

  @Override
  public Map<String, String> getWorkflowTitle() {

    return prefilled.getWorkflowTitle();

  }

  @Override
  public Map<String, String> getTitle() {

    return Map.of("en", "Inspect it");

  }

  @Override
  public String getTaskDefinition() {

    return prefilled.getTaskDefinition();

  }

  @Override
  public Map<String, String> getTaskDefinitionTitle() {

    return prefilled.getTaskDefinitionTitle();

  }

  @Override
  public String getAssignee() {

    return "inspector";

  }

  @Override
  public List<String> getCandidateUsers() {

    return List.of();

  }

  @Override
  public List<String> getCandidateGroups() {

    return List.of("inspectors");

  }

  @Override
  public List<String> getExcludedCandidateUsers() {

    return List.of();

  }

  @Override
  public OffsetDateTime getDueDate() {

    return null;

  }

  @Override
  public OffsetDateTime getFollowUpDate() {

    return null;

  }

  @Override
  public Map<String, Object> getDetails() {

    return Map.of("inspected", Boolean.TRUE);

  }

  @Override
  public String getDetailsFulltextSearch() {

    return "inspect";

  }

  @Override
  public List<String> getI18nLanguages() {

    return List.of("en");

  }

  @Override
  public Object getTemplateContext() {

    return null;

  }

  @Override
  public String getUiUriPath() {

    return prefilled.getUiUriPath();

  }

  @Override
  public NotificationDelivery getNotificationDelivery() {

    return NotificationDelivery.SUPPRESS;

  }

}

package io.vanillabp.spi.cockpit.workflow;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public interface PrefilledWorkflowDetails extends WorkflowDetails {
    String getEventId();

    OffsetDateTime getEventTimestamp();

    String getBpmnProcessId();
    
    String getBpmnProcessVersion();

    /**
     * Names the user who caused this report.
     *
     * <p>This is mandatory, and your workflow module is the only one who can answer it. No
     * workflow system records who started a case, and by the time this object is prefilled the
     * security context of the request is gone. So keep the user in the workflow aggregate and
     * read it back here.
     *
     * <p>Two answers are allowed: a user id, or
     * {@link io.vanillabp.spi.cockpit.Initiator#SYSTEM} where no user caused the action. Set the
     * constant as well to drop a value an adapter prefilled, because setting <code>null</code>
     * looks like a provider which did nothing.
     *
     * @param initiator The user who caused this report
     */
    void setInitiator(String initiator);

    void setComment(String comment);

    void setTitle(Map<String, String> title);

    void setDetails(Map<String, Object> details);

    void setDetailsFulltextSearch(String detailsFulltextSearch);

    void setI18nLanguages(List<String> i18nLanguages);

    void setTemplateContext(Object templateContext);

    void setAccessibleToUsers(List<String> accessibleToUsers);

    void setAccessibleToGroups(List<String> accessibleToGroups);

    /**
     * Tells the cockpit where the status page of this workflow is found.
     *
     * <p>Without this the cockpit uses the path the workflow module configured, which is the same
     * for every workflow of the module. A module whose UI URI type is <code>EXTERNAL</code> uses it
     * to hand out the address of the page in the other application which shows this very case.
     *
     * @param uiUriPath What the cockpit opens for this workflow
     */
    void setUiUriPath(String uiUriPath);

}

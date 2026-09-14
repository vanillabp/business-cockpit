package io.vanillabp.spi.cockpit.workflow;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public interface PrefilledWorkflowDetails extends WorkflowDetails {
    String getEventId();

    OffsetDateTime getEventTimestamp();

    String getBpmnProcessId();
    
    String getBpmnProcessVersion();

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

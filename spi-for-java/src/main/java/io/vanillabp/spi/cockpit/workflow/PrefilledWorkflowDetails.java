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
}

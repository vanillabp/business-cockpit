package io.vanillabp.spi.cockpit.usertask;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public interface UserTask {

    String getId();

    String getBpmnProcessId();

    String getBpmnProcessVersion();

    String getBpmnTaskId();

    String getInitiator();

    String getComment();

    Map<String, String> getWorkflowTitle();

    Map<String, String> getTitle();

    String getTaskDefinition();

    Map<String, String> getTaskDefinitionTitle();

    String getAssignee();

    List<String> getCandidateUsers();

    List<String> getCandidateGroups();

    OffsetDateTime getDueDate();

    OffsetDateTime getFollowUpDate();

    Map<String, Object> getDetails();

    List<String> getI18nLanguages();

}

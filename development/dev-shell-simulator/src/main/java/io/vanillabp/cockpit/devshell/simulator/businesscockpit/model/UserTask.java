package io.vanillabp.cockpit.devshell.simulator.businesscockpit.model;

import io.vanillabp.cockpit.gui.api.v1.Group;
import io.vanillabp.cockpit.gui.api.v1.Person;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "SIMULATOR_USERTASK")
public class UserTask {

    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "VERSION")
    private Integer version;

    @Column(name = "INITIATOR")
    private String initiator;

    @Column(name = "CREATED_AT", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @Column(name = "UPDATED_AT", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt;

    @Column(name = "ENDED_AT", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime endedAt;

    @Column(name = "WORKFLOW_MODULE_ID")
    private String workflowModuleId;

    @Column(name = "COMMENT")
    private String comment;

    @Column(name = "BPMN_PROCESS_ID")
    private String bpmnProcessId;

    @Column(name = "BPMN_PROCESS_VERSION")
    private String bpmnProcessVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "WORKFLOW_TITLE", columnDefinition = "JSON")
    private Map<String, String> workflowTitle = null;

    @Column(name = "WORKFLOW_ID")
    private String workflowId;

    @Column(name = "BUSINESS_ID")
    private String businessId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "TITLE", columnDefinition = "JSON")
    private Map<String, String> title = new HashMap<>();

    @Column(name = "BPMN_TASK_ID")
    private String bpmnTaskId;

    @Column(name = "TASK_DEFINITION")
    private String taskDefinition;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "TASK_DEFINITION_TITLE", columnDefinition = "JSON")
    private Map<String, String> taskDefinitionTitle = null;

    @Column(name = "UI_URI")
    private String uiUri;

    @Column(name = "UI_URI_TYPE")
    private String uiUriType;

    @Column(name = "WORKFLOW_MODULE_URI")
    private String workflowModuleUri;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ASSIGNEE", columnDefinition = "JSON")
    private io.vanillabp.cockpit.gui.api.v1.Person assignee;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "CANDIDATE_USERS", columnDefinition = "JSON")
    private List<Person> candidateUsers = null;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "CANDIDATE_GROUPS", columnDefinition = "JSON")
    private List<Group> candidateGroups = null;

    @Column(name = "DUE_DATE", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime dueDate;

    @Column(name = "FOLLOW_UP_DATE", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime followUpDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "DETAILS", columnDefinition = "JSON")
    private Map<String, Object> details = null;

    @Column(name = "DETAILS_FULLTEXT_SEARCH")
    private String detailsFulltextSearch;

    @Column(name = "READ_AT", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime read;


}

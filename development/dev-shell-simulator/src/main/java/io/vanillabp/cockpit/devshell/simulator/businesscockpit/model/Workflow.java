package io.vanillabp.cockpit.devshell.simulator.businesscockpit.model;

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
@Table(name = "SIMULATOR_WORKFLOW")
public class Workflow {

    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "VERSION")
    private Integer version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "INITIATOR", columnDefinition = "JSON")
    private Person initiator;

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

    @Column(name = "BUSINESS_ID")
    private String businessId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "TITLE", columnDefinition = "JSON")
    private Map<String, String> title = new HashMap<>();

    @Column(name = "UI_URI")
    private String uiUri;

    @Column(name = "UI_URI_TYPE")
    private String uiUriType;

    @Column(name = "WORKFLOW_MODULE_URI")
    private String workflowModuleUri;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ACCESSIBLE_TO_USERS", columnDefinition = "JSON")
    private List<Person> accessibleToUsers = null;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ACCESSIBLE_TO_GROUPS", columnDefinition = "JSON")
    private List<Group> accessibleToGroups = null;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "DETAILS", columnDefinition = "JSON")
    private Map<String, Object> details = null;

    @Column(name = "DETAILS_FULLTEXT_SEARCH")
    private String detailsFulltextSearch;

}

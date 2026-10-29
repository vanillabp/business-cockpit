package io.vanillabp.cockpit.devshell.simulator.businesscockpit.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A workflow module as the simulator stores it, built from its registration.
 */
@Getter
@Setter
@Entity
@Table(name = "SIMULATOR_WORKFLOW_MODULE")
public class WorkflowModule {

    /**
     * Creates an empty workflow module. JPA and the mapper need it.
     */
    public WorkflowModule() {
    }

    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "VERSION")
    private Long version;

    @Column(name = "URI")
    private String uri;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ACCESSIBLE_TO_GROUPS", columnDefinition = "JSON")
    private List<String> accessibleToGroups = null;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "GROUP_HIERARCHY", columnDefinition = "JSON")
    private Map<String, List<String>> groupHierarchy = null;

}

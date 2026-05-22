package io.vanillabp.cockpit.devshell.simulator.businesscockpit.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "SIMULATOR_WORKFLOW_MODULE")
public class WorkflowModule {

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

}

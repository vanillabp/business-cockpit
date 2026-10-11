package io.vanillabp.cockpit.extension.springboot.deployedversion;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** The workflow aggregate of the applications whose deployed version is checked. */
@Entity
@Table(name = "COCKPIT_DEPLOYED_VERSION_AGGREGATE")
public class DeployedVersionAggregate {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  public Long getId() {

    return id;

  }

  public void setId(
      final Long id) {

    this.id = id;

  }

}

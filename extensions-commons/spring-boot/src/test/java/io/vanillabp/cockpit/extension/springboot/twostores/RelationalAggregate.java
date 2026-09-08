package io.vanillabp.cockpit.extension.springboot.twostores;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** The workflow aggregate whose reports belong into the first of the application's two stores. */
@Entity
@Table(name = "COCKPIT_RELATIONAL_AGGREGATE")
public class RelationalAggregate {

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

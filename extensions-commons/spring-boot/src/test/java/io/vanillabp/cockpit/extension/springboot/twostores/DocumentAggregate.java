package io.vanillabp.cockpit.extension.springboot.twostores;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The workflow aggregate of the second store. It is an entity of the same database here, because
 * the attribution is under test and not a second persistence. The application names a store for
 * it, and VanillaBP answers with that one.
 */
@Entity
@Table(name = "COCKPIT_DOCUMENT_AGGREGATE")
public class DocumentAggregate {

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

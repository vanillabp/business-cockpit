package io.vanillabp.cockpit.extension.springboot.broken;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** The workflow aggregate of the applications which are meant not to boot. */
@Entity
@Table(name = "COCKPIT_BROKEN_AGGREGATE")
public class BrokenAggregate {

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

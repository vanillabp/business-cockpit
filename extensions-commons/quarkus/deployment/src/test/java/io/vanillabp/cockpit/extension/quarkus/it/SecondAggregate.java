package io.vanillabp.cockpit.extension.quarkus.it;

/**
 * A second workflow aggregate of the application, living in a store of its own. It is what
 * makes the attribution answerable at all: an event a BPMS observed names a BPMN process, and
 * only the workflow service serving that process says which of the two aggregates it belongs
 * to.
 */
public class SecondAggregate {

  private Long id;

  public Long getId() {

    return id;

  }

  public void setId(
      final Long id) {

    this.id = id;

  }

}

package io.vanillabp.cockpit.extension.quarkus.it;

/**
 * A second workflow aggregate of the application, kept in a store of its own. Without it there
 * would be nothing to attribute. An event a BPMS observed names a BPMN process, and only the
 * workflow service of that process says which of the two aggregates the event belongs to.
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

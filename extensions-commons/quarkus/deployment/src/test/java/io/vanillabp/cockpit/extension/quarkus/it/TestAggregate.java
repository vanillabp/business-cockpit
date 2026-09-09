package io.vanillabp.cockpit.extension.quarkus.it;

/** The workflow aggregate of the test application, with a generated id which is not a string. */
public class TestAggregate {

  private Long id;

  private String customer;

  private String note;

  public Long getId() {

    return id;

  }

  public void setId(
      final Long id) {

    this.id = id;

  }

  public String getCustomer() {

    return customer;

  }

  public void setCustomer(
      final String customer) {

    this.customer = customer;

  }

  public String getNote() {

    return note;

  }

  public void setNote(
      final String note) {

    this.note = note;

  }

}

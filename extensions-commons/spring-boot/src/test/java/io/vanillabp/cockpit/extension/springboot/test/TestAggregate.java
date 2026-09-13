package io.vanillabp.cockpit.extension.springboot.test;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The workflow aggregate of the test application. Its id is generated and not a string, so
 * that the serialized id travelling through the outbox has to be converted back on the way in.
 */
@Entity
@Table(name = "COCKPIT_TEST_AGGREGATE")
public class TestAggregate {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String customer;

  // every aggregate of every test context of this module carries one of these, because the
  // generated ids start at one in each of them and a test asking "was this one saved" would
  // otherwise read the answer given about another context's first aggregate
  private String token = UUID.randomUUID().toString();

  private String note;

  private String workflowNote;

  public Long getId() {

    return id;

  }

  public void setId(
      final Long id) {

    this.id = id;

  }

  public String getToken() {

    return token;

  }

  public void setToken(
      final String token) {

    this.token = token;

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

  public String getWorkflowNote() {

    return workflowNote;

  }

  public void setWorkflowNote(
      final String workflowNote) {

    this.workflowNote = workflowNote;

  }

}

package io.vanillabp.cockpit.devshell.simulator.businesscockpit.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * A person as the simulator stores it: the id, display names, an email address and free details.
 *
 * <p>It is stored in a JSON column. Hibernate compares the value it loaded with the value it holds
 * at the end of a transaction to find out whether the row changed. Without {@code equals} two
 * copies of the same person differ, so every transaction would write the row again.
 */
@Getter
@Setter
@EqualsAndHashCode
public class Person {

  /**
   * Creates an empty person.
   */
  public Person() {
  }

  @JsonProperty("id")
  private String id;

  @JsonProperty("display")
  private String display;

  @JsonProperty("displayShort")
  private String displayShort;

  @JsonProperty("email")
  private String email;

  @JsonProperty("details")
  private Map<String, Object> details = new HashMap<>();

}

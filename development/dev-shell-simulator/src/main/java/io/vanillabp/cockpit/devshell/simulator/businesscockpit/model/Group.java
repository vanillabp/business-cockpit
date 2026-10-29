package io.vanillabp.cockpit.devshell.simulator.businesscockpit.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

/**
 * A group as the simulator stores it: the id, a display name and free details.
 */
@Getter
@Setter
public class Group {

  /**
   * Creates an empty group.
   */
  public Group() {
  }

  @JsonProperty("id")
  private String id;

  @JsonProperty("display")
  private String display;

  @JsonProperty("details")
  private Map<String, Object> details = new HashMap<>();

}

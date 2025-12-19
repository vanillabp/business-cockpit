package io.vanillabp.cockpit.devshell.simulator.businesscockpit.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Person {

    @JsonProperty("id")
    private String id;

    @JsonProperty("display")
    private String display;

    @JsonProperty("displayShort")
    private String displayShort;

    @JsonProperty("email")
    private String email;

    @JsonProperty("avatar")
    private Integer avatar;

    @JsonProperty("details")
    private Map<String, Object> details = new HashMap<>();

}

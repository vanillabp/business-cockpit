package io.vanillabp.cockpit.devshell.simulator.usermanagement;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class User {

    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> groups;
    private Map<String, List<String>> attributes = null;
}

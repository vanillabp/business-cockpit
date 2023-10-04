package io.vanillabp.cockpit.commons.security.usercontext;

import org.springframework.security.core.Authentication;

public interface UserDetailsProvider {

    UserDetails getUserDetails(Authentication authentication);

}

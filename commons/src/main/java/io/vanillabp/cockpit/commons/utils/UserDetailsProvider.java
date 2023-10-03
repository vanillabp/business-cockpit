package io.vanillabp.cockpit.commons.utils;

import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

public interface UserDetailsProvider {

    UserDetails getUserDetails(Authentication authentication);

    Mono<UserDetails> getUserDetailsAsMono(Authentication authentication);
    
}

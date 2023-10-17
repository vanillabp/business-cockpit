package io.vanillabp.cockpit.commons.security.usercontext.reactive;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.commons.security.usercontext.UserDetailsProvider;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

public interface ReactiveUserDetailsProvider extends UserDetailsProvider {

    Mono<UserDetails> getUserDetailsAsMono(Authentication authentication);

}

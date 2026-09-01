package io.vanillabp.cockpit.users;

import io.vanillabp.cockpit.commons.security.usercontext.reactive.ReactiveUserDetailsProvider;
import org.springframework.core.Ordered;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Upserts the {@code users} document of the authenticated caller on every request, independent of
 * which {@link org.springframework.security.web.server.SecurityWebFilterChain} authenticated it.
 * <p>
 * This is deliberately a plain {@link WebFilter} bean (not attached to a specific security chain):
 * derived cockpit applications such as {@code central-ui-service} declare their own
 * {@code guiHttpSecurity}, so a login hook wired into the upstream chain would not run there. A
 * global {@code WebFilter} is applied by WebFlux to every request regardless of the active security
 * chain, so the behavior is inherited. Ordered last so the security context is already established
 * (the security chain runs upstream and writes the reactive security context).
 */
public class UserLoginUpsertWebFilter implements WebFilter, Ordered {

    /**
     * The endpoint the UI calls once per page load to determine the logged-in user - a good, low
     * frequency "login moment". Restricting the upsert to this request avoids upserting on every
     * request (which produced parallel writes and optimistic-lock conflicts on the same document).
     */
    private static final String CURRENT_USER_PATH = "/gui/api/v1/app/current-user";

    private final ReactiveUserDetailsProvider userDetailsProvider;

    private final UserLoginUpsertService userLoginUpsertService;

    public UserLoginUpsertWebFilter(
            final ReactiveUserDetailsProvider userDetailsProvider,
            final UserLoginUpsertService userLoginUpsertService) {

        this.userDetailsProvider = userDetailsProvider;
        this.userLoginUpsertService = userLoginUpsertService;

    }

    @Override
    public Mono<Void> filter(
            final ServerWebExchange exchange,
            final WebFilterChain chain) {

        if (!CURRENT_USER_PATH.equals(exchange.getRequest().getPath().value())) {
            return chain.filter(exchange);
        }

        return ReactiveSecurityContextHolder
                .getContext()
                .map(SecurityContext::getAuthentication)
                .filter(this::isAuthenticated)
                .flatMap(userDetailsProvider::getUserDetailsAsMono)
                .flatMap(userLoginUpsertService::upsertOnLogin)
                .onErrorResume(e -> Mono.empty())
                .then(chain.filter(exchange));

    }

    private boolean isAuthenticated(
            final Authentication authentication) {

        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

    }

    @Override
    public int getOrder() {

        // run after the security web filter chain so the security context is available
        return Ordered.LOWEST_PRECEDENCE;

    }

}

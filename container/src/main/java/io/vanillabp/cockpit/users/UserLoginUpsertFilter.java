package io.vanillabp.cockpit.users;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetailsProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Upserts the {@code users} document of the authenticated caller, independent of which
 * {@link org.springframework.security.web.SecurityFilterChain} authenticated it.
 * <p>
 * This is deliberately a plain filter bean and not part of a specific security chain: derived
 * cockpit applications such as {@code central-ui-service} declare their own {@code guiHttpSecurity},
 * so a login hook wired into the upstream chain would not run there. A filter bean is applied by
 * Spring Boot to every request regardless of the active security chain, so the behavior is
 * inherited. Ordered last so it runs inside the security filter chain, where the security context
 * is already established.
 */
public class UserLoginUpsertFilter extends OncePerRequestFilter implements Ordered {

    /**
     * The endpoint the UI calls once per page load to determine the logged-in user - a good, low
     * frequency "login moment". Restricting the upsert to this request avoids upserting on every
     * request (which produced parallel writes and optimistic-lock conflicts on the same document).
     */
    private static final String CURRENT_USER_PATH = "/gui/api/v1/app/current-user";

    private final UserDetailsProvider userDetailsProvider;

    private final UserLoginUpsertService userLoginUpsertService;

    public UserLoginUpsertFilter(
            final UserDetailsProvider userDetailsProvider,
            final UserLoginUpsertService userLoginUpsertService) {

        this.userDetailsProvider = userDetailsProvider;
        this.userLoginUpsertService = userLoginUpsertService;

    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {

        if (CURRENT_USER_PATH.equals(request.getRequestURI())) {
            upsertCurrentUser();
        }

        filterChain.doFilter(request, response);

    }

    private void upsertCurrentUser() {

        try {
            final var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!isAuthenticated(authentication)) {
                return;
            }
            userLoginUpsertService.upsertOnLogin(
                    userDetailsProvider.getUserDetails(authentication));
        } catch (Exception e) {
            // never let recording the login fail the request
            logger.debug("Could not record the login of the current user", e);
        }

    }

    private boolean isAuthenticated(
            final Authentication authentication) {

        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

    }

    @Override
    public int getOrder() {

        // run inside the security filter chain so the security context is available
        return Ordered.LOWEST_PRECEDENCE;

    }

}

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
 * It is a plain filter bean on purpose, and not part of one security chain. A derived cockpit
 * application such as {@code central-ui-service} declares its own {@code guiHttpSecurity}, so a
 * login hook wired into the chain upstream would not run there. Spring Boot applies a filter bean
 * to every request, whichever security chain is active, so a derived application inherits the
 * behaviour. It is ordered last, so it runs inside the security filter chain where the security
 * context already stands.
 */
public class UserLoginUpsertFilter extends OncePerRequestFilter implements Ordered {

    /**
     * The endpoint the user interface calls once per page load to find out who is logged in.
     * That is a good moment for a login, and a rare one. Upserting on every request wrote the
     * same document in parallel and produced optimistic-lock conflicts.
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

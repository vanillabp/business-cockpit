package io.vanillabp.cockpit.commons.security.jwt;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

/**
 * Logging out has to drop the JWT cookie too. Otherwise the next request would authenticate
 * again with the token, which is still valid.
 */
public class JwtLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

    private final JwtProperties properties;

    public JwtLogoutSuccessHandler(
            final JwtProperties properties) {

        this.properties = properties;

    }

    @Override
    public void onLogoutSuccess(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Authentication authentication) throws IOException, ServletException {

        JwtSecurityContextRepository.clearCookie(properties, response);

        super.onLogoutSuccess(request, response, authentication);

    }

}

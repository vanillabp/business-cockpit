package io.vanillabp.cockpit.devshell.simulator.usermanagement;

import io.vanillabp.cockpit.commons.security.jwt.JwtProperties;
import io.vanillabp.cockpit.devshell.simulator.config.Properties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.TransientSecurityContext;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class UserController {

    private final Properties properties;

    private final JwtServerSecurityContextRepository securityContextRepository;

    private final JwtProperties jwtProperties;

    @GetMapping(value = "/all/id")
    public ResponseEntity<List<String>> allUsersId() {
        return ResponseEntity.ok(properties.getUsers().stream().map(User::getId).toList());
    }

    @GetMapping("/all")
    public ResponseEntity<List<User>> allUsers() {
        return ResponseEntity.ok(properties.getUsers());
    }

    @GetMapping(value = "/", produces = "text/plain")
    public ResponseEntity<String> getUser(final HttpServletRequest request) {
        final var context = securityContextRepository.loadDeferredContext(request).get();
        if (context == null) {
            return ResponseEntity.notFound().build();
        }
        final var auth = context.getAuthentication();
        if (auth == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(auth.getName());
    }

    @PostMapping(value = "/{userId}")
    public ResponseEntity<Void> changeUser(
            final HttpServletRequest request,
            final HttpServletResponse response,
            @PathVariable("userId") final String userId) {
        if (!StringUtils.hasText(userId)) {
            return ResponseEntity.badRequest().build();
        }
        if (userId.equals("---")) {
            logout(response);
        } else {
            setJwtForChangedUser(request, response, userId);
        }
        return ResponseEntity.ok().build();
    }

    private void logout(final HttpServletResponse response) {
        final var cookie = new Cookie(jwtProperties.getCookie().getName(), "");
        cookie.setMaxAge(0);
        cookie.setPath(jwtProperties.getCookie().getPath());
        cookie.setDomain(jwtProperties.getCookie().getDomain());
        cookie.setSecure(jwtProperties.getCookie().isSecure());
        cookie.setHttpOnly(true);
        final var sameSite = getSecurityCookieSameSiteFromEnum();
        if (sameSite != null) {
            cookie.setAttribute("SameSite", sameSite);
        }
        response.addCookie(cookie);
    }

    private void setJwtForChangedUser(
            final HttpServletRequest request,
            final HttpServletResponse response,
            @PathVariable("userId") final String userId) {

        final var user = properties.getUser(userId);
        final var authorities =
                Optional.ofNullable(user.getGroups())
                        .map(groups -> groups.stream().map(SimpleGrantedAuthority::new).toList())
                        .orElse(List.of());
        final var userDetails =
                new org.springframework.security.core.userdetails.User(user.getId(), "", authorities);
        final var context =
                new TransientSecurityContext(
                        new UsernamePasswordAuthenticationToken(userDetails, "", authorities));
        securityContextRepository.saveContext(context, request, response);
    }

    private String getSecurityCookieSameSiteFromEnum() {
        final var sameSite = this.jwtProperties.getCookie().getSameSite();
        return sameSite == null ? null : sameSite.attributeValue();
    }
}

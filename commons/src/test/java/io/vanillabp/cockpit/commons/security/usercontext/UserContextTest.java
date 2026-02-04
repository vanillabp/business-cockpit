package io.vanillabp.cockpit.commons.security.usercontext;

import io.vanillabp.cockpit.commons.exceptions.BcUnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserContextTest {

    private UserDetailsProvider mockProvider;
    private UserContext userContext;

    @BeforeEach
    void setUp() {
        mockProvider = mock(UserDetailsProvider.class);
        userContext = new UserContext(mockProvider);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getUserLoggedIn_withNoSecurityContext_throwsUnauthorized() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> userContext.getUserLoggedIn())
                .isInstanceOf(BcUnauthorizedException.class)
                .hasMessage("No security context");
    }

    @Test
    void getUserLoggedIn_withUnauthenticatedUser_throwsUnauthorized() {
        TestingAuthenticationToken token = new TestingAuthenticationToken("user", "pass");
        token.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(token);

        assertThatThrownBy(() -> userContext.getUserLoggedIn())
                .isInstanceOf(BcUnauthorizedException.class)
                .hasMessage("User anonymous");
    }

    @Test
    void getUserLoggedIn_withAnonymousToken_returnsNull() {
        AnonymousAuthenticationToken token = new AnonymousAuthenticationToken(
                "key",
                "anonymous",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(token);

        String result = userContext.getUserLoggedIn();

        assertThat(result).isNull();
    }

    @Test
    void getUserLoggedIn_withAuthenticatedUser_returnsUserId() {
        TestingAuthenticationToken token = new TestingAuthenticationToken("user", "pass");
        token.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(token);

        UserDetails mockDetails = mock(UserDetails.class);
        when(mockDetails.getId()).thenReturn("user-123");
        when(mockProvider.getUserDetails(any(Authentication.class))).thenReturn(mockDetails);

        String result = userContext.getUserLoggedIn();

        assertThat(result).isEqualTo("user-123");
    }

    @Test
    void getUserLoggedIn_withNullUserDetails_returnsNull() {
        TestingAuthenticationToken token = new TestingAuthenticationToken("user", "pass");
        token.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(token);

        when(mockProvider.getUserDetails(any(Authentication.class))).thenReturn(null);

        String result = userContext.getUserLoggedIn();

        assertThat(result).isNull();
    }

    @Test
    void getUserLoggedInDetails_withNoSecurityContext_throwsUnauthorized() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> userContext.getUserLoggedInDetails())
                .isInstanceOf(BcUnauthorizedException.class)
                .hasMessage("No security context");
    }

    @Test
    void getUserLoggedInDetails_withUnauthenticatedUser_throwsUnauthorized() {
        TestingAuthenticationToken token = new TestingAuthenticationToken("user", "pass");
        token.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(token);

        assertThatThrownBy(() -> userContext.getUserLoggedInDetails())
                .isInstanceOf(BcUnauthorizedException.class)
                .hasMessage("User anonymous");
    }

    @Test
    void getUserLoggedInDetails_withAnonymousToken_returnsNull() {
        AnonymousAuthenticationToken token = new AnonymousAuthenticationToken(
                "key",
                "anonymous",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(token);

        UserDetails result = userContext.getUserLoggedInDetails();

        assertThat(result).isNull();
    }

    @Test
    void getUserLoggedInDetails_withAuthenticatedUser_returnsUserDetails() {
        TestingAuthenticationToken token = new TestingAuthenticationToken("user", "pass");
        token.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(token);

        UserDetails mockDetails = mock(UserDetails.class);
        when(mockProvider.getUserDetails(any(Authentication.class))).thenReturn(mockDetails);

        UserDetails result = userContext.getUserLoggedInDetails();

        assertThat(result).isSameAs(mockDetails);
    }
}

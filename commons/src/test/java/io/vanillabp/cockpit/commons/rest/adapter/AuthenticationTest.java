package io.vanillabp.cockpit.commons.rest.adapter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationTest {

    @Test
    void defaultValues_areSetCorrectly() {
        Authentication auth = new Authentication();

        assertThat(auth.isBasic()).isFalse();
        assertThat(auth.getUsername()).isNull();
        assertThat(auth.getPassword()).isNull();
        assertThat(auth.getOauth()).isNull();
    }

    @Test
    void setBasic_toTrue_updatesValue() {
        Authentication auth = new Authentication();
        auth.setBasic(true);

        assertThat(auth.isBasic()).isTrue();
    }

    @Test
    void setUsername_updatesValue() {
        Authentication auth = new Authentication();
        auth.setUsername("testuser");

        assertThat(auth.getUsername()).isEqualTo("testuser");
    }

    @Test
    void setPassword_updatesValue() {
        Authentication auth = new Authentication();
        auth.setPassword("testpassword");

        assertThat(auth.getPassword()).isEqualTo("testpassword");
    }

    @Test
    void setOauth_updatesValue() {
        Authentication auth = new Authentication();
        OauthAuthentication oauth = new OauthAuthentication();
        oauth.setClientId("client-id");
        oauth.setClientSecret("client-secret");

        auth.setOauth(oauth);

        assertThat(auth.getOauth()).isSameAs(oauth);
        assertThat(auth.getOauth().getClientId()).isEqualTo("client-id");
    }

    @Test
    void basicAuthentication_fullConfiguration() {
        Authentication auth = new Authentication();
        auth.setBasic(true);
        auth.setUsername("admin");
        auth.setPassword("admin-secret");

        assertThat(auth.isBasic()).isTrue();
        assertThat(auth.getUsername()).isEqualTo("admin");
        assertThat(auth.getPassword()).isEqualTo("admin-secret");
        assertThat(auth.getOauth()).isNull();
    }

    @Test
    void oauthAuthentication_fullConfiguration() {
        Authentication auth = new Authentication();
        OauthAuthentication oauth = new OauthAuthentication();
        oauth.setBaseUrl("https://auth.example.com/oauth/token");
        oauth.setClientId("my-client");
        oauth.setClientSecret("my-secret");
        oauth.setBasic(true);

        auth.setOauth(oauth);

        assertThat(auth.isBasic()).isFalse();
        assertThat(auth.getOauth()).isNotNull();
        assertThat(auth.getOauth().getClientId()).isEqualTo("my-client");
        assertThat(auth.getOauth().getClientSecret()).isEqualTo("my-secret");
        assertThat(auth.getOauth().isBasic()).isTrue();
    }
}

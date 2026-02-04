package io.vanillabp.cockpit.commons.security.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.server.Cookie.SameSite;

import static org.assertj.core.api.Assertions.assertThat;

class JwtCookieTest {

    @Test
    void defaultValues_areSetCorrectly() {
        JwtCookie cookie = new JwtCookie();

        assertThat(cookie.getName()).isEqualTo("bc");
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getExpiresDuration()).isEqualTo("PT12H");
        assertThat(cookie.getDomain()).isNull();
        assertThat(cookie.getSameSite()).isNull();
        assertThat(cookie.isSecure()).isFalse();
    }

    @Test
    void setName_updatesValue() {
        JwtCookie cookie = new JwtCookie();
        cookie.setName("custom-cookie");

        assertThat(cookie.getName()).isEqualTo("custom-cookie");
    }

    @Test
    void setDomain_updatesValue() {
        JwtCookie cookie = new JwtCookie();
        cookie.setDomain("example.com");

        assertThat(cookie.getDomain()).isEqualTo("example.com");
    }

    @Test
    void setPath_updatesValue() {
        JwtCookie cookie = new JwtCookie();
        cookie.setPath("/api");

        assertThat(cookie.getPath()).isEqualTo("/api");
    }

    @Test
    void setSameSite_toStrict_updatesValue() {
        JwtCookie cookie = new JwtCookie();
        cookie.setSameSite(SameSite.STRICT);

        assertThat(cookie.getSameSite()).isEqualTo(SameSite.STRICT);
    }

    @Test
    void setSameSite_toLax_updatesValue() {
        JwtCookie cookie = new JwtCookie();
        cookie.setSameSite(SameSite.LAX);

        assertThat(cookie.getSameSite()).isEqualTo(SameSite.LAX);
    }

    @Test
    void setSameSite_toNone_updatesValue() {
        JwtCookie cookie = new JwtCookie();
        cookie.setSameSite(SameSite.NONE);

        assertThat(cookie.getSameSite()).isEqualTo(SameSite.NONE);
    }

    @Test
    void setSecure_toTrue_updatesValue() {
        JwtCookie cookie = new JwtCookie();
        cookie.setSecure(true);

        assertThat(cookie.isSecure()).isTrue();
    }

    @Test
    void setExpiresDuration_updatesValue() {
        JwtCookie cookie = new JwtCookie();
        cookie.setExpiresDuration("PT24H");

        assertThat(cookie.getExpiresDuration()).isEqualTo("PT24H");
    }

    @Test
    void fullConfiguration_worksCorrectly() {
        JwtCookie cookie = new JwtCookie();
        cookie.setName("session");
        cookie.setDomain(".example.com");
        cookie.setPath("/app");
        cookie.setSameSite(SameSite.STRICT);
        cookie.setSecure(true);
        cookie.setExpiresDuration("PT1H");

        assertThat(cookie.getName()).isEqualTo("session");
        assertThat(cookie.getDomain()).isEqualTo(".example.com");
        assertThat(cookie.getPath()).isEqualTo("/app");
        assertThat(cookie.getSameSite()).isEqualTo(SameSite.STRICT);
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getExpiresDuration()).isEqualTo("PT1H");
    }
}

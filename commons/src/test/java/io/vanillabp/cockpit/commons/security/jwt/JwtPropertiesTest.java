package io.vanillabp.cockpit.commons.security.jwt;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    @Test
    void getCookie_returnsDefaultCookie() {
        JwtProperties properties = new JwtProperties();

        assertThat(properties.getCookie()).isNotNull();
    }

    @Test
    void setCookie_updatesValue() {
        JwtProperties properties = new JwtProperties();
        JwtCookie cookie = new JwtCookie();
        cookie.setName("custom-cookie");

        properties.setCookie(cookie);

        assertThat(properties.getCookie()).isSameAs(cookie);
    }

    @Test
    void getHmacSHA256Base64_withoutValue_throwsRuntimeException() {
        JwtProperties properties = new JwtProperties();

        assertThatThrownBy(properties::getHmacSHA256Base64)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No property 'business-cockpit.jwt.hmacSHA256-base64' set!");
    }

    @Test
    void getHmacSHA256Base64_withValue_returnsValue() {
        JwtProperties properties = new JwtProperties();
        String testKey = Base64.getEncoder().encodeToString("test-secret-key-1234".getBytes());
        properties.setHmacSHA256Base64(testKey);

        assertThat(properties.getHmacSHA256Base64()).isEqualTo(testKey);
    }

    @Test
    void setHmacSHA256Base64_updatesValue() {
        JwtProperties properties = new JwtProperties();
        String testKey = "dGVzdC1rZXk="; // "test-key" in base64
        properties.setHmacSHA256Base64(testKey);

        assertThat(properties.getHmacSHA256Base64()).isEqualTo(testKey);
    }

    @Test
    void getHmacSHA256_decodesBase64Key() {
        JwtProperties properties = new JwtProperties();
        byte[] originalKey = "test-secret-key".getBytes();
        String base64Key = Base64.getEncoder().encodeToString(originalKey);
        properties.setHmacSHA256Base64(base64Key);

        byte[] result = properties.getHmacSHA256();

        assertThat(result).isEqualTo(originalKey);
    }

    @Test
    void getHmacSHA256Base64_withEmptyValue_throwsRuntimeException() {
        JwtProperties properties = new JwtProperties();
        properties.setHmacSHA256Base64("");

        assertThatThrownBy(properties::getHmacSHA256Base64)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No property 'business-cockpit.jwt.hmacSHA256-base64' set!");
    }

    @Test
    void getHmacSHA256Base64_withBlankValue_throwsRuntimeException() {
        JwtProperties properties = new JwtProperties();
        properties.setHmacSHA256Base64("   ");

        assertThatThrownBy(properties::getHmacSHA256Base64)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No property 'business-cockpit.jwt.hmacSHA256-base64' set!");
    }
}

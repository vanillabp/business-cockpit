package io.vanillabp.cockpit.commons.rest.adapter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProxyTest {

    @Test
    void defaultValues_areSetCorrectly() {
        Proxy proxy = new Proxy();

        assertThat(proxy.getHost()).isNull();
        assertThat(proxy.getPort()).isZero();
        assertThat(proxy.getUsername()).isNull();
        assertThat(proxy.getPassword()).isNull();
    }

    @Test
    void setHost_updatesValue() {
        Proxy proxy = new Proxy();
        proxy.setHost("proxy.example.com");

        assertThat(proxy.getHost()).isEqualTo("proxy.example.com");
    }

    @Test
    void setPort_updatesValue() {
        Proxy proxy = new Proxy();
        proxy.setPort(8080);

        assertThat(proxy.getPort()).isEqualTo(8080);
    }

    @Test
    void setUsername_updatesValue() {
        Proxy proxy = new Proxy();
        proxy.setUsername("proxyuser");

        assertThat(proxy.getUsername()).isEqualTo("proxyuser");
    }

    @Test
    void setPassword_updatesValue() {
        Proxy proxy = new Proxy();
        proxy.setPassword("secret");

        assertThat(proxy.getPassword()).isEqualTo("secret");
    }

    @Test
    void fullConfiguration_worksCorrectly() {
        Proxy proxy = new Proxy();
        proxy.setHost("corporate-proxy.example.com");
        proxy.setPort(3128);
        proxy.setUsername("admin");
        proxy.setPassword("admin-secret");

        assertThat(proxy.getHost()).isEqualTo("corporate-proxy.example.com");
        assertThat(proxy.getPort()).isEqualTo(3128);
        assertThat(proxy.getUsername()).isEqualTo("admin");
        assertThat(proxy.getPassword()).isEqualTo("admin-secret");
    }
}

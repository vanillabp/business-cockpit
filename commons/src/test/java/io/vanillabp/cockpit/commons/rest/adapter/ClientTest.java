package io.vanillabp.cockpit.commons.rest.adapter;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClientTest {

    @Test
    void toBeDefinedUrl_constant_hasExpectedValue() {
        assertThat(Client.TO_BE_DEFINED_URL).isEqualTo("to-be-defined");
    }

    @Test
    void defaultValues_areSetCorrectly() {
        Client client = new Client();

        assertThat(client.getBaseUrl()).isEqualTo(Client.TO_BE_DEFINED_URL);
        assertThat(client.getConnectTimeout()).isEqualTo(1500);
        assertThat(client.getReadTimeout()).isEqualTo(10000);
        assertThat(client.isLog()).isFalse();
        assertThat(client.getProxy()).isNull();
        assertThat(client.getAuthentication()).isNull();
        assertThat(client.isVerifySsl()).isTrue();
        assertThat(client.getSslTruststoreFilename()).isNull();
        assertThat(client.getSslTruststorePassword()).isNull();
        assertThat(client.getAdditionalGetParameters()).isNull();
        assertThat(client.getRetry()).isNull();
    }

    @Test
    void isInitialized_withDefaultUrl_returnsFalse() {
        Client client = new Client();

        assertThat(client.isInitialized()).isFalse();
    }

    @Test
    void isInitialized_withNullUrl_returnsFalse() {
        Client client = new Client();
        client.setBaseUrl(null);

        assertThat(client.isInitialized()).isFalse();
    }

    @Test
    void isInitialized_withCustomUrl_returnsTrue() {
        Client client = new Client();
        client.setBaseUrl("https://api.example.com");

        assertThat(client.isInitialized()).isTrue();
    }

    @Test
    void useProxy_withNoProxy_returnsFalse() {
        Client client = new Client();

        assertThat(client.useProxy()).isFalse();
    }

    @Test
    void useProxy_withProxyButNoHost_returnsFalse() {
        Client client = new Client();
        Proxy proxy = new Proxy();
        client.setProxy(proxy);

        assertThat(client.useProxy()).isFalse();
    }

    @Test
    void useProxy_withProxyAndHost_returnsTrue() {
        Client client = new Client();
        Proxy proxy = new Proxy();
        proxy.setHost("proxy.example.com");
        client.setProxy(proxy);

        assertThat(client.useProxy()).isTrue();
    }

    @Test
    void setBaseUrl_updatesValue() {
        Client client = new Client();
        client.setBaseUrl("https://api.example.com");

        assertThat(client.getBaseUrl()).isEqualTo("https://api.example.com");
    }

    @Test
    void setConnectTimeout_updatesValue() {
        Client client = new Client();
        client.setConnectTimeout(3000);

        assertThat(client.getConnectTimeout()).isEqualTo(3000);
    }

    @Test
    void setReadTimeout_updatesValue() {
        Client client = new Client();
        client.setReadTimeout(30000);

        assertThat(client.getReadTimeout()).isEqualTo(30000);
    }

    @Test
    void setLog_updatesValue() {
        Client client = new Client();
        client.setLog(true);

        assertThat(client.isLog()).isTrue();
    }

    @Test
    void setProxy_updatesValue() {
        Client client = new Client();
        Proxy proxy = new Proxy();
        proxy.setHost("proxy.example.com");
        proxy.setPort(8080);

        client.setProxy(proxy);

        assertThat(client.getProxy()).isSameAs(proxy);
        assertThat(client.getProxy().getHost()).isEqualTo("proxy.example.com");
    }

    @Test
    void setAuthentication_updatesValue() {
        Client client = new Client();
        Authentication auth = new Authentication();
        auth.setUsername("user");
        auth.setPassword("pass");

        client.setAuthentication(auth);

        assertThat(client.getAuthentication()).isSameAs(auth);
    }

    @Test
    void setVerifySsl_toFalse_updatesValue() {
        Client client = new Client();
        client.setVerifySsl(false);

        assertThat(client.isVerifySsl()).isFalse();
    }

    @Test
    void setSslTruststoreFilename_updatesValue() {
        Client client = new Client();
        client.setSslTruststoreFilename("/path/to/truststore.jks");

        assertThat(client.getSslTruststoreFilename()).isEqualTo("/path/to/truststore.jks");
    }

    @Test
    void setSslTruststorePassword_updatesValue() {
        Client client = new Client();
        client.setSslTruststorePassword("changeit");

        assertThat(client.getSslTruststorePassword()).isEqualTo("changeit");
    }

    @Test
    void setAdditionalGetParameters_updatesValue() {
        Client client = new Client();
        Map<String, String> params = Map.of("key1", "value1", "key2", "value2");

        client.setAdditionalGetParameters(params);

        assertThat(client.getAdditionalGetParameters()).isEqualTo(params);
    }

    @Test
    void setRetry_updatesValue() {
        Client client = new Client();
        Retry retry = new Retry();
        retry.setEnabled(true);
        retry.setMaxAttempts(3);

        client.setRetry(retry);

        assertThat(client.getRetry()).isSameAs(retry);
        assertThat(client.getRetry().isEnabled()).isTrue();
    }

    @Test
    void fullConfiguration_worksCorrectly() {
        Client client = new Client();
        client.setBaseUrl("https://api.example.com/v1");
        client.setConnectTimeout(5000);
        client.setReadTimeout(60000);
        client.setLog(true);
        client.setVerifySsl(false);
        client.setSslTruststoreFilename("/certs/truststore.jks");
        client.setSslTruststorePassword("secret");

        Proxy proxy = new Proxy();
        proxy.setHost("proxy.corp.com");
        proxy.setPort(3128);
        client.setProxy(proxy);

        Authentication auth = new Authentication();
        auth.setBasic(true);
        auth.setUsername("apiuser");
        auth.setPassword("apipass");
        client.setAuthentication(auth);

        Retry retry = new Retry();
        retry.setEnabled(true);
        retry.setMaxAttempts(5);
        client.setRetry(retry);

        client.setAdditionalGetParameters(Map.of("api_key", "12345"));

        assertThat(client.isInitialized()).isTrue();
        assertThat(client.useProxy()).isTrue();
        assertThat(client.getBaseUrl()).isEqualTo("https://api.example.com/v1");
        assertThat(client.getConnectTimeout()).isEqualTo(5000);
        assertThat(client.getReadTimeout()).isEqualTo(60000);
        assertThat(client.isLog()).isTrue();
        assertThat(client.isVerifySsl()).isFalse();
        assertThat(client.getAdditionalGetParameters()).containsEntry("api_key", "12345");
    }
}

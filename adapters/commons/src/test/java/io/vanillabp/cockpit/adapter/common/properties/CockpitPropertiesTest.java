package io.vanillabp.cockpit.adapter.common.properties;

import io.vanillabp.cockpit.commons.kafka.KafkaProperties;
import io.vanillabp.cockpit.commons.rest.adapter.Client;
import io.vanillabp.cockpit.commons.security.jwt.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CockpitPropertiesTest {

    private CockpitProperties properties;

    @BeforeEach
    void setUp() {
        properties = new CockpitProperties();
    }

    @Test
    void defaultUserTasksEnabled_isTrue() {
        assertThat(properties.isUserTasksEnabled()).isTrue();
    }

    @Test
    void defaultWorkflowListEnabled_isTrue() {
        assertThat(properties.isWorkflowListEnabled()).isTrue();
    }

    @Test
    void defaultKafka_isNotNull() {
        assertThat(properties.getKafka()).isNotNull();
    }

    @Test
    void defaultJwt_isNotNull() {
        assertThat(properties.getJwt()).isNotNull();
    }

    @Test
    void setAndGetRest() {
        Client client = new Client();
        properties.setRest(client);
        assertThat(properties.getRest()).isEqualTo(client);
    }

    @Test
    void setAndGetKafka() {
        KafkaProperties kafka = new KafkaProperties();
        properties.setKafka(kafka);
        assertThat(properties.getKafka()).isEqualTo(kafka);
    }

    @Test
    void setAndGetUserTasksEnabled() {
        properties.setUserTasksEnabled(false);
        assertThat(properties.isUserTasksEnabled()).isFalse();

        properties.setUserTasksEnabled(true);
        assertThat(properties.isUserTasksEnabled()).isTrue();
    }

    @Test
    void setAndGetWorkflowListEnabled() {
        properties.setWorkflowListEnabled(false);
        assertThat(properties.isWorkflowListEnabled()).isFalse();

        properties.setWorkflowListEnabled(true);
        assertThat(properties.isWorkflowListEnabled()).isTrue();
    }

    @Test
    void setAndGetTemplateLoaderPath() {
        properties.setTemplateLoaderPath("/templates");
        assertThat(properties.getTemplateLoaderPath()).isEqualTo("/templates");
    }

    @Test
    void setAndGetJwt() {
        JwtProperties jwt = new JwtProperties();
        properties.setJwt(jwt);
        assertThat(properties.getJwt()).isEqualTo(jwt);
    }

    @Test
    void defaultRest_isNull() {
        assertThat(properties.getRest()).isNull();
    }

    @Test
    void defaultTemplateLoaderPath_isNull() {
        assertThat(properties.getTemplateLoaderPath()).isNull();
    }
}

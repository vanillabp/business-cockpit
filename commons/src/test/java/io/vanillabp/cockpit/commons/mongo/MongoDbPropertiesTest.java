package io.vanillabp.cockpit.commons.mongo;

import io.vanillabp.cockpit.commons.utils.AsyncProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MongoDbPropertiesTest {

    @Test
    void defaultValues_areSetCorrectly() {
        MongoDbProperties properties = new MongoDbProperties();

        assertThat(properties.getUseTimeout()).isEqualTo("PT5S");
        assertThat(properties.isUseTls()).isFalse();
        assertThat(properties.getMode()).isEqualTo(MongoDbProperties.Mode.MONGODB_4_8);
        assertThat(properties.getChangeStreamExecutor()).isNotNull();
    }

    @Test
    void setUseTimeout_updatesValue() {
        MongoDbProperties properties = new MongoDbProperties();
        properties.setUseTimeout("PT10S");

        assertThat(properties.getUseTimeout()).isEqualTo("PT10S");
    }

    @Test
    void setUseTls_updatesValue() {
        MongoDbProperties properties = new MongoDbProperties();
        properties.setUseTls(true);

        assertThat(properties.isUseTls()).isTrue();
    }

    @Test
    void setMode_toMongoDb48_updatesValue() {
        MongoDbProperties properties = new MongoDbProperties();
        properties.setMode(MongoDbProperties.Mode.MONGODB_4_8);

        assertThat(properties.getMode()).isEqualTo(MongoDbProperties.Mode.MONGODB_4_8);
    }

    @Test
    void setMode_toAzureCosmos_updatesValue() {
        MongoDbProperties properties = new MongoDbProperties();
        properties.setMode(MongoDbProperties.Mode.AZURE_COSMOS_MONGO_4_2);

        assertThat(properties.getMode()).isEqualTo(MongoDbProperties.Mode.AZURE_COSMOS_MONGO_4_2);
    }

    @Test
    void setChangeStreamExecutor_updatesValue() {
        MongoDbProperties properties = new MongoDbProperties();
        AsyncProperties asyncProperties = new AsyncProperties();
        asyncProperties.setCorePoolSize(5);

        properties.setChangeStreamExecutor(asyncProperties);

        assertThat(properties.getChangeStreamExecutor()).isSameAs(asyncProperties);
        assertThat(properties.getChangeStreamExecutor().getCorePoolSize()).isEqualTo(5);
    }

    @Test
    void mode_enumValues_existAsExpected() {
        MongoDbProperties.Mode[] modes = MongoDbProperties.Mode.values();

        assertThat(modes).hasSize(2);
        assertThat(modes).contains(MongoDbProperties.Mode.MONGODB_4_8, MongoDbProperties.Mode.AZURE_COSMOS_MONGO_4_2);
    }

    @Test
    void mode_valueOf_returnsCorrectEnum() {
        assertThat(MongoDbProperties.Mode.valueOf("MONGODB_4_8")).isEqualTo(MongoDbProperties.Mode.MONGODB_4_8);
        assertThat(MongoDbProperties.Mode.valueOf("AZURE_COSMOS_MONGO_4_2")).isEqualTo(MongoDbProperties.Mode.AZURE_COSMOS_MONGO_4_2);
    }
}

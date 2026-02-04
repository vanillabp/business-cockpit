package io.vanillabp.cockpit.commons.mongo.updateinfo;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateInformationAwareTest {

    @Test
    void systemUser_hasExpectedValue() {
        assertThat(UpdateInformationAware.SYSTEM_USER).isEqualTo("system");
    }

    @Test
    void implementingClass_canSetUpdatedBy() {
        TestUpdateInfoEntity entity = new TestUpdateInfoEntity();

        entity.setUpdatedBy("user-123");

        assertThat(entity.getUpdatedBy()).isEqualTo("user-123");
    }

    @Test
    void implementingClass_canSetUpdatedAt() {
        TestUpdateInfoEntity entity = new TestUpdateInfoEntity();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        entity.setUpdatedAt(now);

        assertThat(entity.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void implementingClass_canUseSystemUser() {
        TestUpdateInfoEntity entity = new TestUpdateInfoEntity();

        entity.setUpdatedBy(UpdateInformationAware.SYSTEM_USER);

        assertThat(entity.getUpdatedBy()).isEqualTo("system");
    }

    private static class TestUpdateInfoEntity implements UpdateInformationAware {
        private String updatedBy;
        private OffsetDateTime updatedAt;

        @Override
        public void setUpdatedBy(String userId) {
            this.updatedBy = userId;
        }

        @Override
        public void setUpdatedAt(OffsetDateTime timestamp) {
            this.updatedAt = timestamp;
        }

        public String getUpdatedBy() {
            return updatedBy;
        }

        public OffsetDateTime getUpdatedAt() {
            return updatedAt;
        }
    }
}

package io.vanillabp.cockpit.notification;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AbstractTemplatingNotificationServiceTest {

    /** A minimal medium whose templates live under {@code templates/notification/test-medium/}. */
    private static class TestNotificationService extends AbstractTemplatingNotificationService {
        TestNotificationService(final NotificationProperties properties) {
            super(properties);
        }

        @Override
        public String getType() {
            return "test-medium";
        }

        @Override
        public Map<String, String> getName() {
            return Map.of("en", "Test");
        }

        @Override
        public List<RecipientConfiguration> getRecipientConfiguration(String userId) {
            return List.of();
        }

        @Override
        public void saveRecipientConfiguration(String userId, Map<String, String> configurationValues) {
        }

        @Override
        public void sendNotification(List<String> userIds, io.vanillabp.cockpit.tasklist.model.UserTask userTask) {
        }
    }

    private static UserDetails userDetails() {
        return new UserDetails() {
            public String getId() {
                return "u1";
            }

            public String getEmail() {
                return "u1@example.org";
            }

            public String getDisplay() {
                return "User One";
            }

            public String getDisplayShort() {
                return "U1";
            }

            public List<String> getAuthorities() {
                return List.of();
            }
        };
    }

    private TestNotificationService service() {
        // resolve the template path of this medium explicitly to the test template directory
        final var properties = new NotificationProperties();
        properties.setTemplates(Map.of("test-medium", "templates/notification/test-medium/"));
        return new TestNotificationService(properties);
    }

    @Test
    void render_exposesContext_notificationType_andForcedHint() {
        final var rendered = service().render(
                "probe",
                Map.of("greeting", "hi"),
                userDetails(),
                NotificationType.CREATED,
                true);

        assertNotNull(rendered);
        assertTrue(rendered.contains("type=CREATED"), rendered);
        assertTrue(rendered.contains("greeting=hi"), rendered);
        assertTrue(rendered.contains("FORCED"), rendered);
    }

    @Test
    void render_notForced_rendersNotForcedBranch() {
        final var rendered = service().render(
                "probe",
                Map.of("greeting", "hi"),
                userDetails(),
                NotificationType.COMPLETED,
                false);

        assertNotNull(rendered);
        assertTrue(rendered.contains("type=COMPLETED"), rendered);
        assertTrue(rendered.contains("NOT_FORCED"), rendered);
    }

    @Test
    void render_withLocale_prefersLocalizedTemplate_andFallsBack() {
        // German locale -> probe_de.ftl exists
        final var german = service().render(
                "probe",
                Map.of("greeting", "hallo"),
                userDetails(),
                NotificationType.CREATED,
                false,
                java.util.Locale.GERMAN);
        assertNotNull(german);
        assertTrue(german.startsWith("DE "), german);

        // French locale -> no probe_fr.ftl -> falls back to locale-less probe.ftl
        final var fallback = service().render(
                "probe",
                Map.of("greeting", "bonjour"),
                userDetails(),
                NotificationType.CREATED,
                false,
                java.util.Locale.FRENCH);
        assertNotNull(fallback);
        assertTrue(fallback.startsWith("type="), fallback);
    }

    @Test
    void renderContent_reportsHtml_forFtlhTemplate() {
        final var rendered = service().renderContent(
                "probe-html",
                Map.of("greeting", "hi"),
                userDetails(),
                NotificationType.CREATED,
                false,
                java.util.Locale.ENGLISH);
        assertNotNull(rendered);
        assertTrue(rendered.html(), "a .ftlh template must be reported as HTML");
        assertTrue(rendered.content().contains("<p>"), rendered.content());
    }

    @Test
    void renderContent_reportsPlain_forFtlTemplate() {
        final var rendered = service().renderContent(
                "probe",
                Map.of("greeting", "hi"),
                userDetails(),
                NotificationType.CREATED,
                false,
                java.util.Locale.ENGLISH);
        assertNotNull(rendered);
        org.junit.jupiter.api.Assertions.assertFalse(rendered.html(), "a .ftl template is plain text");
    }

    @Test
    void render_missingTemplate_returnsNull() {
        assertNull(service().render(
                "does-not-exist",
                Map.of("greeting", "hi"),
                userDetails(),
                NotificationType.CREATED,
                false));
    }

}

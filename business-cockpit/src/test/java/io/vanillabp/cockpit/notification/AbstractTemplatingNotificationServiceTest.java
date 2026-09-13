package io.vanillabp.cockpit.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.integration.test.utils.CapturedOutput;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(SuppressOutputExtension.class)
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
        return service("classpath:templates/notification/test-medium/");
    }

    /** A medium whose templates are configured to come from the given directory. */
    private TestNotificationService service(final String templateDirectory) {
        final var properties = new NotificationProperties();
        properties.setTemplates(Map.of("test-medium", templateDirectory));
        final var service = new TestNotificationService(properties);
        // outside Spring nobody calls the annotated method, and what it refuses is what these
        // tests are about
        service.checkTheTemplateDirectory();
        return service;
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
    void render_missingTemplate_returnsNull(final CapturedOutput output) {
        assertNull(service().render(
                "does-not-exist",
                Map.of("greeting", "hi"),
                userDetails(),
                NotificationType.CREATED,
                false));

        // the operator reads the directory the way they wrote it, prefix included
        assertTrue(
                output.getAll().contains("classpath:templates/notification/test-medium/"),
                output.getAll());
    }

    @Test
    void templatesPath_defaultsToTheDeliveredTemplatesOnTheClasspath() {
        assertEquals(
                "classpath:templates/notification/email/",
                new NotificationProperties().templatesPath("email"));
    }

    @Test
    void checkTheTemplateDirectory_refusesAPathWhichDoesNotSayWhereItIs() {
        final var failure = assertThrows(
                IllegalStateException.class,
                () -> service("notification-templates"));

        final var message = failure.getMessage();
        assertTrue(message.contains("business-cockpit.notification.templates.test-medium"), message);
        assertTrue(message.contains("'classpath:notification-templates'"), message);
        assertTrue(message.contains("'file:notification-templates'"), message);
    }

    @Test
    void checkTheTemplateDirectory_refusesADirectoryItCannotRead(@TempDir final Path directory) {
        final var missing = directory.resolve("nowhere");

        final var failure = assertThrows(
                IllegalStateException.class,
                () -> service("file:" + missing));

        final var message = failure.getMessage();
        assertTrue(message.contains("business-cockpit.notification.templates.test-medium"), message);
        assertTrue(message.contains(missing.toString()), message);
        assertTrue(message.contains("classpath:"), message);
    }

    @Test
    void render_readsTemplatesFromADirectoryOfTheFileSystem(@TempDir final Path directory)
            throws Exception {
        Files.writeString(
                directory.resolve("probe.ftl"),
                "from the file system: ${greeting}");

        final var rendered = service("file:" + directory).render(
                "probe",
                Map.of("greeting", "hi"),
                userDetails(),
                NotificationType.CREATED,
                false);

        assertEquals("from the file system: hi", rendered);
    }

    @Test
    void render_readsAChangedTemplateAgainWithoutARestart(@TempDir final Path directory)
            throws Exception {
        final var template = directory.resolve("probe.ftl");
        Files.writeString(template, "first wording: ${greeting}");
        final var service = service("file:" + directory);
        assertEquals(
                "first wording: hi",
                service.render("probe", Map.of("greeting", "hi"), userDetails(),
                        NotificationType.CREATED, false));

        Files.writeString(template, "second wording: ${greeting}");

        // Freemarker looks at the file again once its update delay has passed, which is five
        // seconds by default and which the cockpit does not change. So the operator who edits a
        // mail waits seconds rather than a deployment, and this test waits with them.
        final var deadline = System.currentTimeMillis() + Duration.ofSeconds(30).toMillis();
        String rendered = null;
        while (System.currentTimeMillis() < deadline) {
            rendered = service.render("probe", Map.of("greeting", "hi"), userDetails(),
                    NotificationType.CREATED, false);
            if ("second wording: hi".equals(rendered)) {
                return;
            }
            Thread.sleep(250);
        }
        assertEquals("second wording: hi", rendered, "the changed template was never re-read");
    }

}

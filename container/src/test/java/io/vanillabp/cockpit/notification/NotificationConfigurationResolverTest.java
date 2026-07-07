package io.vanillabp.cockpit.notification;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vanillabp.cockpit.notification.model.NotificationConfiguration;
import io.vanillabp.cockpit.notification.model.WorkflowNotificationConfiguration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NotificationConfigurationResolverTest {

    private static final String MODULE = "moduleA";
    private static final String PROCESS = "processX";
    private static final String KEY = NotificationConfiguration.workflowKey(MODULE, PROCESS);
    private static final String EMAIL = "email";
    private static final String SMS = "sms";

    private static boolean resolve(final NotificationConfiguration config, final String medium) {
        return NotificationConfigurationResolver.shouldNotify(config, MODULE, PROCESS, medium);
    }

    @Test
    void nullConfiguration_isNone() {
        assertFalse(resolve(null, EMAIL));
    }

    @Test
    void emptyConfiguration_defaultsToNone() {
        final var config = new NotificationConfiguration(Map.of(), Map.of());
        assertFalse(resolve(config, EMAIL));
    }

    @Test
    void globalAllViaEmail_notifiesEmailOnly() {
        final var config = new NotificationConfiguration(Map.of(EMAIL, true), Map.of());
        assertTrue(resolve(config, EMAIL));
        assertFalse(resolve(config, SMS), "media are independent - sms was not enabled");
    }

    @Test
    void perWorkflowEnableException_overridesGlobalNone() {
        final var override = new WorkflowNotificationConfiguration(false, Map.of(EMAIL, true));
        final var config = new NotificationConfiguration(Map.of(), Map.of(KEY, override));
        assertTrue(resolve(config, EMAIL));
    }

    @Test
    void perWorkflowExcludeException_overridesGlobalAll() {
        final var override = new WorkflowNotificationConfiguration(false, Map.of(EMAIL, false));
        final var config = new NotificationConfiguration(Map.of(EMAIL, true), Map.of(KEY, override));
        assertFalse(resolve(config, EMAIL));
    }

    @Test
    void perWorkflowNone_clearsAllMedia_evenWithMediumEnabled() {
        final var override = new WorkflowNotificationConfiguration(true, Map.of(EMAIL, true));
        final var config = new NotificationConfiguration(Map.of(EMAIL, true), Map.of(KEY, override));
        assertFalse(resolve(config, EMAIL));
    }

    @Test
    void perWorkflowWithoutMediumOverride_fallsBackToGlobal() {
        final var override = new WorkflowNotificationConfiguration(false, Map.of());
        final var config = new NotificationConfiguration(Map.of(EMAIL, true), Map.of(KEY, override));
        assertTrue(resolve(config, EMAIL));
    }

    @Test
    void perWorkflowOverride_appliesOnlyToThatWorkflow() {
        final var override = new WorkflowNotificationConfiguration(false, Map.of(EMAIL, true));
        final var config = new NotificationConfiguration(Map.of(), Map.of(KEY, override));
        // a different workflow has no override and no global setting -> none
        assertFalse(NotificationConfigurationResolver.shouldNotify(config, "otherModule", "otherProcess", EMAIL));
    }

}

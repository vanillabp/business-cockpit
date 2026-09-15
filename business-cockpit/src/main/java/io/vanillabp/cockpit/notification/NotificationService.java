package io.vanillabp.cockpit.notification;

import io.vanillabp.cockpit.tasklist.model.UserTask;
import java.util.List;
import java.util.Map;

/**
 * Sends notifications for one particular medium (e.g. e-mail).
 * <p>
 * The Business Cockpit is built to carry derived cockpit applications. Which notification media
 * there are is the set of beans implementing this interface, and that set is read while the
 * application runs. So a derived application adds a medium by contributing another bean, and it
 * replaces the reference e-mail implementation by declaring a bean of its own. Every
 * implementation shipped here is {@code @ConditionalOnMissingBean}.
 * <p>
 * The signatures of the methods are part of the published contract and stay as they are. They
 * block, like the rest of the application. An implementation may read a database or talk to a mail
 * server directly, the way the reference e-mail implementation does.
 */
public interface NotificationService {

    /**
     * @return the technical type of this medium, e.g. {@code "email"}. Also used to resolve the
     *         template path ({@code business-cockpit.notification.<type>.templates}) and as the key
     *         for the per-medium recipient configuration.
     */
    String getType();

    /**
     * @return the display name of the medium per language (key = language, e.g. {@code "de"} ->
     *         {@code "E-Mail"}).
     */
    Map<String, String> getName();

    /**
     * @param userId the user the configuration belongs to
     * @return the medium-specific recipient configuration values of the user (e.g. the e-mail
     *         address), each carrying its language-specific title and description for rendering in
     *         the UI. Implementations should suggest a sensible default value if none was stored yet.
     */
    List<RecipientConfiguration> getRecipientConfiguration(String userId);

    /**
     * Persists the medium-specific recipient configuration of a user.
     *
     * @param userId              the user the configuration belongs to
     * @param configurationValues raw values keyed by {@link RecipientConfiguration#type()}
     *                            (e.g. {@code {"emailAddress": "a@b.c"}})
     */
    void saveRecipientConfiguration(String userId, Map<String, String> configurationValues);

    /**
     * Sends a single notification to many recipients at once (one "bulk").
     * <p>
     * The kind of change ({@link NotificationType}) and whether the workflow module forced the
     * notification are carried as transient fields on the given {@code userTask} (set by the
     * notification poller before this call).
     *
     * @param userIds  the recipients
     * @param userTask the user task the notification is about (with transient
     *                 {@code notificationType} / {@code forced} set)
     */
    void sendNotification(List<String> userIds, UserTask userTask);

}

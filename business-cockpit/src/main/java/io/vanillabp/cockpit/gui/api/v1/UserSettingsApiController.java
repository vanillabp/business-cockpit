package io.vanillabp.cockpit.gui.api.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.vanillabp.cockpit.support.security.BcUnauthorizedException;
import io.vanillabp.cockpit.support.security.usercontext.UserContext;
import io.vanillabp.cockpit.users.UserSettings;
import tools.jackson.databind.json.JsonMapper;

/**
 * The settings the user interface keeps for the user signed in. They live with the user and not
 * in the browser, so they follow the user to every browser.
 * <p>
 * Only the user signed in reaches their own settings. The path names no user, so nobody can ask
 * for the settings of somebody else.
 */
@RestController
@RequestMapping(path = "/gui/api/v1")
public class UserSettingsApiController implements UserSettingsApi {

  private final UserContext userContext;

  private final UserSettings userSettings;

  private final JsonMapper jsonMapper;

  /**
   * Creates the controller.
   *
   * @param userContext Says who is signed in
   * @param userSettings Reads and writes the settings
   * @param jsonMapper Turns a stored value back into JSON
   */
  public UserSettingsApiController(
      final UserContext userContext,
      final UserSettings userSettings,
      final JsonMapper jsonMapper) {

    this.userContext = userContext;
    this.userSettings = userSettings;
    this.jsonMapper = jsonMapper;

  }

  @Override
  public ResponseEntity<Object> getSettingOfCurrentUser(
      final String key) {

    return userSettings
        .get(userIdSignedIn(), key)
        // as a tree, because a value which is a text would otherwise be written as it is, by
        // the converter for strings, and not as a JSON string in quotes
        .<Object>map(jsonMapper::valueToTree)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());

  }

  @Override
  public ResponseEntity<Void> putSettingOfCurrentUser(
      final String key,
      final Object body) {

    userSettings.set(userIdSignedIn(), key, body);
    return ResponseEntity.noContent().build();

  }

  @Override
  public ResponseEntity<Void> deleteSettingOfCurrentUser(
      final String key) {

    userSettings.remove(userIdSignedIn(), key);
    return ResponseEntity.noContent().build();

  }

  private String userIdSignedIn() {

    final var user = userContext.getUserLoggedInDetails();
    // the request which signs in by basic authentication has no user yet, see currentUser
    if ((user == null) || (user.getId() == null)) {
      throw new BcUnauthorizedException("No user signed in for the settings");
    }
    return user.getId();

  }

}

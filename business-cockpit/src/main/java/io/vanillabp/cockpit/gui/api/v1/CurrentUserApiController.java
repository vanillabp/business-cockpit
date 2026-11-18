package io.vanillabp.cockpit.gui.api.v1;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.vanillabp.cockpit.support.security.usercontext.UserContext;
import io.vanillabp.cockpit.users.model.PersonAndGroupApiMapper;

/**
 * Answers who is signed in. This is part of the official GUI API, so a workflow module which
 * forwards the user's cookie asks it with the generated client of that API. It is a class of its
 * own because the cockpit's own GUI API and the official one are two generated interfaces, and
 * one class implementing both would have to choose between their default methods.
 */
@RestController
@RequestMapping(path = "/gui/api/v1")
public class CurrentUserApiController implements OfficialCurrentUserApi {

  /** Creates the controller. Spring fills the fields. */
  public CurrentUserApiController() {
  }

  @Autowired
  private UserContext userContext;

  @Autowired
  private PersonAndGroupApiMapper personAndGroupMapper;

  @Override
  public ResponseEntity<User> currentUser() {

    final var user = userContext.getUserLoggedInDetails();
    // the request which signs in by basic authentication is authenticated, but the response
    // to exactly that request is what carries the JWT cookie. The cockpit reads its user
    // details from that cookie, so the sign-in request has no user to report yet and the
    // client asks again with the cookie. The answer has no body, the way it had while the
    // application was reactive and the empty publisher ended up as an empty 200.
    if (user == null) {
      return ResponseEntity.ok().build();
    }

    return ResponseEntity.ok(Optional
        .ofNullable(personAndGroupMapper.toApiPerson(user.getId()))
        .map(person -> new User()
            .id(person.getId())
            .email(person.getEmail())
            .display(person.getDisplay())
            .displayShort(person.getDisplayShort())
            .details(person.getDetails()))
        .orElse(new User()
            .id(user.getId())
            .display(user.getDisplay())
            .displayShort(user.getDisplayShort())
            .email(user.getEmail()))
        .groups(user
            .getAuthorities()
            .stream()
            .map(personAndGroupMapper::authorityToApiGroup)
            .toList()));

  }

}

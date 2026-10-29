package io.vanillabp.cockpit.simulator.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.vanillabp.cockpit.commons.exceptions.BcUnauthorizedException;
import io.vanillabp.cockpit.commons.security.usercontext.UserContext;
import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;

/**
 * A small API of a fake workflow module called {@code TestModule}. Tests use it to check that the
 * user and the roles in the JWT reach the module.
 */
@RestController
@RequestMapping("/TestModule/api")
public class TestApi {

  @Autowired
  private UserContext userContext;

  /**
   * Creates the controller. Spring creates it, nobody else does.
   */
  public TestApi() {
  }

  /**
   * The details of the user logged in.
   *
   * @return The user details, or "not found" if no user is logged in
   */
  @GetMapping("/user-info")
  public ResponseEntity<UserDetails> getUserInfo() {

    try {
      return ResponseEntity.ok(userContext
          .getUserLoggedInDetails());
    } catch (BcUnauthorizedException e) {
      return ResponseEntity.notFound().build();
    }

  }

  /**
   * The same as {@link #getUserInfo()}, but it needs a role the test user does not have. So a
   * call by the test user has to be rejected.
   *
   * @return The user details
   */
  @Secured({
      "A_ROLE_NOT_OWNED_BY_TEST_USER"
  })
  @GetMapping("/protected-user-info")
  public ResponseEntity<UserDetails> getProtectedUserInfo() {

    return getUserInfo();

  }


  /**
   * The same as {@link #getUserInfo()}, but it needs the role {@code ROLE_TEST}, which the
   * test user has. So a call by the test user has to succeed.
   *
   * @return The user details
   */
  @Secured({
      "ROLE_TEST"
  }) // owned by test user
  @GetMapping("/test-user-info")
  public ResponseEntity<UserDetails> getTestUserInfo() {

    return getUserInfo();

  }

}

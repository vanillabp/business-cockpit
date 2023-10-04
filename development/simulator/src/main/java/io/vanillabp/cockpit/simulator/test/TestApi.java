package io.vanillabp.cockpit.simulator.test;

import io.vanillabp.cockpit.commons.exceptions.BcUnauthorizedException;
import io.vanillabp.cockpit.commons.security.usercontext.UserContext;
import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/TestModule/api")
public class TestApi {

    @Autowired
    private UserContext userContext;

    @GetMapping("/user-info")
    public ResponseEntity<UserDetails> getUserInfo() {

        try {
            return ResponseEntity.ok(userContext
                    .getUserLoggedInDetails());
        } catch (BcUnauthorizedException e) {
            return ResponseEntity.notFound().build();
        }

    }

    @Secured({ "A_ROLE_NOT_OWNED_BY_TEST_USER" })
    @GetMapping("/protected-user-info")
    public ResponseEntity<UserDetails> getProtectedUserInfo() {

        return getUserInfo();

    }


    @Secured({ "ROLE_TEST" }) // owned by test user
    @GetMapping("/test-user-info")
    public ResponseEntity<UserDetails> getTestUserInfo() {

        return getUserInfo();

    }

}

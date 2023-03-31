package io.vanillabp.cockpit.gui.api.v1;

import io.vanillabp.cockpit.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/gui/api/v1")
public class LoginApiController implements LoginApi {

    @Autowired
    private UserContext userContext;
    
    @Override
    public ResponseEntity<AppInformation> appInformation() {
        
        return ResponseEntity.ok(
                new AppInformation()
                        .titleLong("DKE BPMS Cockpit")
                        .titleShort("DKE")
                        .version("0.0.1-SNAPSHOT"));
        
    }
    
    @Override
    public ResponseEntity<User> currentUser(
            final String xRefreshToken) {
        
        final var user = userContext.getUserLoggedIn();
        
        return ResponseEntity.ok(
                new User()
                        .id(user)
                        .sex(Sex.OTHER)
                        .roles(List.of()));

        
    }
    
}

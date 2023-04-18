package io.vanillabp.cockpit.bpms.api.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/ui-components")
public class WebComponentsInformationApiController {

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/{urlType}",
            produces = { "application/json" }
        )
    public ResponseEntity<Void> provideUiComponentsInformation(
            final @PathVariable("urlType") UrlType urlType) {
        
        if (urlType != UrlType.WEBPACK_REACT) {
            return ResponseEntity.notFound().build();
        }
        
        
        
        return ResponseEntity.ok().build();
        
    }
    
}

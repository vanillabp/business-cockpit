package io.vanillabp.cockpit.config.web;

import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Makes deep links into the single-page application work. The browser asks the server for a path
 * like {@code /tasklist/4711} which no controller and no static resource answers. Instead of an
 * error the application shell is returned, and the router inside the browser takes it from there.
 * <p>
 * Ordered ahead of {@code RestfulExceptionHandler}, whose catch-all would otherwise turn every
 * unknown path into an HTTP 500.
 * <p>
 * The jar of this module carries no shell. The Maven build does not build a user interface, see
 * decision 62 in the repository's DECISIONS.md. An application without one of its own answers an
 * unknown path with 404, the same as a server which has no single-page application at all.
 */
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SpaNoHandlerFoundExceptionHandler {

    @Value("${application.spa-default-file:classpath:/static/index.html}")
    private String defaultFile;

    @Autowired
    private ResourceLoader resourceLoader;

    @ExceptionHandler({ NoResourceFoundException.class, NoHandlerFoundException.class })
    public ResponseEntity<Resource> handleNotFound() {

        final var shell = resourceLoader.getResource(defaultFile);
        if (!shell.exists()) {
            return ResponseEntity.notFound().build();
        }

        // the shell itself is the answer and not an error. It must not be cached, because it
        // carries the hashed names of the assets of the deployed build
        return ResponseEntity
                .ok()
                .contentType(MediaType.TEXT_HTML)
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS))
                .body(shell);

    }

}

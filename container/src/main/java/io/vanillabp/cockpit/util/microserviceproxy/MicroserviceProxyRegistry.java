package io.vanillabp.cockpit.util.microserviceproxy;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Serves every workflow module registered at the cockpit under {@code /wm/<module-id>/**} by
 * proxying the request to the module's own URI. The single-page application therefore loads module
 * assets and calls module APIs through the cockpit's origin.
 * <p>
 * Modules register at runtime, so the set of routes changes while the application is running. The
 * servlet gateway collects its {@code RouterFunction} beans once during startup and never asks the
 * application context again, so this registry is one stable bean that delegates to a router
 * function it swaps out on every registration. {@code RouterFunctionMapping} calls
 * {@link #route(ServerRequest)} for each request without caching the outcome, which is what makes
 * the swap take effect immediately.
 */
public class MicroserviceProxyRegistry implements RouterFunction<ServerResponse> {

    private static final Logger logger = LoggerFactory.getLogger(
            MicroserviceProxyRegistry.class);

    public static final String WORKFLOW_MODULES_PATH_PREFIX = "/wm/";

    /** Matches nothing. An empty gateway route builder would throw, so this is the initial state. */
    private static final RouterFunction<ServerResponse> NO_ROUTES = request -> Optional.empty();

    private final Map<String, String> routes = new HashMap<>();

    private final AtomicReference<RouterFunction<ServerResponse>> currentRoutes =
            new AtomicReference<>(NO_ROUTES);

    @Override
    public Optional<HandlerFunction<ServerResponse>> route(
            final ServerRequest request) {

        return currentRoutes.get().route(request);

    }

    @Override
    public void accept(
            final RouterFunctions.Visitor visitor) {

        currentRoutes.get().accept(visitor);

    }

    public synchronized void registerMicroservice(
            final String id,
            final String uri) {

        final var previousUri = routes.put(id, uri);
        if (uri.equals(previousUri)) {
            return;
        }

        rebuildRoutes();

    }

    public synchronized void registerMicroservices(
            final Map<String, String> microserviceUris) {

        final var numberOfPreviousKnownMicroservices = routes.size();

        microserviceUris.forEach(routes::putIfAbsent);

        if (routes.size() != numberOfPreviousKnownMicroservices) {
            rebuildRoutes();
        }

    }

    private void rebuildRoutes() {

        currentRoutes.set(routes
                .entrySet()
                .stream()
                .peek(entry -> logger.info(
                        "Register microservice proxy for workflow module: {}",
                        entry.getKey()))
                .map(entry -> buildRoute(entry.getKey(), entry.getValue()))
                .reduce(RouterFunction::and)
                .orElse(NO_ROUTES));

    }

    private RouterFunction<ServerResponse> buildRoute(
            final String id,
            final String uriAsString) {

        final var proxyPath = WORKFLOW_MODULES_PATH_PREFIX + id + "/";
        final var uri = URI.create(uriAsString);
        // the target URI contributes scheme, host and port only, so whatever path it carries has to
        // become part of the rewritten path
        final var targetPath = (uri.getPath() == null) || uri.getPath().isEmpty()
                ? "/"
                : uri.getPath().endsWith("/")
                ? uri.getPath()
                : uri.getPath() + "/";

        return GatewayRouterFunctions
                .route(id)
                .route(RequestPredicates.path(proxyPath + "**"), HandlerFunctions.http())
                .before(BeforeFilterFunctions.rewritePath(
                        "^" + Pattern.quote(proxyPath),
                        Matcher.quoteReplacement(targetPath)))
                .before(BeforeFilterFunctions.uri(uri))
                .build();

    }

}

package io.vanillabp.cockpit.util.microserviceproxy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MicroserviceProxyRegistry}.
 */
@ExtendWith(MockitoExtension.class)
class MicroserviceProxyRegistryTest {

    @Mock
    private RouteLocatorBuilder routeLocatorBuilder;

    @Mock
    private RouteLocatorBuilder.Builder builder;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private MicroserviceProxyRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new MicroserviceProxyRegistry(routeLocatorBuilder, applicationEventPublisher);
    }

    // --- Path prefix constant test ---

    @Test
    void workflowModulesPathPrefix_hasCorrectValue() {
        // Assert
        assertThat(MicroserviceProxyRegistry.WORKFLOW_MODULES_PATH_PREFIX).isEqualTo("/wm/");
    }

    // --- registerMicroservice tests ---

    @Test
    void registerMicroservice_withNewId_registersAndPublishesEvent() {
        // Act
        registry.registerMicroservice("module-1", "http://localhost:8081");

        // Assert
        verify(applicationEventPublisher).publishEvent(any(RefreshRoutesEvent.class));
    }

    @Test
    void registerMicroservice_withExistingId_doesNotPublishEvent() {
        // Arrange - register the first time
        registry.registerMicroservice("module-1", "http://localhost:8081");
        reset(applicationEventPublisher);

        // Act - try to register the same id again
        registry.registerMicroservice("module-1", "http://localhost:8082");

        // Assert - should not publish event for duplicate
        verify(applicationEventPublisher, never()).publishEvent(any(RefreshRoutesEvent.class));
    }

    @Test
    void registerMicroservice_withMultipleDifferentIds_publishesEventForEach() {
        // Act
        registry.registerMicroservice("module-1", "http://localhost:8081");
        registry.registerMicroservice("module-2", "http://localhost:8082");

        // Assert
        verify(applicationEventPublisher, times(2)).publishEvent(any(RefreshRoutesEvent.class));
    }

    // --- registerMicroservices tests ---

    @Test
    void registerMicroservices_withNewIds_registersAllAndPublishesEvent() {
        // Arrange
        Map<String, String> microservices = new HashMap<>();
        microservices.put("module-1", "http://localhost:8081");
        microservices.put("module-2", "http://localhost:8082");

        // Act
        registry.registerMicroservices(microservices);

        // Assert
        verify(applicationEventPublisher).publishEvent(any(RefreshRoutesEvent.class));
    }

    @Test
    void registerMicroservices_withEmptyMap_doesNotPublishEvent() {
        // Act
        registry.registerMicroservices(new HashMap<>());

        // Assert
        verify(applicationEventPublisher, never()).publishEvent(any(RefreshRoutesEvent.class));
    }

    @Test
    void registerMicroservices_withAllExistingIds_doesNotPublishEvent() {
        // Arrange - register first
        registry.registerMicroservice("module-1", "http://localhost:8081");
        reset(applicationEventPublisher);

        Map<String, String> microservices = new HashMap<>();
        microservices.put("module-1", "http://localhost:8082");

        // Act
        registry.registerMicroservices(microservices);

        // Assert
        verify(applicationEventPublisher, never()).publishEvent(any(RefreshRoutesEvent.class));
    }

    @Test
    void registerMicroservices_withMixOfNewAndExistingIds_publishesEvent() {
        // Arrange - register one first
        registry.registerMicroservice("module-1", "http://localhost:8081");
        reset(applicationEventPublisher);

        Map<String, String> microservices = new HashMap<>();
        microservices.put("module-1", "http://localhost:8082"); // existing
        microservices.put("module-2", "http://localhost:8083"); // new

        // Act
        registry.registerMicroservices(microservices);

        // Assert - should publish because of the new one
        verify(applicationEventPublisher).publishEvent(any(RefreshRoutesEvent.class));
    }

    // --- getRoutes tests ---

    @Test
    void getRoutes_withNoRegistrations_returnsEmptyFlux() {
        // Arrange
        when(routeLocatorBuilder.routes()).thenReturn(builder);
        when(builder.build()).thenReturn(() -> Flux.empty());

        // Act
        Flux<Route> routes = registry.getRoutes();

        // Assert
        assertThat(routes.collectList().block()).isEmpty();
    }
}

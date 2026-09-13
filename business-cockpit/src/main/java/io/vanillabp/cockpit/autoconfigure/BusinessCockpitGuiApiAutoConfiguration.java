package io.vanillabp.cockpit.autoconfigure;

import io.vanillabp.cockpit.commons.exceptions.RestfulExceptionHandler;
import io.vanillabp.cockpit.commons.security.usercontext.UserContextConfiguration;
import io.vanillabp.cockpit.config.MicroserviceProxyConfiguration;
import io.vanillabp.cockpit.config.web.JsonConfiguration;
import io.vanillabp.cockpit.config.web.SpaNoHandlerFoundExceptionHandler;
import io.vanillabp.cockpit.gui.api.v1.LoginApiController;
import io.vanillabp.cockpit.notification.api.v1.NotificationConfigGuiApiController;
import io.vanillabp.cockpit.tasklist.api.v1.TasklistGuiApiMapperImpl;
import io.vanillabp.cockpit.workflowlist.api.v1.WorkflowListGuiApiMapperImpl;
import io.vanillabp.cockpit.workflowmodules.api.v1.WorkflowModulesGuiApiMapperImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Import;

/**
 * What the user interface talks to: the endpoints of the GUI API which are the same for every
 * cockpit, the JSON format they answer in, the fallback which answers a deep link with the
 * application shell, and the proxy which forwards a request for a workflow module's own screens.
 * <p>
 * The three list endpoints are not here. Which tasks, workflows and modules a user may see is the
 * one question the library does not answer, so an application subclasses the abstract controllers
 * and registers those itself. The mappers below are what those subclasses need.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
@Import({
    JsonConfiguration.class,
    LoginApiController.class,
    MicroserviceProxyConfiguration.class,
    NotificationConfigGuiApiController.class,
    RestfulExceptionHandler.class,
    SpaNoHandlerFoundExceptionHandler.class,
    TasklistGuiApiMapperImpl.class,
    UserContextConfiguration.class,
    WorkflowListGuiApiMapperImpl.class,
    WorkflowModulesGuiApiMapperImpl.class
})
public class BusinessCockpitGuiApiAutoConfiguration {

}

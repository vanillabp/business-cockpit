package io.vanillabp.cockpit.autoconfigure;

import io.vanillabp.cockpit.bpms.BpmsApiProperties;
import io.vanillabp.cockpit.commons.utils.LoggerFactory;
import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import io.vanillabp.cockpit.config.startup.StartupConfigurationCheck;
import io.vanillabp.cockpit.notification.NotificationProperties;
import io.vanillabp.cockpit.tasklist.FollowUpScheduler;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.util.kwic.KwicService;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowmodules.GroupHierarchyService;
import io.vanillabp.cockpit.workflowmodules.WorkflowModuleService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * What every cockpit application has, whatever else it switches on: the three property classes, the
 * check which reports on each start what is still unconfigured, and the services behind the two
 * lists and the workflow modules.
 * <p>
 * The startup check is a {@code BeanFactoryPostProcessor}, so it runs before the first bean of the
 * application is built. That is the reason it is imported here rather than built by a {@code @Bean}
 * method: an imported class is a plain bean definition, and nothing of this configuration has to
 * exist for it to run.
 */
@AutoConfiguration
@EnableConfigurationProperties({
    ApplicationProperties.class,
    BpmsApiProperties.class,
    NotificationProperties.class
})
@Import({
    LoggerFactory.class,
    StartupConfigurationCheck.class,
    KwicService.class,
    GroupHierarchyService.class,
    WorkflowModuleService.class,
    UserTaskService.class,
    WorkflowlistService.class,
    FollowUpScheduler.class,
    io.vanillabp.cockpit.tasklist.api.GuiNotificationService.class,
    io.vanillabp.cockpit.workflowlist.api.GuiNotificationService.class
})
public class BusinessCockpitAutoConfiguration {

}

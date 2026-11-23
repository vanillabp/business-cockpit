package io.vanillabp.cockpit.autoconfigure;

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * What workflow modules and BPMS adapters report to: the two versions of the BPMS API and the
 * mappers turning a reported event into a stored document.
 * <p>
 * The endpoints are always registered, and they answer only a caller with the authority
 * {@code ROLE_BPMS-API}. How a caller logs in is decided by a security chain of the application.
 * The library brings none, and the startup check says on every start when it is missing.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
@Import({
    io.vanillabp.cockpit.bpms.api.v1.BpmsApiController.class, io.vanillabp.cockpit.bpms.api.v1.UserTaskMapperV1Impl.class, io.vanillabp.cockpit.bpms.api.v1.WorkflowMapperV1Impl.class, io.vanillabp.cockpit.bpms.api.v1_1.BpmsApiController.class, io.vanillabp.cockpit.bpms.api.v1_1.UserTaskMapperV1_1Impl.class, io.vanillabp.cockpit.bpms.api.v1_1.WorkflowMapperV1_1Impl.class
})
public class BusinessCockpitBpmsApiAutoConfiguration {

  /**
   * Called by Spring Boot. Everything this configuration adds comes from its annotations.
   */
  public BusinessCockpitBpmsApiAutoConfiguration() {

  }

  /**
   * Ends the start if {@code @Secured} does not count for the controllers, whoever switched method
   * security on.
   *
   * @return the check, run once the application is ready
   */
  @Bean
  SecuredCountsForTheBpmsApi securedCountsForTheBpmsApi() {

    return new SecuredCountsForTheBpmsApi(List.of(
        io.vanillabp.cockpit.bpms.api.v1.BpmsApiController.class,
        io.vanillabp.cockpit.bpms.api.v1_1.BpmsApiController.class));

  }

}

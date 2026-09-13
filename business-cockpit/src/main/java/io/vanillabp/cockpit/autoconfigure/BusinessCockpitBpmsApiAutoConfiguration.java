package io.vanillabp.cockpit.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Import;

/**
 * What workflow modules and BPMS adapters report to: the two versions of the BPMS API and the
 * mappers turning a reported event into a stored document.
 * <p>
 * The endpoints are always registered. Whether they answer is decided by the security chain of the
 * BPMS API, which is absent while no client is configured, and the startup check says on every start
 * what to configure to have one.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
@Import({
    io.vanillabp.cockpit.bpms.api.v1.BpmsApiController.class,
    io.vanillabp.cockpit.bpms.api.v1.UserTaskMapperV1Impl.class,
    io.vanillabp.cockpit.bpms.api.v1.WorkflowMapperV1Impl.class,
    io.vanillabp.cockpit.bpms.api.v1_1.BpmsApiController.class,
    io.vanillabp.cockpit.bpms.api.v1_1.UserTaskMapperV1_1Impl.class,
    io.vanillabp.cockpit.bpms.api.v1_1.WorkflowMapperV1_1Impl.class
})
public class BusinessCockpitBpmsApiAutoConfiguration {

}

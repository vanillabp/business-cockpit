package io.vanillabp.cockpit.workflowmodules.api.v1;

import io.vanillabp.cockpit.gui.api.v1.WorkflowModule;
import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Turns a stored workflow module into the one the GUI API returns. MapStruct writes the
 * implementation.
 */
@Mapper(implementationName = "WorkflowModulesGuiApiMapperImpl")
public abstract class GuiApiMapper {

    /** Creates the mapper. The generated implementation calls it. */
    public GuiApiMapper() {
    }

    /**
     * Maps a stored workflow module to the GUI API. The URI is replaced by the cockpit's own proxy
     * path, see {@link #uriMapper}.
     *
     * @param model The stored workflow module
     * @return The workflow module of the GUI API
     */
    @Mapping(target = "uri", expression = "java(uriMapper(model))")
    public abstract WorkflowModule toApi(io.vanillabp.cockpit.workflowmodules.model.WorkflowModule model);

    /**
     * The URI the GUI uses to reach a workflow module. The browser does not call the module
     * directly. It calls the cockpit, which passes the request on to the module.
     *
     * @param module The stored workflow module
     * @return The cockpit's proxy path for the module, or {@code null} when the module registered
     *     no URI
     */
    public String uriMapper(
            final io.vanillabp.cockpit.workflowmodules.model.WorkflowModule module) {

        if (module == null) {
            return null;
        }
        if (module.getUri() == null) {
            return null;
        }
        return MicroserviceProxyRegistry.WORKFLOW_MODULES_PATH_PREFIX + module.getId();

    }

}

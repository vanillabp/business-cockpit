package io.vanillabp.cockpit.extension.springboot.writinghandler;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

import io.vanillabp.integration.extension.spi.handler.CoreHandlerParameter;
import io.vanillabp.integration.extension.spi.handler.ExtensionHandlers;
import io.vanillabp.integration.extension.spi.handler.HandlerContract;

/**
 * A second extension next to the Business Cockpit, added to a boot which wants a handler that
 * may write.
 * <p>
 * Its contract says nothing about saving, which is the normal case. VanillaBP saves the workflow
 * aggregate after such a method ran. So a boot warns about it, and that is what the test reads,
 * next to the silence about the cockpit's own providers.
 */
@Configuration
public class ExtensionWithAWritingHandler implements InitializingBean {

  /** The id the warning names for this extension. */
  public static final String EXTENSION_ID = "writing-extension";

  private final ExtensionHandlers handlers;

  public ExtensionWithAWritingHandler(
      final ExtensionHandlers handlers) {

    this.handlers = handlers;

  }

  @Override
  public void afterPropertiesSet() {

    handlers
        .register(
            HandlerContract
                .of(EXTENSION_ID, NoteOnTheCase.class)
                .coreParameters(CoreHandlerParameter.WORKFLOW_AGGREGATE)
                .build());

  }

}

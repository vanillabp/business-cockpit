package io.vanillabp.cockpit.extension.springboot.writinghandler;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

import io.vanillabp.integration.extension.spi.handler.CoreHandlerParameter;
import io.vanillabp.integration.extension.spi.handler.ExtensionHandlers;
import io.vanillabp.integration.extension.spi.handler.HandlerContract;

/**
 * A second extension next to the Business Cockpit. A boot adds it when it wants a handler which
 * may write.
 * <p>
 * Its contract says nothing about saving, which is the normal case, so VanillaBP saves the
 * workflow aggregate after such a method ran. So the boot warns about it. The test reads this
 * warning, and checks that the cockpit's own providers cause none.
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

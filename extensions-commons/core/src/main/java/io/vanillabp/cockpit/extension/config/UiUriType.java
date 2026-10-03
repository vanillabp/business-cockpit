package io.vanillabp.cockpit.extension.config;

/**
 * The values of <code>ui-uri-type</code> the shipped user interface of the Business Cockpit
 * knows.
 * <p>
 * The value says which kind of user interface a workflow module brings. The cockpit server
 * carries it and never reads it, so this is a list of known names and not a closed set of
 * allowed ones. A workflow module whose user interface is somebody else's writes whatever that
 * one agreed on, and only that user interface has to know the name.
 * <p>
 * The configuration still has to name a value. The start of the application checks that one is
 * there and not which one it is, and it offers these names as a hint where nothing was written.
 */
public final class UiUriType {

  /** The cockpit opens the module's own page instead of loading a form. */
  public static final String EXTERNAL = "EXTERNAL";

  /** The cockpit loads a React micro frontend through Webpack module federation. */
  public static final String WEBPACK_MF_REACT = "WEBPACK_MF_REACT";

  private UiUriType() {
  }

  /**
   * @return The known names, for a message which offers them as a hint
   */
  public static String[] names() {

    return new String[]{
        EXTERNAL, WEBPACK_MF_REACT
    };

  }

}

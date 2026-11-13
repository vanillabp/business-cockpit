package io.vanillabp.cockpit.extension.config;

/**
 * Where the templates of one workflow module are.
 * <p>
 * The directory is written in two parts, because the application's directory is shared by every
 * module. Freemarker opens the directory which was configured, and the module's templates lie
 * below it. A directory of the file system has to exist when the application starts. The
 * directory of one module below it does not, because a module may have no templates at all.
 *
 * @param loaderPath The directory Freemarker opens, with the place in front of it:
 *          <code>classpath:</code>, <code>classpath*:</code> or <code>file:</code>
 * @param below The path of the module's templates below {@link #loaderPath}, or an empty text
 *          where they are right in it
 * @param key The property key which says so, for a message about a directory which cannot be
 *          read. It is <code>null</code> for the default directory, which nobody configured
 */
public record TemplateDirectory(
                                String loaderPath,
                                String below,
                                String key) {

  /**
   * Makes a missing path below the directory an empty one.
   */
  public TemplateDirectory {
    below = below == null ? "" : below.replaceAll("^/+", "").replaceAll("/+$", "");
  }

}

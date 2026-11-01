package io.vanillabp.cockpit.bpms;

/**
 * The role a caller of the BPMS API needs. The BPMS API controllers ask for it, so it belongs to
 * the library. How a caller logs in and gets the role is up to the application. The module
 * {@code container} gives it to the one user it builds from {@code bpms-api.username}.
 */
public final class BpmsApiAuthority {

  /**
   * The role of a caller of the BPMS API, as it is given to a user.
   */
  public static final String BPMS_API_ROLE = "BPMS-API";

  /**
   * The authority Spring Security makes of {@link #BPMS_API_ROLE}. The BPMS API controllers ask
   * for it.
   */
  public static final String BPMS_API_AUTHORITY = "ROLE_"
      + BPMS_API_ROLE;

  private BpmsApiAuthority() {
  }

}

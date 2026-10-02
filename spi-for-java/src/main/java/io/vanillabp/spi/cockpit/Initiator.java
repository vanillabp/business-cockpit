package io.vanillabp.spi.cockpit;

/**
 * The answer for an action no user caused.
 *
 * <p>Every report the Business Cockpit receives names an initiator, the user who caused what is
 * being reported. No workflow system records that, so a workflow module sets it in a method
 * annotated with <code>&#64;WorkflowDetailsProvider</code> or
 * <code>&#64;UserTaskDetailsProvider</code>, out of what it wrote down itself.
 *
 * <p>Where no user was involved, a timer or a nightly import for instance, there is nobody to
 * name, and this constant is what a provider sets instead. It is also the way to drop a value an
 * adapter prefilled: setting the field to <code>null</code> again looks exactly like a provider
 * which did nothing, so the cockpit cannot tell the two apart.
 *
 * <p>The cockpit server writes the same text as
 * <code>UpdateInformationAware.SYSTEM_USER</code>, in the artifact
 * <code>io.vanillabp.businesscockpit:commons</code>. The two modules do not see each other, so
 * they hold two constants of the same value, and neither of them may be changed alone.
 */
public final class Initiator {

    /**
     * The initiator of an action no user caused.
     */
    public static final String SYSTEM = "system";

    private Initiator() {
    }

}

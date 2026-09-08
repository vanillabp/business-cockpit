package io.vanillabp.cockpit.extension.springboot;

import java.util.function.Supplier;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import io.vanillabp.integration.spi.TransactionRunner;

/**
 * The extension's transactions on Spring Boot.
 * <p>
 * Two things need one. An event a remote engine reported arrives on a worker thread with no
 * transaction around it, and the outbox entry has to be written in one. And the registration of
 * a workflow module happens while the application starts, where there is none either.
 * <p>
 * The runner is not the platform's own, although it does the same thing, because an extension
 * compiles against the two SPI artifacts and the neutral core and not against a platform
 * integration - a rule which exists so that an extension cannot quietly grow a dependency on
 * one platform. What that costs is this class.
 */
public class SpringTransactionRunner implements TransactionRunner {

  private final PlatformTransactionManager transactionManager;

  /**
   * @param transactionManager The manager covering the workflow aggregates of the application
   */
  public SpringTransactionRunner(
      final PlatformTransactionManager transactionManager) {

    this.transactionManager = transactionManager;

  }

  @Override
  public <T> T requireNew(
      final Supplier<T> work) {

    return run(work, TransactionDefinition.PROPAGATION_REQUIRES_NEW);

  }

  @Override
  public <T> T inCurrent(
      final Supplier<T> work) {

    if (!isTransactionActive()) {
      throw new IllegalStateException(
          """
              The Business Cockpit extension was asked to write its outbox entry into the caller's \
              transaction but no transaction is running on this thread. A BPMS half asks for that \
              only where its engine invokes it inside the engine's own transaction; a remote \
              engine has to ask for a new one instead.""");
    }
    return run(work, TransactionDefinition.PROPAGATION_MANDATORY);

  }

  @Override
  public boolean isTransactionActive() {

    return TransactionSynchronizationManager.isActualTransactionActive();

  }

  @Override
  public boolean isRollbackOnly() {

    // the extension never marks a transaction rollback-only itself, and Spring reports the
    // mark only from inside the template's callback - so the honest answer here is "no mark
    // of mine"
    return false;

  }

  private <T> T run(
      final Supplier<T> work,
      final int propagation) {

    final var template = new TransactionTemplate(transactionManager);
    template.setPropagationBehavior(propagation);
    return template.execute(status -> work.get());

  }

}

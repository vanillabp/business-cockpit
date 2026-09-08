package io.vanillabp.cockpit.extension.quarkus;

import java.util.function.Supplier;

import io.quarkus.arc.Arc;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.vanillabp.integration.spi.TransactionRunner;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionSynchronizationRegistry;

/**
 * The extension's transactions on Quarkus, for the same two situations the Spring Boot half
 * needs them: an event a remote engine reported on a worker thread, and the registration of a
 * workflow module while the application starts.
 * <p>
 * The CDI request context is activated around the work as well. The extension writes through
 * the application's outbox store, and a store built on Hibernate needs one; a worker thread of
 * a BPMS brings none.
 */
public class QuarkusTransactionRunner implements TransactionRunner {

  private final TransactionSynchronizationRegistry transactionRegistry;

  /**
   * @param transactionRegistry What tells whether a transaction is running on this thread
   */
  public QuarkusTransactionRunner(
      final TransactionSynchronizationRegistry transactionRegistry) {

    this.transactionRegistry = transactionRegistry;

  }

  @Override
  public <T> T requireNew(
      final Supplier<T> work) {

    return withRequestContext(() -> QuarkusTransaction.requiringNew().call(work::get));

  }

  @Override
  public <T> T inCurrent(
      final Supplier<T> work) {

    if (transactionRegistry.getTransactionStatus() == Status.STATUS_NO_TRANSACTION) {
      throw new IllegalStateException(
          """
              The Business Cockpit extension was asked to write its outbox entry into the caller's \
              transaction but no transaction is running on this thread. A BPMS half asks for that \
              only where its engine invokes it inside the engine's own transaction; a remote \
              engine has to ask for a new one instead.""");
    }
    return withRequestContext(work);

  }

  @Override
  public boolean isTransactionActive() {

    return transactionRegistry.getTransactionStatus() != Status.STATUS_NO_TRANSACTION;

  }

  @Override
  public boolean isRollbackOnly() {

    return (transactionRegistry.getTransactionStatus() != Status.STATUS_NO_TRANSACTION) && transactionRegistry
        .getRollbackOnly();

  }

  private <T> T withRequestContext(
      final Supplier<T> work) {

    final var requestContext = Arc.container().requestContext();
    if (requestContext.isActive()) {
      return work.get();
    }
    requestContext.activate();
    try {
      return work.get();
    } finally {
      requestContext.terminate();
    }

  }

}

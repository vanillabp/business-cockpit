package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import java.util.function.Supplier;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs one change of a stored workflow or user task in a transaction of its own, and runs it again
 * where another change of the same row came first.
 *
 * <p>The simulator stores a report in three steps: it reads the row, lays the report onto it and
 * writes the row back. A workflow module sends its reports through an outbox, and two reports about
 * the same workflow can arrive at the same moment. Without a check, both read the same row, and the
 * one which writes last undoes the other one. The entities carry a version, so the second write
 * fails instead, and this class repeats that change on the row as it is now. Two reports which both
 * find no row meet the same way: the second insert fails on the key, and the repetition finds the
 * row the first one stored.
 *
 * <p>A change which still meets another one after {@value #MOST_ATTEMPTS} attempts fails, and the
 * report is answered with an error. The outbox of the workflow module sends it again later.
 */
@Component
public class ConflictingChanges {

  /** How often a change is tried before the report is refused. */
  static final int MOST_ATTEMPTS = 10;

  private final TransactionTemplate transactionOfItsOwn;

  /**
   * Creates the helper. Spring creates it, nobody else does.
   *
   * @param transactionManager The transaction manager of the simulator's database
   */
  public ConflictingChanges(
      final PlatformTransactionManager transactionManager) {

    transactionOfItsOwn = new TransactionTemplate(transactionManager);
    // every attempt needs a fresh transaction. A transaction of the caller would still hold the
    // row of the failed attempt, and the next attempt would read it again
    transactionOfItsOwn.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

  }

  /**
   * Runs a change, and runs it again as long as it meets another change of the same row.
   *
   * @param <T> What the change returns
   * @param change Reads the row, changes it and writes it. It is run once per attempt, so it must
   *        read the row itself and not use one read before
   * @return What the last attempt returned
   */
  public <T> T runAndRepeatOnConflict(
      final Supplier<T> change) {

    for (int attempt = 1;; ++attempt) {
      try {
        return transactionOfItsOwn.execute(status -> change.get());
      } catch (ConcurrencyFailureException | DataIntegrityViolationException e) {
        if (attempt >= MOST_ATTEMPTS) {
          throw e;
        }
      }
    }

  }

}

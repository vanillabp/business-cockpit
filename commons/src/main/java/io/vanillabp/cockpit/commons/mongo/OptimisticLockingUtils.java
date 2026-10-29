package io.vanillabp.cockpit.commons.mongo;

import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.dao.OptimisticLockingFailureException;

/**
 * Runs a database action again if it fails because somebody else changed the same document at the
 * same time. Only an {@link OptimisticLockingFailureException} leads to a new attempt.
 */
public class OptimisticLockingUtils {

  /**
   * How often an action is run again if no other number is given. With 2 retries an action runs at
   * most three times.
   */
  public static final int DEFAULT_NUMBER_OF_RETRIES = 2;

  /**
   * There is no instance, the class only has static methods.
   */
  private OptimisticLockingUtils() {
  }

  /**
   * Runs an action and runs it again up to {@link #DEFAULT_NUMBER_OF_RETRIES} times on an optimistic
   * locking failure.
   *
   * @param <R>      The type of the result
   * @param dbAction The action, which has to load the document again on each run
   * @return The result of the first run which did not fail
   * @throws OptimisticLockingFailureException If the last run fails as well
   */
  public static <R> R doWithRetries(
      final Supplier<R> dbAction) {

    return doWithRetries(DEFAULT_NUMBER_OF_RETRIES, dbAction);

  }

  /**
   * Runs an action and runs it again up to the given number of times on an optimistic locking
   * failure.
   *
   * @param <R>      The type of the result
   * @param retries  How often the action is run again at most
   * @param dbAction The action, which has to load the document again on each run
   * @return The result of the first run which did not fail
   * @throws OptimisticLockingFailureException If the last run fails as well
   */
  public static <R> R doWithRetries(
      final int retries,
      final Supplier<R> dbAction) {

    for (int i = 0; i <= retries; ++i) {
      try {
        return dbAction.get();
      } catch (OptimisticLockingFailureException e) {
        if (i == retries) {
          throw e;
        }
      }
    }

    throw new RuntimeException("Unexpected situation in optimistic locking retry behavior");

  }

  /**
   * Runs an action on a bean and runs it again up to {@link #DEFAULT_NUMBER_OF_RETRIES} times on an
   * optimistic locking failure.
   *
   * @param <T>      The type of the bean
   * @param <R>      The type of the result
   * @param bean     The bean the action gets on each run
   * @param dbAction The action, which has to load the document again on each run
   * @return The result of the first run which did not fail
   * @throws OptimisticLockingFailureException If the last run fails as well
   */
  public static <T, R> R doWithRetries(
      final T bean,
      final Function<T, R> dbAction) {

    return doWithRetries(DEFAULT_NUMBER_OF_RETRIES, bean, dbAction);

  }

  /**
   * Runs an action on a bean and runs it again up to the given number of times on an optimistic
   * locking failure.
   *
   * @param <T>      The type of the bean
   * @param <R>      The type of the result
   * @param retries  How often the action is run again at most
   * @param bean     The bean the action gets on each run
   * @param dbAction The action, which has to load the document again on each run
   * @return The result of the first run which did not fail
   * @throws OptimisticLockingFailureException If the last run fails as well
   */
  public static <T, R> R doWithRetries(
      final int retries,
      final T bean,
      final Function<T, R> dbAction) {

    for (int i = 0; i <= retries; ++i) {
      try {
        return dbAction.apply(bean);
      } catch (OptimisticLockingFailureException e) {
        if (i == retries) {
          throw e;
        }
      }
    }

    throw new RuntimeException("Unexpected situation in optimistic locking retry behavior");

  }

}

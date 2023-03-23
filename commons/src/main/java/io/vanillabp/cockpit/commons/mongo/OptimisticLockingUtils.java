package io.vanillabp.cockpit.commons.mongo;

import org.springframework.dao.OptimisticLockingFailureException;

import java.util.function.Function;
import java.util.function.Supplier;

public class OptimisticLockingUtils {

    public static final int DEFAULT_NUMBER_OF_RETRIES = 2;

    public static <R> R doWithRetries(
            final Supplier<R> dbAction) {
        
        return doWithRetries(DEFAULT_NUMBER_OF_RETRIES, dbAction);
        
    }

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
    
    public static <T, R> R doWithRetries(
            final T bean,
            final Function<T, R> dbAction) {
        
        return doWithRetries(DEFAULT_NUMBER_OF_RETRIES, bean, dbAction);
        
    }

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

package ai.grakn.engine.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace;

/**
 * Utility class to execute a Runnable and log any exceptions thrown without propagating them further.
 * @author Denis Igorevich Lobanov
 */
public class ExceptionWrapper {
    private static Logger LOG = LoggerFactory.getLogger(ExceptionWrapper.class);

    public static void noThrow(Runnable fn, String errorMessage) {
        try {
            fn.run();
        }
        catch (Throwable t) {
            LOG.error(errorMessage + "\nThe exception was: " + getFullStackTrace(t));
        }
    }
}

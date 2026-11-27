package {{rootPackage}}.exception;

import {{rootPackage}}.util.Check;

/**
 * A downstream could not be reached or answered 503 after the retries, mapped to 503. For a proxy
 * that is a normal outcome, never a 500.
 */
public final class ServiceUnavailableException extends RuntimeException {

    /**
     * @param message which service is unavailable
     */
    public ServiceUnavailableException(final String message) {
        super(message);
        Check.isNotEmpty(message, "message");
    }
}

package {{rootPackage}}.exception;

import {{rootPackage}}.util.Check;

/**
 * A downstream refused the change because of the current state, mapped to 409.
 */
public final class ConflictException extends RuntimeException {

    /**
     * @param message what conflicts
     */
    public ConflictException(final String message) {
        super(message);
        Check.isNotEmpty(message, "message");
    }
}

package {{rootPackage}}.exception;

import {{rootPackage}}.util.Check;

import java.util.List;

/**
 * A downstream rejected the request as malformed, mapped to 400.
 */
public final class BadRequestException extends RuntimeException {

    private final List<String> messages;

    /**
     * @param messages what is wrong with the request
     */
    public BadRequestException(final List<String> messages) {
        super(String.join("; ", messages));
        Check.isNotEmpty(messages, "messages");
        this.messages = List.copyOf(messages);
    }

    /**
     * @return what is wrong with the request
     */
    public List<String> getMessages() {
        return messages;
    }
}

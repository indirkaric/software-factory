package {{rootPackage}}.util;

import {{rootPackage}}.client.response.ClientResponseWrapper;
import {{rootPackage}}.exception.BadRequestException;
import {{rootPackage}}.exception.BusinessException;
import {{rootPackage}}.exception.ConflictException;
import {{rootPackage}}.exception.EntityNotFoundException;
import {{rootPackage}}.exception.ServiceUnavailableException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a {@link ClientResponseWrapper} into the result or the exception that answers the caller with
 * the downstream's status: 400, 403, 404, 409 and 422 keep their status and messages, an unreachable
 * downstream or a 503 becomes {@link ServiceUnavailableException}, anything else a 500.
 */
public final class ResponseUtil {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .findAndAddModules()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private ResponseUtil() {
    }

    /**
     * @param response what the downstream answered, {@code null} when it could not be reached
     * @param type type of the body, {@code null} when the caller needs no body
     * @param serviceName downstream name for messages, e.g. "Invoice"
     * @param <T> body type
     * @return the body as {@code type}, or {@code null} for a 2xx without a body (204)
     */
    public static <T> T handleResponse(final ClientResponseWrapper response, final Class<T> type,
                                       final String serviceName) {
        Check.isNotEmpty(serviceName, "serviceName");

        if (response == null || response.statusCode().value() == HttpStatus.SERVICE_UNAVAILABLE.value()) {
            throw new ServiceUnavailableException(serviceName + " service is unavailable. Please try again later.");
        }

        if (response.statusCode().is2xxSuccessful()) {
            return type == null || response.body() == null ? null : MAPPER.convertValue(response.body(), type);
        }

        int status = response.statusCode().value();
        List<String> messages = messages(response.body(), serviceName + " answered " + status + ".");
        HttpStatus known = HttpStatus.resolve(status);
        if (known == null) {
            throw new IllegalStateException(messages.get(0));
        }

        throw switch (known) {
            case BAD_REQUEST -> new BadRequestException(messages);
            case UNAUTHORIZED, FORBIDDEN -> new AccessDeniedException(messages.get(0));
            case NOT_FOUND -> new EntityNotFoundException(messages.get(0));
            case CONFLICT -> new ConflictException(messages.get(0));
            case UNPROCESSABLE_ENTITY -> new BusinessException(messages);
            default -> new IllegalStateException(serviceName + " answered " + status + ": " + messages);
        };
    }

    /**
     * @param body error body: {@code {"messages": [...]}}, {@code {"message": "..."}} or plain text
     * @param fallback message when the body has none
     * @return the messages, never empty
     */
    private static List<String> messages(final Object body, final String fallback) {
        List<String> messages = new ArrayList<>();
        if (body instanceof String text && !text.isBlank()) {
            try {
                JsonNode root = MAPPER.readTree(text);
                root.path("messages").forEach(message -> messages.add(message.asText()));
                if (messages.isEmpty() && root.hasNonNull("message")) {
                    messages.add(root.get("message").asText());
                }
            } catch (JsonProcessingException exception) {
                messages.add(text);
            }
        }

        messages.removeIf(String::isBlank);
        return messages.isEmpty() ? List.of(fallback) : messages;
    }
}

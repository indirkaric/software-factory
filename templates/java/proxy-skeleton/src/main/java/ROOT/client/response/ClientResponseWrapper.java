package {{rootPackage}}.client.response;

import org.springframework.http.HttpStatusCode;

/**
 * What a downstream answered: its status and body. For a 2xx the body is the decoded JSON (maps and
 * lists), for an error status the raw body as a String. {@code ResponseUtil} turns it into the result.
 *
 * @param statusCode downstream status
 * @param body decoded JSON, the error body, or {@code null} when there was none
 */
public record ClientResponseWrapper(HttpStatusCode statusCode, Object body) {
}

package {{rootPackage}}.util;

import {{rootPackage}}.client.response.ClientResponseWrapper;
import {{rootPackage}}.dto.response.EntityCreatedResponse;
import {{rootPackage}}.exception.BadRequestException;
import {{rootPackage}}.exception.BusinessException;
import {{rootPackage}}.exception.ConflictException;
import {{rootPackage}}.exception.EntityNotFoundException;
import {{rootPackage}}.exception.ServiceUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResponseUtilTest {

    private static final String SERVICE = "Downstream";

    private static final int NON_STANDARD_STATUS = 599;

    @Test
    void shouldMapTheBodyAndIgnoreUnknownFields() {
        EntityCreatedResponse response = ResponseUtil.handleResponse(
                wrap(HttpStatus.CREATED, Map.of("id", "A", "extra", 1)), EntityCreatedResponse.class, SERVICE);

        assertThat(response).isEqualTo(new EntityCreatedResponse("A"));
    }

    @Test
    void shouldReturnNullWithoutABodyOrType() {
        assertThat(ResponseUtil.handleResponse(wrap(HttpStatus.NO_CONTENT, null), EntityCreatedResponse.class,
                SERVICE)).isNull();
        Object body = ResponseUtil.handleResponse(wrap(HttpStatus.OK, Map.of("id", "A")), null, SERVICE);
        assertThat(body).isNull();
    }

    @Test
    void shouldThrowServiceUnavailableWhenUnreachableOr503() {
        assertThatThrownBy(() -> ResponseUtil.handleResponse(null, null, SERVICE))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Downstream service is unavailable. Please try again later.");
        assertThatThrownBy(() -> ResponseUtil.handleResponse(wrap(HttpStatus.SERVICE_UNAVAILABLE, ""), null, SERVICE))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void shouldKeepTheDownstreamStatusAndMessages() {
        assertThatThrownBy(() -> fail(HttpStatus.BAD_REQUEST, "{\"messages\":[\"a\",\"b\"]}"))
                .isInstanceOfSatisfying(BadRequestException.class,
                        e -> assertThat(e.getMessages()).containsExactly("a", "b"));
        assertThatThrownBy(() -> fail(HttpStatus.FORBIDDEN, ""))
                .isInstanceOf(AccessDeniedException.class).hasMessage("Downstream answered 403.");
        assertThatThrownBy(() -> fail(HttpStatus.NOT_FOUND, "{\"message\":\"missing\"}"))
                .isInstanceOf(EntityNotFoundException.class).hasMessage("missing");
        assertThatThrownBy(() -> fail(HttpStatus.CONFLICT, "plain text"))
                .isInstanceOf(ConflictException.class).hasMessage("plain text");
        assertThatThrownBy(() -> fail(HttpStatus.UNPROCESSABLE_ENTITY, "{\"messages\":[\"taken\"]}"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getMessages()).containsExactly("taken"));
    }

    @Test
    void shouldTurnAnyOtherErrorInto500() {
        assertThatThrownBy(() -> fail(HttpStatus.INTERNAL_SERVER_ERROR, "{\"message\":\"boom\"}"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("500");
        assertThatThrownBy(() -> ResponseUtil.handleResponse(
                new ClientResponseWrapper(HttpStatusCode.valueOf(NON_STANDARD_STATUS), null), null, SERVICE))
                .isInstanceOf(IllegalStateException.class).hasMessage("Downstream answered 599.");
    }

    @Test
    void shouldRequireTheServiceName() {
        assertThatThrownBy(() -> ResponseUtil.handleResponse(wrap(HttpStatus.OK, null), null, ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static void fail(final HttpStatus status, final String body) {
        ResponseUtil.handleResponse(wrap(status, body), null, SERVICE);
    }

    private static ClientResponseWrapper wrap(final HttpStatus status, final Object body) {
        return new ClientResponseWrapper(status, body);
    }
}

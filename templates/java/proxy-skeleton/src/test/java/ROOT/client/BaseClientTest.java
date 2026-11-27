package {{rootPackage}}.client;

import {{rootPackage}}.client.response.ClientResponseWrapper;
import {{rootPackage}}.constants.Constants;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BaseClientTest {

    private final AtomicReference<ClientRequest> sent = new AtomicReference<>();

    @Test
    void shouldBindBaseUriAndClientIdAndDecodeTheBody() {
        ClientResponseWrapper response = client(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).body("{\"id\":\"A\"}").build())
                .call("/v1/items");

        assertThat(sent.get().url().toString()).isEqualTo("http://downstream.local/api/v1/items");
        assertThat(sent.get().headers().getFirst(Constants.SERVICE_COMMUNICATION_ID)).isEqualTo("Test.Downstream");
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).isEqualTo(Map.of("id", "A"));
    }

    @Test
    void shouldKeepTheStatusAndRawBodyOfAnError() {
        ClientResponseWrapper response = client(ClientResponse.create(HttpStatus.UNPROCESSABLE_ENTITY)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"messages\":[\"taken\"]}").build())
                .call("/v1/items");

        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.body()).isEqualTo("{\"messages\":[\"taken\"]}");
    }

    @Test
    void shouldReturnNullWhenTheDownstreamCannotBeReached() {
        WebClient unreachable = WebClient.builder().exchangeFunction(request -> Mono.error(
                new WebClientRequestException(new IllegalStateException("refused"), HttpMethod.GET,
                        URI.create("http://downstream.local"), HttpHeaders.EMPTY))).build();

        assertThat(new TestClient(unreachable).call("/v1/items")).isNull();
    }

    @Test
    void shouldEncodeEveryQueryValueAndSkipEmptyOnes() {
        client(ClientResponse.create(HttpStatus.NO_CONTENT).build()).webClient().get().uri(builder -> {
            Map<String, Object> variables = new HashMap<>();
            builder.path("/v1/items");
            BaseClient.queryParam(builder, variables, "ids", List.of("A", "B"));
            BaseClient.queryParam(builder, variables, "textSearch", "a+b&c=d");
            BaseClient.queryParam(builder, variables, "status", null);
            BaseClient.queryParam(builder, variables, "empty", "");
            return builder.build(variables);
        }).retrieve().toBodilessEntity().block();

        assertThat(sent.get().url().toString())
                .isEqualTo("http://downstream.local/api/v1/items?ids=A&ids=B&textSearch=a%2Bb%26c%3Dd");
    }

    @Test
    void shouldRequireArguments() {
        assertThatThrownBy(() -> new TestClient(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client(ClientResponse.create(HttpStatus.OK).build()).exchange(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private TestClient client(final ClientResponse response) {
        return new TestClient(WebClient.builder().exchangeFunction(request -> {
            sent.set(request);
            return Mono.just(response);
        }).build());
    }

    private static final class TestClient extends BaseClient {

        TestClient(final WebClient webClient) {
            super(webClient, "http://downstream.local/api/", "Test.Downstream");
        }

        ClientResponseWrapper call(final String path) {
            return exchange(webClient().get().uri(path));
        }
    }
}

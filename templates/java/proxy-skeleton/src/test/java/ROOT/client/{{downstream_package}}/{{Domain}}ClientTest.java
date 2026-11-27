package {{rootPackage}}.client.{{downstream_package}};

import {{rootPackage}}.client.response.ClientResponseWrapper;
import {{rootPackage}}.constants.Constants;
import {{rootPackage}}.modules.{{downstream_package}}.{{domain_package}}.model.Create{{Domain}}Request;
import {{rootPackage}}.modules.{{downstream_package}}.{{domain_package}}.model.Modify{{Domain}}Request;
import {{rootPackage}}.modules.{{downstream_package}}.{{domain_package}}.model.{{Domain}}ListRequest;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The requests the client sends to {{downstream}}: method, URL, query string and headers.
 */
class {{Domain}}ClientTest {

    private static final String BASE = "http://{{downstream}}.local/{{downstream}}/";

    private static final String URL = BASE + "v1/{{domain-path}}";

    private static final int PAGE = 2;

    private static final int SIZE = 10;

    private final AtomicReference<ClientRequest> sent = new AtomicReference<>();

    private final {{Domain}}Client client = new {{Domain}}Client(WebClient.builder()
            .exchangeFunction(request -> {
                sent.set(request);
                return Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"id\":\"ABC1234\"}").build());
            }).build(), BASE);

    @Test
    void shouldSendFiltersPagingAndSortAsQueryParameters() {
        var request = new {{Domain}}ListRequest(List.of("A", "B"), (short) 1, "x y");

        ClientResponseWrapper response = client.getList(request,
                PageRequest.of(PAGE, SIZE, Sort.by(Sort.Order.desc("name"))));

        assertThat(sent.get().method()).isEqualTo(HttpMethod.GET);
        assertThat(sent.get().url().toString())
                .isEqualTo(URL + "?ids=A&ids=B&status=1&textSearch=x%20y&page=2&size=10&sort=name%2Cdesc");
        assertThat(sent.get().headers().getFirst(Constants.SERVICE_COMMUNICATION_ID))
                .isEqualTo(Constants.{{DOWNSTREAM}}_CLIENT);
        assertThat(response.body()).isEqualTo(Map.of("id", "ABC1234"));
    }

    @Test
    void shouldLeaveOutEmptyFilters() {
        client.getList(new {{Domain}}ListRequest(), PageRequest.of(0, SIZE));

        assertThat(sent.get().url().toString()).isEqualTo(URL + "?page=0&size=" + SIZE);
    }

    @Test
    void shouldCallTheItemEndpoints() {
        client.get("ABC1234");
        assertThat(sent.get().method()).isEqualTo(HttpMethod.GET);
        assertThat(sent.get().url().toString()).isEqualTo(URL + "/ABC1234");

        client.create(new Create{{Domain}}Request("n", "d"));
        assertThat(sent.get().method()).isEqualTo(HttpMethod.POST);
        assertThat(sent.get().url().toString()).isEqualTo(URL);
        assertThat(sent.get().headers().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

        client.modify("ABC1234", new Modify{{Domain}}Request("n", "d", (short) 1));
        assertThat(sent.get().method()).isEqualTo(HttpMethod.PUT);
        assertThat(sent.get().url().toString()).isEqualTo(URL + "/ABC1234");

        client.delete("ABC1234");
        assertThat(sent.get().method()).isEqualTo(HttpMethod.DELETE);
        assertThat(sent.get().url().toString()).isEqualTo(URL + "/ABC1234");
    }

    @Test
    void shouldRejectMissingArguments() {
        assertThatThrownBy(() -> client.getList(null, PageRequest.of(0, 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.get("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.create(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.modify("A", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.delete(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}

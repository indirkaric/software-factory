package {{rootPackage}}.client;

import {{rootPackage}}.client.response.ClientResponseWrapper;
import {{rootPackage}}.constants.Constants;
import {{rootPackage}}.util.Check;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * Base of every {@code client/<downstream>/<Resource>Client}. Binds the shared WebClient to one
 * downstream base URI and its {@code service-communication-id}, so no call can forget the header, and
 * turns every call into a {@link ClientResponseWrapper}: the downstream's status and body, also for a
 * 4xx or 5xx, or {@code null} when the downstream could not be reached (retries spent). The service
 * layer hands the wrapper to {@code ResponseUtil}, which returns the body or throws the matching exception.
 */
public abstract class BaseClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseClient.class);

    private final WebClient webClient;

    /**
     * @param webClient shared WebClient from {@code WebClientConfig}
     * @param baseUri downstream base URI, from {@code client.<name>.base-uri}
     * @param clientId outbound id, format "&lt;ThisService&gt;.&lt;Downstream&gt;"
     */
    protected BaseClient(final WebClient webClient, final String baseUri, final String clientId) {
        Check.isNotNull(webClient, "webClient");
        Check.isNotEmpty(baseUri, "baseUri");
        Check.isNotEmpty(clientId, "clientId");
        this.webClient = webClient.mutate()
                .baseUrl(baseUri)
                .defaultHeader(Constants.SERVICE_COMMUNICATION_ID, clientId)
                .build();
    }

    /**
     * @return WebClient bound to this downstream
     */
    protected final WebClient webClient() {
        return webClient;
    }

    /**
     * Sends the request and waits for the answer on the calling thread: the scope context the
     * header filter reads lives in ThreadLocals.
     *
     * @param request request with URI, headers and body set
     * @return status and body (the raw error body for a 4xx/5xx), or {@code null} when unreachable
     */
    protected final ClientResponseWrapper exchange(final WebClient.RequestHeadersSpec<?> request) {
        Check.isNotNull(request, "request");

        try {
            return request.accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(Object.class)
                    .map(entity -> new ClientResponseWrapper(entity.getStatusCode(), entity.getBody()))
                    .onErrorResume(WebClientResponseException.class, BaseClient::toWrapper)
                    .block();
        } catch (RuntimeException exception) {
            LOGGER.error("Downstream call failed: {}", exception.getMessage(), exception);
            return null;
        }
    }

    /**
     * Adds query parameter values as URI variables, so every value is fully encoded ({@code +},
     * {@code &} and {@code =} included). A null or empty value adds nothing.
     *
     * @param builder URI builder of the request
     * @param variables URI variables, passed to {@code builder.build(variables)} at the end
     * @param name query parameter name
     * @param values values; a collection adds one parameter per element
     */
    protected static void queryParam(final UriBuilder builder, final Map<String, Object> variables,
                                     final String name, final Object values) {
        Check.isNotNull(builder, "builder");
        Check.isNotNull(variables, "variables");
        Check.isNotEmpty(name, "name");

        Collection<?> all = values instanceof Collection<?> collection ? collection : Arrays.asList(values);
        for (Object value : all) {
            if (value != null && !value.toString().isEmpty()) {
                String key = "q" + variables.size();
                builder.queryParam(name, "{" + key + "}");
                variables.put(key, value);
            }
        }
    }

    private static Mono<ClientResponseWrapper> toWrapper(final WebClientResponseException exception) {
        LOGGER.warn("Downstream answered {}: {}", exception.getStatusCode().value(),
                exception.getResponseBodyAsString());
        return Mono.just(new ClientResponseWrapper(exception.getStatusCode(), exception.getResponseBodyAsString()));
    }
}

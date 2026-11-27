package {{rootPackage}}.client.{{downstream_package}};

import {{rootPackage}}.client.BaseClient;
import {{rootPackage}}.client.response.ClientResponseWrapper;
import {{rootPackage}}.constants.Constants;
import {{rootPackage}}.modules.{{downstream_package}}.{{domain_package}}.model.Create{{Domain}}Request;
import {{rootPackage}}.modules.{{downstream_package}}.{{domain_package}}.model.Modify{{Domain}}Request;
import {{rootPackage}}.modules.{{downstream_package}}.{{domain_package}}.model.{{Domain}}ListRequest;
import {{rootPackage}}.util.Check;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * {{downstream}}'s {@code {{domain-path}}} endpoints. Every method returns what {{downstream}} answered;
 * the services turn it into a result with {@code ResponseUtil}.
 */
@Component
public class {{Domain}}Client extends BaseClient {

    /** Name of the downstream in error messages. */
    public static final String SERVICE_NAME = "{{Downstream}}";

    private static final String PATH = "/v1/{{domain-path}}";

    private static final String ITEM_PATH = PATH + "/{id}";

    /**
     * @param webClient shared WebClient
     * @param baseUri {@code client.{{downstream}}.base-uri}
     */
    public {{Domain}}Client(final WebClient webClient,
                            @Value("${client.{{downstream}}.base-uri}") final String baseUri) {
        super(webClient, baseUri, Constants.{{DOWNSTREAM}}_CLIENT);
    }

    /**
     * Designed for extension.
     *
     * @param request filters; empty ones are not sent
     * @param pageable page, size and sort
     * @return {{downstream}}'s answer
     */
    public ClientResponseWrapper getList(final {{Domain}}ListRequest request, final Pageable pageable) {
        Check.isNotNull(request, "request");
        Check.isNotNull(pageable, "pageable");

        return exchange(webClient().get().uri(builder -> {
            Map<String, Object> variables = new HashMap<>();
            builder.path(PATH);
            queryParam(builder, variables, "ids", request.getIds());
            queryParam(builder, variables, "status", request.getStatus());
            queryParam(builder, variables, "textSearch", request.getTextSearch());
            queryParam(builder, variables, "page", pageable.getPageNumber());
            queryParam(builder, variables, "size", pageable.getPageSize());
            pageable.getSort().forEach(order -> queryParam(builder, variables, "sort",
                    order.getProperty() + "," + order.getDirection().name().toLowerCase(Locale.ROOT)));
            return builder.build(variables);
        }));
    }

    /**
     * Designed for extension.
     *
     * @param id public id
     * @return {{downstream}}'s answer
     */
    public ClientResponseWrapper get(final String id) {
        Check.isNotEmpty(id, "id");

        return exchange(webClient().get().uri(ITEM_PATH, id));
    }

    /**
     * Designed for extension.
     *
     * @param request new {{domain}}
     * @return {{downstream}}'s answer, {@code {"id": ...}} on 201
     */
    public ClientResponseWrapper create(final Create{{Domain}}Request request) {
        Check.isNotNull(request, "request");

        return exchange(webClient().post().uri(PATH).contentType(MediaType.APPLICATION_JSON).bodyValue(request));
    }

    /**
     * Designed for extension.
     *
     * @param id public id
     * @param request the full new state
     * @return {{downstream}}'s answer, 204 on success
     */
    public ClientResponseWrapper modify(final String id, final Modify{{Domain}}Request request) {
        Check.isNotEmpty(id, "id");
        Check.isNotNull(request, "request");

        return exchange(webClient().put().uri(ITEM_PATH, id).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request));
    }

    /**
     * Designed for extension.
     *
     * @param id public id
     * @return {{downstream}}'s answer, 204 on success
     */
    public ClientResponseWrapper delete(final String id) {
        Check.isNotEmpty(id, "id");

        return exchange(webClient().delete().uri(ITEM_PATH, id));
    }
}

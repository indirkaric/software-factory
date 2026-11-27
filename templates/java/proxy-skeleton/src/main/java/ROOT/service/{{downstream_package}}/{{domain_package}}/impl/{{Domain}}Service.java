package {{rootPackage}}.service.{{downstream_package}}.{{domain_package}}.impl;

import {{rootPackage}}.client.{{downstream_package}}.{{Domain}}Client;
import {{rootPackage}}.exception.EntityNotFoundException;
import {{rootPackage}}.modules.{{downstream_package}}.{{domain_package}}.model.{{Domain}}ListRequest;
import {{rootPackage}}.modules.{{downstream_package}}.{{domain_package}}.model.{{Domain}}ListResponse;
import {{rootPackage}}.modules.{{downstream_package}}.{{domain_package}}.model.{{Domain}}Response;
import {{rootPackage}}.service.{{downstream_package}}.{{domain_package}}.I{{Domain}}Service;
import {{rootPackage}}.util.Check;
import {{rootPackage}}.util.ResponseUtil;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Reads {{domain}}s through {@link {{Domain}}Client}. A 204 from {{downstream}} is an empty page.
 */
@Service
public class {{Domain}}Service implements I{{Domain}}Service {

    private final {{Domain}}Client client;

    /**
     * Constructor with dependency injection.
     *
     * @param client {{downstream}} client
     */
    public {{Domain}}Service(final {{Domain}}Client client) {
        Check.isNotNull(client, "client");
        this.client = client;
    }

    /**
     * Designed for extension.
     *
     * @param request filters
     * @param pageable page, size and sort
     * @return one page; empty data when {{downstream}} answers 204
     */
    @Override
    public {{Domain}}ListResponse getList(final {{Domain}}ListRequest request,
                                          final Pageable pageable) {
        Check.isNotNull(request, "request");
        Check.isNotNull(pageable, "pageable");

        {{Domain}}ListResponse response = ResponseUtil.handleResponse(client.getList(request, pageable),
                {{Domain}}ListResponse.class, {{Domain}}Client.SERVICE_NAME);
        if (response == null || response.data() == null) {
            return new {{Domain}}ListResponse(List.of(), 0);
        }

        return response;
    }

    /**
     * Designed for extension.
     *
     * @param id public id
     * @return the {{domain}}
     */
    @Override
    public {{Domain}}Response get(final String id) {
        Check.isNotEmpty(id, "id");

        {{Domain}}Response response = ResponseUtil.handleResponse(
                client.get(id), {{Domain}}Response.class, {{Domain}}Client.SERVICE_NAME);
        if (response == null) {
            throw new EntityNotFoundException("{{Domain}} not found.");
        }

        return response;
    }
}

package {{rootPackage}}.service.{{downstream_package}}.{{domain_package}}.impl;

import {{rootPackage}}.client.{{downstream_package}}.{{Domain}}Client;
import {{rootPackage}}.dto.response.EntityCreatedResponse;
import {{rootPackage}}.modules.{{downstream_package}}.{{domain_package}}.model.Create{{Domain}}Request;
import {{rootPackage}}.service.{{downstream_package}}.{{domain_package}}.ICreate{{Domain}}Service;
import {{rootPackage}}.util.Check;
import {{rootPackage}}.util.ResponseUtil;
import {{rootPackage}}.util.StringHelper;
import org.springframework.stereotype.Service;

/**
 * Creates a {{domain}} through {@link {{Domain}}Client}.
 */
@Service
public class Create{{Domain}}Service implements ICreate{{Domain}}Service {

    private final {{Domain}}Client client;

    /**
     * Constructor with dependency injection.
     *
     * @param client {{downstream}} client
     */
    public Create{{Domain}}Service(final {{Domain}}Client client) {
        Check.isNotNull(client, "client");
        this.client = client;
    }

    /**
     * Designed for extension.
     *
     * @param request validated, normalized request
     * @return public id of the new {{domain}}
     */
    @Override
    public String create(final Create{{Domain}}Request request) {
        Check.isNotNull(request, "request");

        EntityCreatedResponse created = ResponseUtil.handleResponse(client.create(request),
                EntityCreatedResponse.class, {{Domain}}Client.SERVICE_NAME);
        if (created == null || StringHelper.isEmpty(created.id())) {
            throw new IllegalStateException(
                    {{Domain}}Client.SERVICE_NAME + " created a {{domain}} without an id.");
        }

        return created.id();
    }
}

package {{rootPackage}}.service.{{downstream_package}}.{{domain_package}}.impl;

import {{rootPackage}}.client.{{downstream_package}}.{{Domain}}Client;
import {{rootPackage}}.modules.{{downstream_package}}.{{domain_package}}.model.Modify{{Domain}}Request;
import {{rootPackage}}.service.{{downstream_package}}.{{domain_package}}.IModify{{Domain}}Service;
import {{rootPackage}}.util.Check;
import {{rootPackage}}.util.ResponseUtil;
import org.springframework.stereotype.Service;

/**
 * Modifies a {{domain}} through {@link {{Domain}}Client}.
 */
@Service
public class Modify{{Domain}}Service implements IModify{{Domain}}Service {

    private final {{Domain}}Client client;

    /**
     * Constructor with dependency injection.
     *
     * @param client {{downstream}} client
     */
    public Modify{{Domain}}Service(final {{Domain}}Client client) {
        Check.isNotNull(client, "client");
        this.client = client;
    }

    /**
     * Designed for extension.
     *
     * @param id public id
     * @param request validated, normalized request
     */
    @Override
    public void modify(final String id, final Modify{{Domain}}Request request) {
        Check.isNotEmpty(id, "id");
        Check.isNotNull(request, "request");

        ResponseUtil.handleResponse(client.modify(id, request), null, {{Domain}}Client.SERVICE_NAME);
    }
}

package {{rootPackage}}.service.{{downstream_package}}.{{domain_package}}.impl;

import {{rootPackage}}.client.{{downstream_package}}.{{Domain}}Client;
import {{rootPackage}}.service.{{downstream_package}}.{{domain_package}}.IDelete{{Domain}}Service;
import {{rootPackage}}.util.Check;
import {{rootPackage}}.util.ResponseUtil;
import org.springframework.stereotype.Service;

/**
 * Deletes a {{domain}} through {@link {{Domain}}Client}.
 */
@Service
public class Delete{{Domain}}Service implements IDelete{{Domain}}Service {

    private final {{Domain}}Client client;

    /**
     * Constructor with dependency injection.
     *
     * @param client {{downstream}} client
     */
    public Delete{{Domain}}Service(final {{Domain}}Client client) {
        Check.isNotNull(client, "client");
        this.client = client;
    }

    /**
     * Designed for extension.
     *
     * @param id public id
     */
    @Override
    public void delete(final String id) {
        Check.isNotEmpty(id, "id");

        ResponseUtil.handleResponse(client.delete(id), null, {{Domain}}Client.SERVICE_NAME);
    }
}

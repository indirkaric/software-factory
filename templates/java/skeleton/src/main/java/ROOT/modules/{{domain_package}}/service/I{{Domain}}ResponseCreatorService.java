package {{rootPackage}}.modules.{{domain_package}}.service;

import {{rootPackage}}.modules.{{domain_package}}.model.{{Domain}}Response;

/**
 * Builds the {@code GET /{id}} response.
 */
public interface I{{Domain}}ResponseCreatorService {

    /**
     * @param entityId public id
     * @return the response
     */
    {{Domain}}Response create(String entityId);
}

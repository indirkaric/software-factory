package {{rootPackage}}.service.{{domain_package}};

/**
 * Deletes a {{domain}}.
 */
public interface IDelete{{Domain}}Service {

    /**
     * @param entityId public id
     */
    void delete(String entityId);
}

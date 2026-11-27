package {{rootPackage}}.service.{{downstream_package}}.{{domain_package}};

/**
 * Deletes a {{domain}} in {{downstream}}.
 */
public interface IDelete{{Domain}}Service {

    /**
     * @param id public id
     */
    void delete(String id);
}

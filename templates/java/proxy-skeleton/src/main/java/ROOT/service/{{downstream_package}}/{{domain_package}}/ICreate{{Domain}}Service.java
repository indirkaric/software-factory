package {{rootPackage}}.service.{{downstream_package}}.{{domain_package}};

import {{rootPackage}}.modules.{{downstream_package}}.{{domain_package}}.model.Create{{Domain}}Request;

/**
 * Creates a {{domain}} in {{downstream}}.
 */
public interface ICreate{{Domain}}Service {

    /**
     * @param request new {{domain}}
     * @return public id of the new {{domain}}
     */
    String create(Create{{Domain}}Request request);
}

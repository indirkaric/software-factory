package {{rootPackage}}.service.{{domain_package}};

import {{rootPackage}}.modules.{{domain_package}}.model.Create{{Domain}}Request;

/**
 * Creates a {{domain}}.
 */
public interface ICreate{{Domain}}Service {

    /**
     * @param request validated, normalized request
     * @return public id of the new {{domain}}
     */
    String create(Create{{Domain}}Request request);
}

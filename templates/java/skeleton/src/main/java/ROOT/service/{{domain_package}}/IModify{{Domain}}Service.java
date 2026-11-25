package {{rootPackage}}.service.{{domain_package}};

import {{rootPackage}}.modules.{{domain_package}}.model.Modify{{Domain}}Request;

/**
 * Modifies a {{domain}}.
 */
public interface IModify{{Domain}}Service {

    /**
     * @param entityId public id
     * @param request validated, normalized request
     */
    void modify(String entityId, Modify{{Domain}}Request request);
}

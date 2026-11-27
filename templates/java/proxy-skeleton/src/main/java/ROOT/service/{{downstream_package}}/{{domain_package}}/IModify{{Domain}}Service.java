package {{rootPackage}}.service.{{downstream_package}}.{{domain_package}};

import {{rootPackage}}.modules.{{downstream_package}}.{{domain_package}}.model.Modify{{Domain}}Request;

/**
 * Modifies a {{domain}} in {{downstream}}.
 */
public interface IModify{{Domain}}Service {

    /**
     * @param id public id
     * @param request the full new state
     */
    void modify(String id, Modify{{Domain}}Request request);
}

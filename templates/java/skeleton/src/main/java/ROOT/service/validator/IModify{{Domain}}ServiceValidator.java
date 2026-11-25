package {{rootPackage}}.service.validator;

import {{rootPackage}}.entity.{{Domain}};
import {{rootPackage}}.modules.{{domain_package}}.model.Modify{{Domain}}Request;

/**
 * Business rules of modify {{domain}}; violations are a {@code BusinessException} (422).
 */
public interface IModify{{Domain}}ServiceValidator {

    /**
     * @param request modify request
     * @param existing the {{domain}} being modified
     */
    void validate(Modify{{Domain}}Request request, {{Domain}} existing);
}

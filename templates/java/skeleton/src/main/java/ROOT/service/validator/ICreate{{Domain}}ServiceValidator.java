package {{rootPackage}}.service.validator;

import {{rootPackage}}.modules.{{domain_package}}.model.Create{{Domain}}Request;

/**
 * Business rules of create {{domain}}; violations are a {@code BusinessException} (422).
 */
public interface ICreate{{Domain}}ServiceValidator {

    /**
     * @param request create request
     */
    void validate(Create{{Domain}}Request request);
}

package {{rootPackage}}.service.validator;

import {{rootPackage}}.entity.{{Domain}};

/**
 * Business rules of delete {{domain}}; violations are a {@code BusinessException} (422).
 */
public interface IDelete{{Domain}}ServiceValidator {

    /**
     * @param existing the {{domain}} to delete
     */
    void validate({{Domain}} existing);
}

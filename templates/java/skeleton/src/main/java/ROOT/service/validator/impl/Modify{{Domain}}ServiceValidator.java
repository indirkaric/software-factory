package {{rootPackage}}.service.validator.impl;

import {{rootPackage}}.entity.{{Domain}};
import {{rootPackage}}.modules.{{domain_package}}.model.Modify{{Domain}}Request;
import {{rootPackage}}.repository.I{{Domain}}Repository;
import {{rootPackage}}.service.validator.Base{{Domain}}DataValidator;
import {{rootPackage}}.service.validator.IModify{{Domain}}ServiceValidator;
import {{rootPackage}}.util.Check;
import org.springframework.stereotype.Service;

/**
 * Modify rules: the new name is free, ignoring the {{domain}} itself.
 */
@Service
public class Modify{{Domain}}ServiceValidator extends Base{{Domain}}DataValidator
        implements IModify{{Domain}}ServiceValidator {

    /**
     * Constructor with dependency injection.
     *
     * @param repository {{domain}} repository
     */
    public Modify{{Domain}}ServiceValidator(final I{{Domain}}Repository repository) {
        super(repository);
    }

    /**
     * Designed for extension.
     *
     * @param request modify request
     * @param existing the {{domain}} being modified
     */
    @Override
    public void validate(final Modify{{Domain}}Request request, final {{Domain}} existing) {
        Check.isNotNull(request, "request");
        Check.isNotNull(existing, "existing");

        validateUniqueName(request.getName(), existing.getEntityId());
    }
}

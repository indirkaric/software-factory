package {{rootPackage}}.service.validator.impl;

import {{rootPackage}}.modules.{{domain_package}}.model.Create{{Domain}}Request;
import {{rootPackage}}.repository.I{{Domain}}Repository;
import {{rootPackage}}.service.validator.Base{{Domain}}DataValidator;
import {{rootPackage}}.service.validator.ICreate{{Domain}}ServiceValidator;
import {{rootPackage}}.util.Check;
import org.springframework.stereotype.Service;

/**
 * Create rules: the name is free.
 */
@Service
public class Create{{Domain}}ServiceValidator extends Base{{Domain}}DataValidator
        implements ICreate{{Domain}}ServiceValidator {

    /**
     * Constructor with dependency injection.
     *
     * @param repository {{domain}} repository
     */
    public Create{{Domain}}ServiceValidator(final I{{Domain}}Repository repository) {
        super(repository);
    }

    /**
     * Designed for extension.
     *
     * @param request create request
     */
    @Override
    public void validate(final Create{{Domain}}Request request) {
        Check.isNotNull(request, "request");

        validateUniqueName(request.getName(), null);
    }
}

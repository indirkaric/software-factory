package {{rootPackage}}.service.{{domain_package}}.impl;

import {{rootPackage}}.entity.{{Domain}};
import {{rootPackage}}.exception.EntityNotFoundException;
import {{rootPackage}}.repository.I{{Domain}}Repository;
import {{rootPackage}}.service.validator.IDelete{{Domain}}ServiceValidator;
import {{rootPackage}}.service.{{domain_package}}.IDelete{{Domain}}Service;
import {{rootPackage}}.util.Check;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes a {{domain}}.
 */
@Service
@Transactional
public class Delete{{Domain}}Service implements IDelete{{Domain}}Service {

    private final I{{Domain}}Repository repository;

    private final IDelete{{Domain}}ServiceValidator validator;

    /**
     * Constructor with dependency injection.
     *
     * @param repository {{domain}} repository
     * @param validator delete validator
     */
    public Delete{{Domain}}Service(final I{{Domain}}Repository repository,
                                   final IDelete{{Domain}}ServiceValidator validator) {
        Check.isNotNull(repository, "repository");
        Check.isNotNull(validator, "validator");

        this.repository = repository;
        this.validator = validator;
    }

    /**
     * Designed for extension.
     *
     * @param entityId public id
     */
    @Override
    public void delete(final String entityId) {
        Check.isNotEmpty(entityId, "entityId");

        {{Domain}} entity = repository.findByEntityId(entityId)
                .orElseThrow(() -> new EntityNotFoundException("{{Domain}} with id { id: " + entityId
                        + " } doesn't exist."));

        validator.validate(entity);

        repository.delete(entity);
    }
}

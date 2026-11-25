package {{rootPackage}}.service.{{domain_package}}.impl;

import {{rootPackage}}.entity.{{Domain}};
import {{rootPackage}}.exception.EntityNotFoundException;
import {{rootPackage}}.modules.{{domain_package}}.model.Modify{{Domain}}Request;
import {{rootPackage}}.repository.I{{Domain}}Repository;
import {{rootPackage}}.service.validator.IModify{{Domain}}ServiceValidator;
import {{rootPackage}}.service.{{domain_package}}.IModify{{Domain}}Service;
import {{rootPackage}}.util.Check;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Modifies a {{domain}}. The loaded entity is managed, so the changes are flushed on commit.
 */
@Service
@Transactional
public class Modify{{Domain}}Service implements IModify{{Domain}}Service {

    private final I{{Domain}}Repository repository;

    private final IModify{{Domain}}ServiceValidator validator;

    /**
     * Constructor with dependency injection.
     *
     * @param repository {{domain}} repository
     * @param validator modify validator
     */
    public Modify{{Domain}}Service(final I{{Domain}}Repository repository,
                                   final IModify{{Domain}}ServiceValidator validator) {
        Check.isNotNull(repository, "repository");
        Check.isNotNull(validator, "validator");

        this.repository = repository;
        this.validator = validator;
    }

    /**
     * Designed for extension.
     *
     * @param entityId public id
     * @param request validated, normalized request
     */
    @Override
    public void modify(final String entityId, final Modify{{Domain}}Request request) {
        Check.isNotEmpty(entityId, "entityId");
        Check.isNotNull(request, "request");

        {{Domain}} entity = repository.findByEntityId(entityId)
                .orElseThrow(() -> new EntityNotFoundException("{{Domain}} with id { id: " + entityId
                        + " } doesn't exist."));

        validator.validate(request, entity);

        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setStatus(request.getStatus());
    }
}

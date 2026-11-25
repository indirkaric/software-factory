package {{rootPackage}}.service.{{domain_package}}.impl;

import {{rootPackage}}.constants.Constants;
import {{rootPackage}}.entity.{{Domain}};
import {{rootPackage}}.modules.{{domain_package}}.model.Create{{Domain}}Request;
import {{rootPackage}}.repository.I{{Domain}}Repository;
import {{rootPackage}}.service.generator.IEntityIdGeneratorService;
import {{rootPackage}}.service.validator.ICreate{{Domain}}ServiceValidator;
import {{rootPackage}}.service.{{domain_package}}.ICreate{{Domain}}Service;
import {{rootPackage}}.util.Check;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a {{domain}}: validate, generate the public id, insert.
 */
@Service
@Transactional
public class Create{{Domain}}Service implements ICreate{{Domain}}Service {

    private final I{{Domain}}Repository repository;

    private final ICreate{{Domain}}ServiceValidator validator;

    private final IEntityIdGeneratorService entityIdGenerator;

    /**
     * Constructor with dependency injection.
     *
     * @param repository {{domain}} repository
     * @param validator create validator
     * @param entityIdGenerator public id generator
     */
    public Create{{Domain}}Service(final I{{Domain}}Repository repository,
                                   final ICreate{{Domain}}ServiceValidator validator,
                                   final IEntityIdGeneratorService entityIdGenerator) {
        Check.isNotNull(repository, "repository");
        Check.isNotNull(validator, "validator");
        Check.isNotNull(entityIdGenerator, "entityIdGenerator");

        this.repository = repository;
        this.validator = validator;
        this.entityIdGenerator = entityIdGenerator;
    }

    /**
     * Designed for extension.
     *
     * @param request validated, normalized request
     * @return public id of the new {{domain}}
     */
    @Override
    public String create(final Create{{Domain}}Request request) {
        Check.isNotNull(request, "request");

        validator.validate(request);

        {{Domain}} entity = new {{Domain}}();
        entity.setEntityId(entityIdGenerator.generate(Constants.DEFAULT_ENTITY_ID_SIZE,
                repository::existsByEntityId));
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setStatus(Constants.Status.ACTIVE);

        repository.save(entity);

        return entity.getEntityId();
    }
}

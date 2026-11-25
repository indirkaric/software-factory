package {{rootPackage}}.service.validator;

import {{rootPackage}}.exception.BusinessException;
import {{rootPackage}}.repository.I{{Domain}}Repository;
import {{rootPackage}}.util.Check;

/**
 * {{Domain}} rules shared by more than one write operation. A rule that only one operation has stays in
 * that operation's validator. Field-format rules ({@code @NotBlank}, {@code @Size}) are on the request
 * model and checked by {@code @Valid} (400); validators hold the business rules (422).
 */
public abstract class Base{{Domain}}DataValidator {

    private final I{{Domain}}Repository repository;

    /**
     * Constructor with dependency injection.
     *
     * @param repository {{domain}} repository
     */
    protected Base{{Domain}}DataValidator(final I{{Domain}}Repository repository) {
        Check.isNotNull(repository, "repository");

        this.repository = repository;
    }

    /**
     * Names are unique, case-insensitively. Designed for extension.
     *
     * @param name name to check
     * @param excludedEntityId the {{domain}} being modified, or null on create
     */
    protected void validateUniqueName(final String name, final String excludedEntityId) {
        if (repository.existsByName(name, excludedEntityId)) {
            throw new BusinessException("{{Domain}} with the name { name: " + name + " } already exists.");
        }
    }
}

package {{rootPackage}}.service.validator.impl;

import {{rootPackage}}.constants.Constants;
import {{rootPackage}}.entity.{{Domain}};
import {{rootPackage}}.exception.BusinessException;
import {{rootPackage}}.service.validator.IDelete{{Domain}}ServiceValidator;
import {{rootPackage}}.util.Check;
import org.springframework.stereotype.Service;

/**
 * Delete rules: only an inactive {{domain}} can be deleted.
 */
@Service
public class Delete{{Domain}}ServiceValidator implements IDelete{{Domain}}ServiceValidator {

    /**
     * Designed for extension.
     *
     * @param existing the {{domain}} to delete
     */
    @Override
    public void validate(final {{Domain}} existing) {
        Check.isNotNull(existing, "existing");

        if (existing.getStatus() == null || existing.getStatus() != Constants.Status.INACTIVE) {
            throw new BusinessException("{{Domain}} { id: " + existing.getEntityId()
                    + " } is active; deactivate it before deleting it.");
        }
    }
}

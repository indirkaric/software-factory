package {{rootPackage}}.service.validator.impl;

import {{rootPackage}}.constants.Constants;
import {{rootPackage}}.entity.{{Domain}};
import {{rootPackage}}.exception.BusinessException;
import {{rootPackage}}.service.validator.IDelete{{Domain}}ServiceValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Delete{{Domain}}ServiceValidatorTest {

    private final IDelete{{Domain}}ServiceValidator validator =
            new Delete{{Domain}}ServiceValidator();

    @Test
    void shouldAcceptInactive() {
        assertThatCode(() -> validator.validate(withStatus(Constants.Status.INACTIVE))).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectActiveOrUnknownStatus() {
        assertThatThrownBy(() -> validator.validate(withStatus(Constants.Status.ACTIVE)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("deactivate");
        assertThatThrownBy(() -> validator.validate(withStatus(null))).isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectNull() {
        assertThatThrownBy(() -> validator.validate(null)).isInstanceOf(IllegalArgumentException.class);
    }

    private static {{Domain}} withStatus(final Short status) {
        {{Domain}} entity = new {{Domain}}();
        entity.setEntityId("ABC1234");
        entity.setStatus(status);
        return entity;
    }
}

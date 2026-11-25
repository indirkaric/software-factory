package {{rootPackage}}.service.validator.impl;

import {{rootPackage}}.constants.Constants;
import {{rootPackage}}.entity.{{Domain}};
import {{rootPackage}}.exception.BusinessException;
import {{rootPackage}}.modules.{{domain_package}}.model.Modify{{Domain}}Request;
import {{rootPackage}}.repository.I{{Domain}}Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Modify{{Domain}}ServiceValidatorTest {

    private static final String ID = "ABC1234";

    @Mock
    private I{{Domain}}Repository repository;

    private Modify{{Domain}}ServiceValidator validator;

    private {{Domain}} existing;

    @BeforeEach
    void setUp() {
        validator = new Modify{{Domain}}ServiceValidator(repository);
        existing = new {{Domain}}();
        existing.setEntityId(ID);
    }

    @Test
    void shouldIgnoreItselfWhenCheckingTheName() {
        when(repository.existsByName("Name", ID)).thenReturn(false);

        assertThatCode(() -> validator.validate(request(), existing)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectNameOfAnother() {
        when(repository.existsByName("Name", ID)).thenReturn(true);

        assertThatThrownBy(() -> validator.validate(request(), existing)).isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectMissingArguments() {
        assertThatThrownBy(() -> validator.validate(null, existing)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate(request(), null)).isInstanceOf(IllegalArgumentException.class);
    }

    private static Modify{{Domain}}Request request() {
        return new Modify{{Domain}}Request("Name", null, Constants.Status.ACTIVE);
    }
}

package {{rootPackage}}.service.validator.impl;

import {{rootPackage}}.exception.BusinessException;
import {{rootPackage}}.modules.{{domain_package}}.model.Create{{Domain}}Request;
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
class Create{{Domain}}ServiceValidatorTest {

    @Mock
    private I{{Domain}}Repository repository;

    private Create{{Domain}}ServiceValidator validator;

    @BeforeEach
    void setUp() {
        validator = new Create{{Domain}}ServiceValidator(repository);
    }

    @Test
    void shouldAcceptFreeName() {
        when(repository.existsByName("Name", null)).thenReturn(false);

        assertThatCode(() -> validator.validate(new Create{{Domain}}Request("Name", null)))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectTakenName() {
        when(repository.existsByName("Name", null)).thenReturn(true);

        assertThatThrownBy(() -> validator.validate(new Create{{Domain}}Request("Name", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void shouldRejectMissingArguments() {
        assertThatThrownBy(() -> new Create{{Domain}}ServiceValidator(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate(null)).isInstanceOf(IllegalArgumentException.class);
    }
}

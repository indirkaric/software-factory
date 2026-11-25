package {{rootPackage}}.service.{{domain_package}}.impl;

import {{rootPackage}}.entity.{{Domain}};
import {{rootPackage}}.exception.BusinessException;
import {{rootPackage}}.exception.EntityNotFoundException;
import {{rootPackage}}.repository.I{{Domain}}Repository;
import {{rootPackage}}.service.validator.IDelete{{Domain}}ServiceValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Delete{{Domain}}ServiceTest {

    private static final String ID = "ABC1234";

    @Mock
    private I{{Domain}}Repository repository;

    @Mock
    private IDelete{{Domain}}ServiceValidator validator;

    private Delete{{Domain}}Service service;

    @BeforeEach
    void setUp() {
        service = new Delete{{Domain}}Service(repository, validator);
    }

    @Test
    void shouldValidateAndDelete() {
        {{Domain}} entity = new {{Domain}}();
        when(repository.findByEntityId(ID)).thenReturn(Optional.of(entity));

        service.delete(ID);

        verify(validator).validate(entity);
        verify(repository).delete(entity);
    }

    @Test
    void shouldThrowNotFound() {
        when(repository.findByEntityId(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(ID)).isInstanceOf(EntityNotFoundException.class);

        verify(repository, never()).delete(any());
    }

    @Test
    void shouldNotDeleteWhenValidationFails() {
        {{Domain}} entity = new {{Domain}}();
        when(repository.findByEntityId(ID)).thenReturn(Optional.of(entity));
        doThrow(new BusinessException("active")).when(validator).validate(entity);

        assertThatThrownBy(() -> service.delete(ID)).isInstanceOf(BusinessException.class);

        verify(repository, never()).delete(any());
    }

    @Test
    void shouldRejectMissingArguments() {
        assertThatThrownBy(() -> new Delete{{Domain}}Service(repository, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.delete("")).isInstanceOf(IllegalArgumentException.class);
    }
}

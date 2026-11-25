package {{rootPackage}}.service.{{domain_package}}.impl;

import {{rootPackage}}.constants.Constants;
import {{rootPackage}}.entity.{{Domain}};
import {{rootPackage}}.exception.BusinessException;
import {{rootPackage}}.exception.EntityNotFoundException;
import {{rootPackage}}.modules.{{domain_package}}.model.Modify{{Domain}}Request;
import {{rootPackage}}.repository.I{{Domain}}Repository;
import {{rootPackage}}.service.validator.IModify{{Domain}}ServiceValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Modify{{Domain}}ServiceTest {

    private static final String ID = "ABC1234";

    @Mock
    private I{{Domain}}Repository repository;

    @Mock
    private IModify{{Domain}}ServiceValidator validator;

    private Modify{{Domain}}Service service;

    @BeforeEach
    void setUp() {
        service = new Modify{{Domain}}Service(repository, validator);
    }

    @Test
    void shouldValidateAndApplyChanges() {
        {{Domain}} entity = existing();
        var request = new Modify{{Domain}}Request("New", "Text", Constants.Status.INACTIVE);
        when(repository.findByEntityId(ID)).thenReturn(Optional.of(entity));

        service.modify(ID, request);

        verify(validator).validate(request, entity);
        assertThat(entity.getName()).isEqualTo("New");
        assertThat(entity.getDescription()).isEqualTo("Text");
        assertThat(entity.getStatus()).isEqualTo(Constants.Status.INACTIVE);
    }

    @Test
    void shouldThrowNotFound() {
        when(repository.findByEntityId(ID)).thenReturn(Optional.empty());
        var request = new Modify{{Domain}}Request("New", null, Constants.Status.ACTIVE);

        assertThatThrownBy(() -> service.modify(ID, request)).isInstanceOf(EntityNotFoundException.class);

        verify(validator, never()).validate(any(), any());
    }

    @Test
    void shouldNotChangeWhenValidationFails() {
        {{Domain}} entity = existing();
        var request = new Modify{{Domain}}Request("Taken", null, Constants.Status.ACTIVE);
        when(repository.findByEntityId(ID)).thenReturn(Optional.of(entity));
        doThrow(new BusinessException("taken")).when(validator).validate(request, entity);

        assertThatThrownBy(() -> service.modify(ID, request)).isInstanceOf(BusinessException.class);

        assertThat(entity.getName()).isEqualTo("Old");
    }

    @Test
    void shouldRejectMissingArguments() {
        assertThatThrownBy(() -> new Modify{{Domain}}Service(null, validator))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.modify(ID, null)).isInstanceOf(IllegalArgumentException.class);
    }

    private static {{Domain}} existing() {
        {{Domain}} entity = new {{Domain}}();
        entity.setEntityId(ID);
        entity.setName("Old");
        entity.setStatus(Constants.Status.ACTIVE);
        return entity;
    }
}

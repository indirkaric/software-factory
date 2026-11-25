package {{rootPackage}}.service.{{domain_package}}.impl;

import {{rootPackage}}.constants.Constants;
import {{rootPackage}}.entity.{{Domain}};
import {{rootPackage}}.exception.BusinessException;
import {{rootPackage}}.modules.{{domain_package}}.model.Create{{Domain}}Request;
import {{rootPackage}}.repository.I{{Domain}}Repository;
import {{rootPackage}}.service.generator.IEntityIdGeneratorService;
import {{rootPackage}}.service.validator.ICreate{{Domain}}ServiceValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Create{{Domain}}ServiceTest {

    @Mock
    private I{{Domain}}Repository repository;

    @Mock
    private ICreate{{Domain}}ServiceValidator validator;

    @Mock
    private IEntityIdGeneratorService entityIdGenerator;

    private Create{{Domain}}Service service;

    @BeforeEach
    void setUp() {
        service = new Create{{Domain}}Service(repository, validator, entityIdGenerator);
    }

    @Test
    void shouldValidateGenerateIdAndSaveActive() {
        Create{{Domain}}Request request = new Create{{Domain}}Request("Name", "Text");
        when(entityIdGenerator.generate(eq(Constants.DEFAULT_ENTITY_ID_SIZE), any())).thenReturn("ABC1234");

        String id = service.create(request);

        ArgumentCaptor<{{Domain}}> saved = ArgumentCaptor.forClass({{Domain}}.class);
        verify(validator).validate(request);
        verify(repository).save(saved.capture());
        assertThat(id).isEqualTo("ABC1234");
        assertThat(saved.getValue().getEntityId()).isEqualTo("ABC1234");
        assertThat(saved.getValue().getName()).isEqualTo("Name");
        assertThat(saved.getValue().getDescription()).isEqualTo("Text");
        assertThat(saved.getValue().getStatus()).isEqualTo(Constants.Status.ACTIVE);
    }

    @Test
    void shouldNotSaveWhenValidationFails() {
        Create{{Domain}}Request request = new Create{{Domain}}Request("Taken", null);
        doThrow(new BusinessException("taken")).when(validator).validate(request);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(BusinessException.class);

        verify(entityIdGenerator, never()).generate(anyInt(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void shouldRejectMissingArguments() {
        assertThatThrownBy(() -> new Create{{Domain}}Service(repository, null, entityIdGenerator))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create(null)).isInstanceOf(IllegalArgumentException.class);
    }
}

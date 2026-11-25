package {{rootPackage}}.modules.{{domain_package}}.service.impl;

import {{rootPackage}}.dto.{{Domain}}DTO;
import {{rootPackage}}.mapper.I{{Domain}}Mapper;
import {{rootPackage}}.modules.{{domain_package}}.model.{{Domain}}Response;
import {{rootPackage}}.service.{{domain_package}}.I{{Domain}}Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class {{Domain}}ResponseCreatorServiceTest {

    private static final String ID = "ABC1234";

    @Mock
    private I{{Domain}}Service service;

    @Mock
    private I{{Domain}}Mapper mapper;

    private {{Domain}}ResponseCreatorService creator;

    @BeforeEach
    void setUp() {
        creator = new {{Domain}}ResponseCreatorService(service, mapper);
    }

    @Test
    void shouldMapTheReadDto() {
        {{Domain}}DTO dto = new {{Domain}}DTO(ID, "n", null, (short) 1, null, null);
        {{Domain}}Response response = new {{Domain}}Response(ID, "n", null, (short) 1, null, null);
        when(service.getByEntityId(ID)).thenReturn(dto);
        when(mapper.toResponse(dto)).thenReturn(response);

        assertThat(creator.create(ID)).isEqualTo(response);
    }

    @Test
    void shouldRejectMissingArguments() {
        assertThatThrownBy(() -> new {{Domain}}ResponseCreatorService(service, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> creator.create(null)).isInstanceOf(IllegalArgumentException.class);
    }
}

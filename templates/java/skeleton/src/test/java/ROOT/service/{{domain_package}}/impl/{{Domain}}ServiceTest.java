package {{rootPackage}}.service.{{domain_package}}.impl;

import {{rootPackage}}.dto.{{Domain}}DTO;
import {{rootPackage}}.exception.EntityNotFoundException;
import {{rootPackage}}.mapper.I{{Domain}}Mapper;
import {{rootPackage}}.repository.I{{Domain}}Repository;
import {{rootPackage}}.repository.projection.{{Domain}}ItemProjection;
import {{rootPackage}}.repository.projection.{{Domain}}Projection;
import {{rootPackage}}.service.model.PagedList;
import {{rootPackage}}.service.model.{{Domain}}ListFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class {{Domain}}ServiceTest {

    private static final String ID = "ABC1234";

    @Mock
    private I{{Domain}}Repository repository;

    @Mock
    private I{{Domain}}Mapper mapper;

    private {{Domain}}Service service;

    @BeforeEach
    void setUp() {
        service = new {{Domain}}Service(repository, mapper);
    }

    @Test
    void shouldReturnPageWithTotalCount() {
        {{Domain}}ListFilter filter = new {{Domain}}ListFilter(null, null, "x");
        Pageable pageable = PageRequest.of(0, 1);
        var rows = List.of(new {{Domain}}ItemProjection(ID, "x", (short) 1));
        var dtos = List.of(new {{Domain}}DTO(ID, "x", null, (short) 1, null, null));
        when(repository.findPagedList(filter, pageable)).thenReturn(new PageImpl<>(rows, pageable, 2));
        when(mapper.toItemDtoList(rows)).thenReturn(dtos);

        PagedList<{{Domain}}DTO> page = service.getPagedList(filter, pageable);

        assertThat(page.items()).isEqualTo(dtos);
        assertThat(page.count()).isEqualTo(2);
    }

    @Test
    void shouldReturnOne() {
        var projection = new {{Domain}}Projection(ID, "x", "d", (short) 1, "U1", null);
        {{Domain}}DTO dto = new {{Domain}}DTO(ID, "x", "d", (short) 1, "U1", null);
        when(repository.findProjectionByEntityId(ID)).thenReturn(Optional.of(projection));
        when(mapper.toDto(projection)).thenReturn(dto);

        assertThat(service.getByEntityId(ID)).isEqualTo(dto);
    }

    @Test
    void shouldThrowNotFound() {
        when(repository.findProjectionByEntityId(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByEntityId(ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(ID);
    }

    @Test
    void shouldRejectMissingArguments() {
        assertThatThrownBy(() -> new {{Domain}}Service(null, mapper))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getPagedList(null, PageRequest.of(0, 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getByEntityId(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}

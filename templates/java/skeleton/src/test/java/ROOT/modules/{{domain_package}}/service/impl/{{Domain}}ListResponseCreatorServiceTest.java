package {{rootPackage}}.modules.{{domain_package}}.service.impl;

import {{rootPackage}}.dto.{{Domain}}DTO;
import {{rootPackage}}.mapper.I{{Domain}}Mapper;
import {{rootPackage}}.modules.{{domain_package}}.model.{{Domain}}ItemResponse;
import {{rootPackage}}.modules.{{domain_package}}.model.{{Domain}}ListRequest;
import {{rootPackage}}.modules.{{domain_package}}.model.{{Domain}}ListResponse;
import {{rootPackage}}.service.model.PagedList;
import {{rootPackage}}.service.model.{{Domain}}ListFilter;
import {{rootPackage}}.service.{{domain_package}}.I{{Domain}}Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class {{Domain}}ListResponseCreatorServiceTest {

    private static final String ID = "ABC1234";

    @Mock
    private I{{Domain}}Service service;

    @Mock
    private I{{Domain}}Mapper mapper;

    private {{Domain}}ListResponseCreatorService creator;

    @BeforeEach
    void setUp() {
        creator = new {{Domain}}ListResponseCreatorService(service, mapper);
    }

    @Test
    void shouldPassFiltersAndKeepTheTotalCount() {
        Pageable pageable = PageRequest.of(0, 1);
        List<{{Domain}}DTO> dtos = List.of(new {{Domain}}DTO(ID, "n", null, (short) 1, null, null));
        var items = List.of(new {{Domain}}ItemResponse(ID, "n", (short) 1));
        when(service.getPagedList(any(), eq(pageable))).thenReturn(new PagedList<>(dtos, 2));
        when(mapper.toItemResponseList(dtos)).thenReturn(items);

        var request = new {{Domain}}ListRequest(List.of(ID), (short) 1, "n");

        {{Domain}}ListResponse response = creator.create(request, pageable);

        var filter = ArgumentCaptor.forClass({{Domain}}ListFilter.class);
        verify(service).getPagedList(filter.capture(), eq(pageable));
        assertThat(filter.getValue()).isEqualTo(new {{Domain}}ListFilter(List.of(ID), (short) 1, "n"));
        assertThat(response).isEqualTo(new {{Domain}}ListResponse(items, 2));
    }

    @Test
    void shouldRejectMissingArguments() {
        assertThatThrownBy(() -> new {{Domain}}ListResponseCreatorService(null, mapper))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> creator.create(null, PageRequest.of(0, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

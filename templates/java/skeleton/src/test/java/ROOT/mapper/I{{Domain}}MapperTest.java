package {{rootPackage}}.mapper;

import {{rootPackage}}.dto.{{Domain}}DTO;
import {{rootPackage}}.modules.{{domain_package}}.model.{{Domain}}ItemResponse;
import {{rootPackage}}.modules.{{domain_package}}.model.{{Domain}}Response;
import {{rootPackage}}.repository.projection.{{Domain}}ItemProjection;
import {{rootPackage}}.repository.projection.{{Domain}}Projection;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class I{{Domain}}MapperTest {

    private static final String ID = "ABC1234";

    private final I{{Domain}}Mapper mapper = Mappers.getMapper(I{{Domain}}Mapper.class);

    @Test
    void shouldMapDetailProjectionToResponseWithEntityIdAsId() {
        Date created = new Date();
        var dto = mapper.toDto(new {{Domain}}Projection(ID, "n", "d", (short) 1, "U1", created));

        {{Domain}}Response response = mapper.toResponse(dto);

        assertThat(response).isEqualTo(new {{Domain}}Response(ID, "n", "d", (short) 1, "U1", created));
    }

    @Test
    void shouldMapListRows() {
        var row = new {{Domain}}ItemProjection(ID, "n", (short) 0);
        List<{{Domain}}DTO> dtos = mapper.toItemDtoList(List.of(row));

        List<{{Domain}}ItemResponse> items = mapper.toItemResponseList(dtos);

        assertThat(dtos.get(0).description()).isNull();
        assertThat(items).containsExactly(new {{Domain}}ItemResponse(ID, "n", (short) 0));
    }
}

package {{rootPackage}}.mapper;

import {{rootPackage}}.dto.{{Domain}}DTO;
import {{rootPackage}}.modules.{{domain_package}}.model.{{Domain}}ItemResponse;
import {{rootPackage}}.modules.{{domain_package}}.model.{{Domain}}Response;
import {{rootPackage}}.repository.projection.{{Domain}}ItemProjection;
import {{rootPackage}}.repository.projection.{{Domain}}Projection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Projection → DTO on the way in, DTO → API model on the way out. The only class that knows both sides.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface I{{Domain}}Mapper {

    /**
     * @param projection detail projection
     * @return dto
     */
    {{Domain}}DTO toDto({{Domain}}Projection projection);

    /**
     * @param projection list row
     * @return dto without the detail-only fields
     */
    {{Domain}}DTO toItemDto({{Domain}}ItemProjection projection);

    /**
     * @param projections list rows
     * @return dtos
     */
    List<{{Domain}}DTO> toItemDtoList(List<{{Domain}}ItemProjection> projections);

    /**
     * @param dto dto
     * @return detail response; {@code entityId} is exposed as {@code id}
     */
    @Mapping(target = "id", source = "entityId")
    {{Domain}}Response toResponse({{Domain}}DTO dto);

    /**
     * @param dto dto
     * @return list item; {@code entityId} is exposed as {@code id}
     */
    @Mapping(target = "id", source = "entityId")
    {{Domain}}ItemResponse toItemResponse({{Domain}}DTO dto);

    /**
     * @param dtos dtos
     * @return list items
     */
    List<{{Domain}}ItemResponse> toItemResponseList(List<{{Domain}}DTO> dtos);
}

package {{rootPackage}}.modules.{{downstream_package}}.{{domain_package}}.model;

import {{rootPackage}}.annotations.Trimmed;
import {{rootPackage}}.annotations.UppercaseList;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Filters of {@code GET} (the list), bound from query parameters:
 * {@code ?ids=A,B&status=1&textSearch=abc&page=0&size=50&sort=name,desc}. Every field is optional.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class {{Domain}}ListRequest {

    @Schema(description = "Public ids to include")
    @UppercaseList
    private List<String> ids;

    @Schema(description = "0 inactive, 1 active")
    private Short status;

    @Schema(description = "Case-insensitive text contained in the name or the description")
    @Trimmed
    private String textSearch;
}

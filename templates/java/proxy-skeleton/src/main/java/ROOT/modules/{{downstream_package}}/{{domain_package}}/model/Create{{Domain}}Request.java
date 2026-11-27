package {{rootPackage}}.modules.{{downstream_package}}.{{domain_package}}.model;

import {{rootPackage}}.annotations.Trimmed;
import {{rootPackage}}.constants.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body of {@code POST}. A mutable class, not a record, because {@code CustomFieldProcessor} normalizes
 * the annotated fields in place.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Create{{Domain}}Request {

    @Schema(example = "First {{domain}}")
    @NotBlank(message = "Name is required.")
    @Size(max = Constants.MAX_NAME_LENGTH, message = "Name is too long.")
    @Trimmed
    private String name;

    @Schema(example = "What it is for")
    @Size(max = Constants.MAX_DESCRIPTION_LENGTH, message = "Description is too long.")
    @Trimmed
    private String description;
}

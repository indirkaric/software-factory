package {{rootPackage}}.modules.{{downstream_package}}.{{domain_package}}.model;

import {{rootPackage}}.annotations.Trimmed;
import {{rootPackage}}.constants.Constants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body of {@code PUT /{id}}: the full new state.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Modify{{Domain}}Request {

    @Schema(example = "Renamed {{domain}}")
    @NotBlank(message = "Name is required.")
    @Size(max = Constants.MAX_NAME_LENGTH, message = "Name is too long.")
    @Trimmed
    private String name;

    @Size(max = Constants.MAX_DESCRIPTION_LENGTH, message = "Description is too long.")
    @Trimmed
    private String description;

    @Schema(description = "0 inactive, 1 active", example = "1")
    @NotNull(message = "Status is required.")
    @Min(value = Constants.Status.INACTIVE, message = "Status must be 0 or 1.")
    @Max(value = Constants.Status.ACTIVE, message = "Status must be 0 or 1.")
    private Short status;
}

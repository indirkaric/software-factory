package {{rootPackage}}.modules.{{domain_package}};

import {{rootPackage}}.constants.Constants;
import {{rootPackage}}.dto.response.EntityCreatedResponse;
import {{rootPackage}}.exception.response.BusinessExceptionResponse;
import {{rootPackage}}.exception.response.RestApiResponse;
import {{rootPackage}}.modules.{{domain_package}}.model.Create{{Domain}}Request;
import {{rootPackage}}.modules.{{domain_package}}.model.Modify{{Domain}}Request;
import {{rootPackage}}.modules.{{domain_package}}.model.{{Domain}}ListRequest;
import {{rootPackage}}.modules.{{domain_package}}.model.{{Domain}}ListResponse;
import {{rootPackage}}.modules.{{domain_package}}.model.{{Domain}}Response;
import {{rootPackage}}.modules.{{domain_package}}.service.I{{Domain}}ListResponseCreatorService;
import {{rootPackage}}.modules.{{domain_package}}.service.I{{Domain}}ResponseCreatorService;
import {{rootPackage}}.service.{{domain_package}}.ICreate{{Domain}}Service;
import {{rootPackage}}.service.{{domain_package}}.IDelete{{Domain}}Service;
import {{rootPackage}}.service.{{domain_package}}.IModify{{Domain}}Service;
import {{rootPackage}}.util.Check;
import {{rootPackage}}.util.CustomFieldProcessor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

/**
 * {{Domain}} API, the factory's reference module. Every endpoint: Check its parameters, normalize the
 * request model, delegate to exactly one service, wrap the result in a ResponseEntity. Reads go through
 * a response creator; a write that returns only an id or nothing calls its operation service directly.
 * No method is {@code final}: {@code @PreAuthorize} works through a CGLIB proxy that overrides them.
 */
@RestController
@RequestMapping(path = {"/{{api-prefix}}/v1/{{domain-path}}"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "{{Domain}}", description = "{{Domain}} create, read, update and delete.")
public class {{Domain}}Controller {

    private static final String NOT_FOUND = "{{Domain}} not found.";

    private final I{{Domain}}ListResponseCreatorService listResponseCreator;

    private final I{{Domain}}ResponseCreatorService responseCreator;

    private final ICreate{{Domain}}Service createService;

    private final IModify{{Domain}}Service modifyService;

    private final IDelete{{Domain}}Service deleteService;

    /**
     * Constructor with dependency injection.
     *
     * @param listResponseCreator list response creator
     * @param responseCreator detail response creator
     * @param createService create service
     * @param modifyService modify service
     * @param deleteService delete service
     */
    public {{Domain}}Controller(final I{{Domain}}ListResponseCreatorService listResponseCreator,
                                final I{{Domain}}ResponseCreatorService responseCreator,
                                final ICreate{{Domain}}Service createService,
                                final IModify{{Domain}}Service modifyService,
                                final IDelete{{Domain}}Service deleteService) {
        Check.isNotNull(listResponseCreator, "listResponseCreator");
        Check.isNotNull(responseCreator, "responseCreator");
        Check.isNotNull(createService, "createService");
        Check.isNotNull(modifyService, "modifyService");
        Check.isNotNull(deleteService, "deleteService");

        this.listResponseCreator = listResponseCreator;
        this.responseCreator = responseCreator;
        this.createService = createService;
        this.modifyService = modifyService;
        this.deleteService = deleteService;
    }

    /**
     * Designed for extension.
     *
     * @param request filters from the query string
     * @param pageable {@code page}, {@code size}, {@code sort} from the query string
     * @return 200 with one page, or 204 when the page is empty
     */
    @Operation(summary = "List {{domain}}s, filtered, paged and sorted.",
            security = {@SecurityRequirement(name = Constants.BEARER_KEY_VALUE)})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "One page.", content = @Content(
                    schema = @Schema(implementation = {{Domain}}ListResponse.class))),
            @ApiResponse(responseCode = "204", description = "Nothing matches.", content = @Content),
            @ApiResponse(responseCode = "400", description = "Unsupported sort property.", content = @Content(
                    schema = @Schema(implementation = RestApiResponse.class)))})
    @GetMapping
    @PreAuthorize(Constants.Authority.RESTRICTED_AUTHORITIES)
    public ResponseEntity<{{Domain}}ListResponse> getList(
            @ParameterObject final {{Domain}}ListRequest request,
            @ParameterObject @PageableDefault(size = Constants.DEFAULT_PAGE_SIZE) final Pageable pageable) {
        Check.isNotNull(request, "request");
        Check.isNotNull(pageable, "pageable");

        CustomFieldProcessor.processAnnotatedFields(request);

        {{Domain}}ListResponse response = listResponseCreator.create(request, pageable);

        return response.data().isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
    }

    /**
     * Designed for extension.
     *
     * @param id public id
     * @return 200 with the {{domain}}
     */
    @Operation(summary = "Get one {{domain}}.",
            security = {@SecurityRequirement(name = Constants.BEARER_KEY_VALUE)})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The {{domain}}."),
            @ApiResponse(responseCode = "404", description = NOT_FOUND, content = @Content(
                    schema = @Schema(implementation = RestApiResponse.class)))})
    @GetMapping("/{id}")
    @PreAuthorize(Constants.Authority.RESTRICTED_AUTHORITIES)
    public ResponseEntity<{{Domain}}Response> get(@PathVariable("id") final String id) {
        Check.isNotEmpty(id, "id");

        return ResponseEntity.ok(responseCreator.create(id.toUpperCase(Locale.ROOT)));
    }

    /**
     * Designed for extension.
     *
     * @param request new {{domain}}
     * @return 201 with the new public id
     */
    @Operation(summary = "Create a {{domain}}.",
            security = {@SecurityRequirement(name = Constants.BEARER_KEY_VALUE)})
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created."),
            @ApiResponse(responseCode = "400", description = "Invalid field.", content = @Content(
                    schema = @Schema(implementation = BusinessExceptionResponse.class))),
            @ApiResponse(responseCode = "422", description = "Name already taken.", content = @Content(
                    schema = @Schema(implementation = BusinessExceptionResponse.class)))})
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(Constants.Authority.FULL_AUTHORITIES)
    public ResponseEntity<EntityCreatedResponse> create(
            @Valid @RequestBody final Create{{Domain}}Request request) {
        Check.isNotNull(request, "request");

        CustomFieldProcessor.processAnnotatedFields(request);
        String id = createService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(new EntityCreatedResponse(id));
    }

    /**
     * Designed for extension.
     *
     * @param id public id
     * @param request the full new state
     * @return 204
     */
    @Operation(summary = "Modify a {{domain}}.",
            security = {@SecurityRequirement(name = Constants.BEARER_KEY_VALUE)})
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Modified.", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid field.", content = @Content(
                    schema = @Schema(implementation = BusinessExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = NOT_FOUND, content = @Content(
                    schema = @Schema(implementation = RestApiResponse.class))),
            @ApiResponse(responseCode = "422", description = "Name already taken.", content = @Content(
                    schema = @Schema(implementation = BusinessExceptionResponse.class)))})
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(Constants.Authority.FULL_AUTHORITIES)
    public ResponseEntity<Void> modify(@PathVariable("id") final String id,
                                       @Valid @RequestBody final Modify{{Domain}}Request request) {
        Check.isNotEmpty(id, "id");
        Check.isNotNull(request, "request");

        CustomFieldProcessor.processAnnotatedFields(request);
        modifyService.modify(id.toUpperCase(Locale.ROOT), request);

        return ResponseEntity.noContent().build();
    }

    /**
     * Designed for extension.
     *
     * @param id public id
     * @return 204
     */
    @Operation(summary = "Delete an inactive {{domain}}.",
            security = {@SecurityRequirement(name = Constants.BEARER_KEY_VALUE)})
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted.", content = @Content),
            @ApiResponse(responseCode = "404", description = NOT_FOUND, content = @Content(
                    schema = @Schema(implementation = RestApiResponse.class))),
            @ApiResponse(responseCode = "422", description = "Still active.", content = @Content(
                    schema = @Schema(implementation = BusinessExceptionResponse.class)))})
    @DeleteMapping("/{id}")
    @PreAuthorize(Constants.Authority.FULL_AUTHORITIES)
    public ResponseEntity<Void> delete(@PathVariable("id") final String id) {
        Check.isNotEmpty(id, "id");

        deleteService.delete(id.toUpperCase(Locale.ROOT));

        return ResponseEntity.noContent().build();
    }
}

package {{rootPackage}}.modules.{{downstream_package}}.{{domain_package}}.model;

import java.util.Date;

/**
 * Body of {@code GET /{id}}.
 *
 * @param id public id
 * @param name name
 * @param description description
 * @param status 0 inactive, 1 active
 * @param createdBy who created it
 * @param createdUtc when it was created
 */
public record {{Domain}}Response(String id, String name, String description, Short status,
                                 String createdBy, Date createdUtc) {
}

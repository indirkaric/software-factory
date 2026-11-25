package {{rootPackage}}.repository.projection;

import java.util.Date;

/**
 * One {{domain}} for the detail read.
 *
 * @param entityId public id
 * @param name name
 * @param description description
 * @param status status, see {@code Constants.Status}
 * @param createdBy who created it
 * @param createdUtc when it was created
 */
public record {{Domain}}Projection(String entityId, String name, String description, Short status,
                                   String createdBy, Date createdUtc) {
}

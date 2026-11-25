package {{rootPackage}}.dto;

import java.util.Date;

/**
 * Internal {{domain}} data between the read service and the response creators; never returned by a
 * controller. List rows leave the detail-only fields null.
 *
 * @param entityId public id
 * @param name name
 * @param description description
 * @param status status, see {@code Constants.Status}
 * @param createdBy who created it
 * @param createdUtc when it was created
 */
public record {{Domain}}DTO(String entityId, String name, String description, Short status,
                            String createdBy, Date createdUtc) {
}

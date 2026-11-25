package {{rootPackage}}.repository.projection;

/**
 * One row of the {{domain}} list: only the columns the list shows. Built by the Criteria query with
 * {@code criteriaBuilder.construct}, so the component order and types match the select exactly.
 *
 * @param entityId public id
 * @param name name
 * @param status status, see {@code Constants.Status}
 */
public record {{Domain}}ItemProjection(String entityId, String name, Short status) {
}

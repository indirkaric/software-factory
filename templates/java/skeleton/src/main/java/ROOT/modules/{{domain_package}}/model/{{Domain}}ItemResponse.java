package {{rootPackage}}.modules.{{domain_package}}.model;

/**
 * One row of the list.
 *
 * @param id public id
 * @param name name
 * @param status 0 inactive, 1 active
 */
public record {{Domain}}ItemResponse(String id, String name, Short status) {
}

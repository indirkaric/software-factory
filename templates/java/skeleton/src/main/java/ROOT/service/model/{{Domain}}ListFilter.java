package {{rootPackage}}.service.model;

import java.util.List;

/**
 * What the {{domain}} list is filtered by. Built by the list response creator from the API's
 * {@code {{Domain}}ListRequest}, so the repository never depends on an API model. Every field is optional.
 *
 * @param ids public ids to include
 * @param status status to match
 * @param textSearch case-insensitive text contained in the name or the description
 */
public record {{Domain}}ListFilter(List<String> ids, Short status, String textSearch) {
}

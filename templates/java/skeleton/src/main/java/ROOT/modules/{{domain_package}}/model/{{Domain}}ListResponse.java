package {{rootPackage}}.modules.{{domain_package}}.model;

import java.util.List;

/**
 * Body of {@code GET} (the list): one page and the total count. The controller answers 204 instead
 * when the page is empty.
 *
 * @param data rows of this page
 * @param count total rows matching the filter
 */
public record {{Domain}}ListResponse(List<{{Domain}}ItemResponse> data, long count) {
}

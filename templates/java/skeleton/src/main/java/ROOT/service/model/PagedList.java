package {{rootPackage}}.service.model;

import {{rootPackage}}.util.Check;

import java.util.List;

/**
 * One page of a list plus the total count over all pages. What a read service returns for a list;
 * the response creator turns it into the module's {@code <Domain>ListResponse(data, count)}.
 *
 * @param items items of this page
 * @param count total number of items matching the filter
 * @param <T> item type
 */
public record PagedList<T>(List<T> items, long count) {

    /**
     * @param items items of this page
     * @param count total number of items matching the filter
     */
    public PagedList {
        Check.isNotNull(items, "items");
        items = List.copyOf(items);
    }
}

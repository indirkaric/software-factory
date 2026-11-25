package {{rootPackage}}.util;

import {{rootPackage}}.constants.Constants;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Turns a {@link Pageable}'s sort into Criteria API orders.
 */
public final class SortHelper {

    /** Escape character for {@link #containsPattern(String)}. */
    public static final char LIKE_ESCAPE = '\\';

    private static final String ID = "id";

    private SortHelper() {
    }

    /**
     * Orders for a paged Criteria query. {@code id} means {@code entityId}: the database id is never
     * exposed. Only {@code sortable} properties are accepted, so a client can't sort by any column it
     * guesses; anything else is a 400. {@code entityId} is always the last order, so rows with equal
     * values keep a stable order across pages.
     *
     * @param criteriaBuilder criteria builder
     * @param root query root
     * @param pageable page request with an optional sort, e.g. {@code ?sort=name,desc}
     * @param sortable entity attribute names a client may sort by (use the metamodel constants)
     * @return orders, never empty
     */
    public static List<Order> getOrders(final CriteriaBuilder criteriaBuilder, final Root<?> root,
                                        final Pageable pageable, final Set<String> sortable) {
        Check.isNotNull(criteriaBuilder, "criteriaBuilder");
        Check.isNotNull(root, "root");
        Check.isNotNull(pageable, "pageable");
        Check.isNotNull(sortable, "sortable");

        List<Order> orders = new ArrayList<>();
        boolean byEntityId = false;
        for (Sort.Order order : pageable.getSort()) {
            String property = ID.equalsIgnoreCase(order.getProperty()) ? Constants.ENTITY_ID : order.getProperty();
            if (!Constants.ENTITY_ID.equals(property) && !sortable.contains(property)) {
                throw new IllegalArgumentException("Sorting by '" + order.getProperty() + "' is not supported.");
            }

            Path<?> path = root.get(property);
            orders.add(order.isAscending() ? criteriaBuilder.asc(path) : criteriaBuilder.desc(path));
            byEntityId = byEntityId || Constants.ENTITY_ID.equals(property);
        }

        if (!byEntityId) {
            orders.add(criteriaBuilder.asc(root.get(Constants.ENTITY_ID)));
        }

        return orders;
    }

    /**
     * Escapes {@code %}, {@code _} and {@code \} so user text is matched literally by a LIKE with
     * {@link #LIKE_ESCAPE}.
     *
     * @param text user text
     * @return {@code %text%}, lower-cased and escaped
     */
    public static String containsPattern(final String text) {
        Check.isNotNull(text, "text");

        String escaped = text.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}

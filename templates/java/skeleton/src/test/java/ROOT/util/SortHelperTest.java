package {{rootPackage}}.util;

import {{rootPackage}}.constants.Constants;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SortHelperTest {

    private static final Set<String> SORTABLE = Set.of("name");

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private Root<Object> root;

    @Mock
    private Path<Object> namePath;

    @Mock
    private Path<Object> entityIdPath;

    private final Order nameDesc = mock(Order.class);

    private final Order entityIdAsc = mock(Order.class);

    @Test
    void shouldSortByEntityIdWhenUnsorted() {
        stubEntityIdOrder();
        List<Order> orders = SortHelper.getOrders(criteriaBuilder, root, PageRequest.of(0, 1), SORTABLE);

        assertThat(orders).containsExactly(entityIdAsc);
    }

    @Test
    void shouldAppendEntityIdAfterRequestedSort() {
        stubEntityIdOrder();
        when(root.get("name")).thenReturn(namePath);
        when(criteriaBuilder.desc(namePath)).thenReturn(nameDesc);

        List<Order> orders = SortHelper.getOrders(criteriaBuilder, root,
                PageRequest.of(0, 1, Sort.by(Sort.Order.desc("name"))), SORTABLE);

        assertThat(orders).containsExactly(nameDesc, entityIdAsc);
    }

    @Test
    void shouldMapIdToEntityIdOnce() {
        stubEntityIdOrder();
        List<Order> orders = SortHelper.getOrders(criteriaBuilder, root,
                PageRequest.of(0, 1, Sort.by("id")), SORTABLE);

        assertThat(orders).containsExactly(entityIdAsc);
    }

    @Test
    void shouldRejectUnsupportedProperty() {
        PageRequest pageable = PageRequest.of(0, 1, Sort.by("password"));

        assertThatThrownBy(() -> SortHelper.getOrders(criteriaBuilder, root, pageable, SORTABLE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("password");
    }

    @Test
    void shouldEscapeLikeWildcards() {
        assertThat(SortHelper.containsPattern("A%b_c\\")).isEqualTo("%a\\%b\\_c\\\\%");
    }

    private void stubEntityIdOrder() {
        when(root.get(Constants.ENTITY_ID)).thenReturn(entityIdPath);
        when(criteriaBuilder.asc(entityIdPath)).thenReturn(entityIdAsc);
    }
}

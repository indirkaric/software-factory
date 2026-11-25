package {{rootPackage}}.service.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PagedListTest {

    @Test
    void shouldKeepAnImmutableCopy() {
        List<String> items = new ArrayList<>(List.of("a"));

        PagedList<String> paged = new PagedList<>(items, 2);
        items.add("b");

        assertThat(paged.items()).containsExactly("a");
        assertThat(paged.count()).isEqualTo(2);
    }

    @Test
    void shouldRejectNullItems() {
        assertThatThrownBy(() -> new PagedList<>(null, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}

package {{rootPackage}}.util;

import {{rootPackage}}.annotations.Lowercase;
import {{rootPackage}}.annotations.Trimmed;
import {{rootPackage}}.annotations.Uppercase;
import {{rootPackage}}.annotations.UppercaseList;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomFieldProcessorTest {

    @Test
    void shouldNormalizeAnnotatedFieldsIncludingInherited() {
        Sample sample = new Sample();

        CustomFieldProcessor.processAnnotatedFields(sample);

        assertThat(sample.trimmed).isEqualTo("a b");
        assertThat(sample.upper).isEqualTo("ABC");
        assertThat(sample.lower).isEqualTo("abc");
        assertThat(sample.ids).containsExactly("X1", null);
        assertThat(sample.plain).isEqualTo(" keep ");
        assertThat(((Parent) sample).inherited).isEqualTo("x");
    }

    @Test
    void shouldLeaveNullsAndOtherTypesAlone() {
        Sample sample = new Sample();
        sample.trimmed = null;
        sample.ids = null;

        CustomFieldProcessor.processAnnotatedFields(sample);

        assertThat(sample.trimmed).isNull();
        assertThat(sample.ids).isNull();
    }

    @Test
    void shouldRejectNull() {
        assertThatThrownBy(() -> CustomFieldProcessor.processAnnotatedFields(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    static class Parent {

        @Trimmed
        private String inherited = "  x ";
    }

    static class Sample extends Parent {

        @Trimmed
        private String trimmed = "  a    b ";

        @Uppercase
        private String upper = "abc";

        @Lowercase
        private String lower = "ABC";

        @UppercaseList
        private List<String> ids = Arrays.asList(" x1 ", null);

        private String plain = " keep ";
    }
}

package com.psytrack.unformulieren.domain.model;

import com.psytrack.unformulieren.domain.enums.QuestionType;
import com.psytrack.unformulieren.domain.exception.InvalidQuestionOrderException;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionTest {

    private final UUID formId = UUID.randomUUID();

    @Test
    void constructor_setsAllFields() {
        Question q = new Question(formId, "How do you feel?", QuestionType.SCALE, true, Map.of("min", "1", "max", "5"), 1);

        assertThat(q.getId()).isNotNull();
        assertThat(q.getFormId()).isEqualTo(formId);
        assertThat(q.getText()).isEqualTo("How do you feel?");
        assertThat(q.getType()).isEqualTo(QuestionType.SCALE);
        assertThat(q.isRequired()).isTrue();
        assertThat(q.getConfig()).containsEntry("min", "1").containsEntry("max", "5");
        assertThat(q.getOrder()).isEqualTo(1);
    }

    @Test
    void constructor_rejectsBlankText() {
        assertThatThrownBy(() -> new Question(formId, "  ", QuestionType.TEXT, false, Map.of(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("text");
    }

    @Test
    void constructor_rejectsNullType() {
        assertThatThrownBy(() -> new Question(formId, "Some question?", null, false, Map.of(), 0))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_rejectsNegativeOrder() {
        assertThatThrownBy(() -> new Question(formId, "Question?", QuestionType.TEXT, false, Map.of(), -1))
                .isInstanceOf(InvalidQuestionOrderException.class);
    }

    @Test
    void reconstitute_rebuildsQuestion() {
        UUID id = UUID.randomUUID();
        Question q = Question.reconstitute(id, formId, "Text?", QuestionType.BOOLEAN, false, Map.of(), 3);

        assertThat(q.getId()).isEqualTo(id);
        assertThat(q.getOrder()).isEqualTo(3);
        assertThat(q.getType()).isEqualTo(QuestionType.BOOLEAN);
    }
}

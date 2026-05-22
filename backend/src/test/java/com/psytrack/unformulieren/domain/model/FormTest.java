package com.psytrack.unformulieren.domain.model;

import com.psytrack.unformulieren.domain.enums.Status;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormTest {

    @Test
    void constructor_setsFields() {
        Form form = new Form("Health Check", "Monthly survey", Status.ACTIVE);

        assertThat(form.getId()).isNotNull();
        assertThat(form.getTitle()).isEqualTo("Health Check");
        assertThat(form.getDescription()).isEqualTo("Monthly survey");
        assertThat(form.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(form.getQuestions()).isEmpty();
    }

    @Test
    void constructor_trimsDescription() {
        Form form = new Form("Title", "  desc  ", Status.CREATED);
        assertThat(form.getDescription()).isEqualTo("desc");
    }

    @Test
    void constructor_defaultsDescriptionToEmpty() {
        Form form = new Form("Title", null, Status.CREATED);
        assertThat(form.getDescription()).isEmpty();
    }

    @Test
    void constructor_rejectsBlankTitle() {
        assertThatThrownBy(() -> new Form("  ", "desc", Status.ACTIVE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }

    @Test
    void constructor_rejectsNullStatus() {
        assertThatThrownBy(() -> new Form("Title", "desc", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void addQuestion_appendsQuestion() {
        Form form = new Form("Form1", "desc", Status.ACTIVE);
        UUID formId = form.getId();
        Question q = new Question(formId, "How are you?",
                com.psytrack.unformulieren.domain.enums.QuestionType.SCALE, true, java.util.Map.of(), 0);

        form.addQuestion(q);

        assertThat(form.getQuestions()).hasSize(1).containsExactly(q);
    }

    @Test
    void addQuestion_rejectsNull() {
        Form form = new Form("Form1", "desc", Status.ACTIVE);
        assertThatThrownBy(() -> form.addQuestion(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void questions_returnsUnmodifiableList() {
        Form form = new Form("Form1", "desc", Status.ACTIVE);
        assertThatThrownBy(() -> form.getQuestions().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void reconstitute_preservesId() {
        UUID id = UUID.randomUUID();
        Form form = Form.reconstitute(id, "Title", "desc", Status.ENDED);
        assertThat(form.getId()).isEqualTo(id);
        assertThat(form.getStatus()).isEqualTo(Status.ENDED);
    }
}

package com.psytrack.unformulieren.application.service;

import com.psytrack.unformulieren.application.port.out.QuestionRepositoryPort;
import com.psytrack.unformulieren.domain.enums.QuestionType;
import com.psytrack.unformulieren.domain.exception.QuestionNotFoundException;
import com.psytrack.unformulieren.domain.model.Question;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock private QuestionRepositoryPort questionRepositoryPort;

    @InjectMocks private QuestionService questionService;

    private final UUID formId = UUID.randomUUID();

    private Question sampleQuestion() {
        return new Question(formId, "How are you?", QuestionType.SCALE, true, Map.of(), 0);
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    void create_savesAndReturnsQuestion() {
        Question saved = sampleQuestion();
        given(questionRepositoryPort.save(any(Question.class))).willReturn(saved);

        Question result = questionService.create(formId, "How are you?", QuestionType.SCALE, true, Map.of(), 0);

        assertThat(result).isNotNull();
        assertThat(result.getText()).isEqualTo("How are you?");
        verify(questionRepositoryPort).save(any(Question.class));
    }

    // ── get ──────────────────────────────────────────────────────────────────

    @Test
    void get_returnsQuestion_whenFound() {
        Question question = sampleQuestion();
        given(questionRepositoryPort.findById(question.getId())).willReturn(Optional.of(question));

        Question result = questionService.get(question.getId());

        assertThat(result).isEqualTo(question);
    }

    @Test
    void get_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        given(questionRepositoryPort.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.get(id))
                .isInstanceOf(QuestionNotFoundException.class);
    }

    // ── list ─────────────────────────────────────────────────────────────────

    @Test
    void list_returnsAllQuestions() {
        List<Question> questions = List.of(sampleQuestion(),
                new Question(formId, "Rate your stress?", QuestionType.LIKERT, false, Map.of(), 1));
        given(questionRepositoryPort.findAll()).willReturn(questions);

        List<Question> result = questionService.list();

        assertThat(result).hasSize(2);
    }

    // ── findByType ───────────────────────────────────────────────────────────

    @Test
    void findByType_returnsQuestionsMatchingType() {
        List<Question> scales = List.of(sampleQuestion());
        given(questionRepositoryPort.findByType(QuestionType.SCALE)).willReturn(scales);

        List<Question> result = questionService.findByType(QuestionType.SCALE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(QuestionType.SCALE);
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    void update_savesReconstitutedQuestion() {
        Question existing = sampleQuestion();
        UUID newFormId = UUID.randomUUID();
        given(questionRepositoryPort.findById(existing.getId())).willReturn(Optional.of(existing));
        given(questionRepositoryPort.save(any(Question.class))).willAnswer(inv -> inv.getArgument(0));

        Question updated = questionService.update(
                existing.getId(), newFormId, "Updated text?", QuestionType.TEXT, false, Map.of(), 2);

        assertThat(updated.getText()).isEqualTo("Updated text?");
        assertThat(updated.getType()).isEqualTo(QuestionType.TEXT);
        assertThat(updated.getOrder()).isEqualTo(2);
    }

    @Test
    void update_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        given(questionRepositoryPort.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.update(id, formId, "Text?", QuestionType.TEXT, false, Map.of(), 0))
                .isInstanceOf(QuestionNotFoundException.class);
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_succeeds_whenQuestionExists() {
        Question question = sampleQuestion();
        given(questionRepositoryPort.findById(question.getId())).willReturn(Optional.of(question));

        questionService.delete(question.getId());

        verify(questionRepositoryPort).deleteById(question.getId());
    }

    @Test
    void delete_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        given(questionRepositoryPort.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.delete(id))
                .isInstanceOf(QuestionNotFoundException.class);
    }
}

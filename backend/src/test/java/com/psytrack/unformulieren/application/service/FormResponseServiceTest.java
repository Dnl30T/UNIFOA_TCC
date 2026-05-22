package com.psytrack.unformulieren.application.service;

import com.psytrack.unformulieren.application.port.out.FormResponseRepositoryPort;
import com.psytrack.unformulieren.domain.exception.FormResponseNotFoundException;
import com.psytrack.unformulieren.domain.model.FormResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FormResponseServiceTest {

    @Mock private FormResponseRepositoryPort formResponseRepositoryPort;

    @InjectMocks private FormResponseService formResponseService;

    private final UUID formId     = UUID.randomUUID();
    private final UUID questionId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final Instant now     = Instant.now();

    private FormResponse sampleResponse() {
        return new FormResponse(formId, questionId, employeeId, 4, now);
    }

    // ── submitResponse ───────────────────────────────────────────────────────

    @Test
    void submitResponse_savesAndReturnsResponse() {
        FormResponse saved = sampleResponse();
        given(formResponseRepositoryPort.save(any(FormResponse.class))).willReturn(saved);

        FormResponse result = formResponseService.submitResponse(formId, questionId, employeeId, 4, now);

        assertThat(result.getValue()).isEqualTo(4);
        assertThat(result.getEmployeeId()).isEqualTo(employeeId);
        verify(formResponseRepositoryPort).save(any(FormResponse.class));
    }

    // ── get ──────────────────────────────────────────────────────────────────

    @Test
    void get_returnsResponse_whenFound() {
        FormResponse response = sampleResponse();
        given(formResponseRepositoryPort.findById(response.getId())).willReturn(Optional.of(response));

        assertThat(formResponseService.get(response.getId())).isEqualTo(response);
    }

    @Test
    void get_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        given(formResponseRepositoryPort.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> formResponseService.get(id))
                .isInstanceOf(FormResponseNotFoundException.class);
    }

    // ── findByEmployeeId ─────────────────────────────────────────────────────

    @Test
    void findByEmployeeId_returnsMatchingResponses() {
        List<FormResponse> responses = List.of(sampleResponse());
        given(formResponseRepositoryPort.findByEmployeeId(employeeId)).willReturn(responses);

        assertThat(formResponseService.findByEmployeeId(employeeId)).hasSize(1);
    }

    // ── list ─────────────────────────────────────────────────────────────────

    @Test
    void list_returnsAll() {
        given(formResponseRepositoryPort.findAll()).willReturn(List.of(sampleResponse()));

        assertThat(formResponseService.list()).hasSize(1);
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    void update_savesReconstitutedResponse() {
        FormResponse existing = sampleResponse();
        given(formResponseRepositoryPort.findById(existing.getId())).willReturn(Optional.of(existing));
        given(formResponseRepositoryPort.save(any(FormResponse.class))).willAnswer(inv -> inv.getArgument(0));

        FormResponse updated = formResponseService.update(
                existing.getId(), formId, questionId, employeeId, 5, now);

        assertThat(updated.getValue()).isEqualTo(5);
    }

    @Test
    void update_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        given(formResponseRepositoryPort.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> formResponseService.update(id, formId, questionId, employeeId, 5, now))
                .isInstanceOf(FormResponseNotFoundException.class);
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_succeeds_whenResponseExists() {
        FormResponse response = sampleResponse();
        given(formResponseRepositoryPort.findById(response.getId())).willReturn(Optional.of(response));

        formResponseService.delete(response.getId());

        verify(formResponseRepositoryPort).deleteById(response.getId());
    }

    @Test
    void delete_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        given(formResponseRepositoryPort.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> formResponseService.delete(id))
                .isInstanceOf(FormResponseNotFoundException.class);
    }
}

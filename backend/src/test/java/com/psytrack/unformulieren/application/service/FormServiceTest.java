package com.psytrack.unformulieren.application.service;

import com.psytrack.unformulieren.application.port.out.EmployeeRepositoryPort;
import com.psytrack.unformulieren.application.port.out.FormRepositoryPort;
import com.psytrack.unformulieren.application.port.out.UserRepositoryPort;
import com.psytrack.unformulieren.domain.enums.Status;
import com.psytrack.unformulieren.domain.exception.FormNotFoundException;
import com.psytrack.unformulieren.domain.model.Form;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FormServiceTest {

    @Mock private FormRepositoryPort formRepositoryPort;
    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private EmployeeRepositoryPort employeeRepositoryPort;

    @InjectMocks private FormService formService;

    private Form sampleForm() {
        return new Form("Health Check", "Monthly health survey", Status.ACTIVE, "counselor1");
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    void create_returnsNewForm() {
        Form saved = sampleForm();
        given(formRepositoryPort.save(any(Form.class))).willReturn(saved);

        Form result = formService.create("Health Check", "Monthly health survey", Status.ACTIVE, "counselor1");

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Health Check");
        verify(formRepositoryPort).save(any(Form.class));
    }

    // ── get ──────────────────────────────────────────────────────────────────

    @Test
    void get_returnsForm_whenFound() {
        Form form = sampleForm();
        given(formRepositoryPort.findById(form.getId())).willReturn(Optional.of(form));

        Form result = formService.get(form.getId());

        assertThat(result).isEqualTo(form);
    }

    @Test
    void get_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        given(formRepositoryPort.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> formService.get(id))
                .isInstanceOf(FormNotFoundException.class);
    }

    // ── list ─────────────────────────────────────────────────────────────────

    @Test
    void list_returnsAllForms() {
        List<Form> forms = List.of(sampleForm(), new Form("Stress Test", "desc", Status.CREATED));
        given(formRepositoryPort.findAll()).willReturn(forms);

        List<Form> result = formService.list();

        assertThat(result).hasSize(2);
    }

    // ── findByStatus ─────────────────────────────────────────────────────────

    @Test
    void findByStatus_returnsMatchingForms() {
        List<Form> active = List.of(sampleForm());
        given(formRepositoryPort.findByStatus(Status.ACTIVE)).willReturn(active);

        List<Form> result = formService.findByStatus(Status.ACTIVE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(Status.ACTIVE);
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    void update_succeeds_withNewTitle() {
        Form existing = sampleForm();
        given(formRepositoryPort.findById(existing.getId())).willReturn(Optional.of(existing));
        given(formRepositoryPort.save(any(Form.class))).willAnswer(inv -> inv.getArgument(0));

        Form updated = formService.update(existing.getId(), "New Title", "new desc", Status.ENDED, List.of());

        assertThat(updated.getTitle()).isEqualTo("New Title");
        assertThat(updated.getStatus()).isEqualTo(Status.ENDED);
    }

    @Test
    void update_succeeds_withSameTitle() {
        Form existing = sampleForm();
        given(formRepositoryPort.findById(existing.getId())).willReturn(Optional.of(existing));
        given(formRepositoryPort.save(any(Form.class))).willAnswer(inv -> inv.getArgument(0));

        Form updated = formService.update(existing.getId(), "Health Check", "updated desc", Status.ACTIVE, List.of());

        assertThat(updated.getTitle()).isEqualTo("Health Check");
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_succeeds_whenFormExists() {
        Form form = sampleForm();
        given(formRepositoryPort.findById(form.getId())).willReturn(Optional.of(form));

        formService.delete(form.getId());

        verify(formRepositoryPort).deleteById(form.getId());
    }

    @Test
    void delete_throws_whenFormNotFound() {
        UUID id = UUID.randomUUID();
        given(formRepositoryPort.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> formService.delete(id))
                .isInstanceOf(FormNotFoundException.class);
    }
}

package com.psytrack.unformulieren.application.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.psytrack.unformulieren.application.port.out.EmployeeRepositoryPort;
import com.psytrack.unformulieren.application.port.out.FormResponseRepositoryPort;
import com.psytrack.unformulieren.application.port.out.FormRepositoryPort;
import com.psytrack.unformulieren.application.port.out.UserRepositoryPort;
import com.psytrack.unformulieren.domain.enums.Status;
import com.psytrack.unformulieren.domain.exception.FormNotFoundException;
import com.psytrack.unformulieren.domain.model.Form;
import com.psytrack.unformulieren.domain.model.Question;

@Service
public class FormService {

    private final FormRepositoryPort formRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final EmployeeRepositoryPort employeeRepositoryPort;
    private final FormResponseService formResponseService;
    private final FormResponseRepositoryPort formResponseRepositoryPort;

    public FormService(FormRepositoryPort formRepositoryPort,
                       UserRepositoryPort userRepositoryPort,
                       EmployeeRepositoryPort employeeRepositoryPort,
                       FormResponseService formResponseService,
                       FormResponseRepositoryPort formResponseRepositoryPort) {
        this.formRepositoryPort = formRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.employeeRepositoryPort = employeeRepositoryPort;
        this.formResponseService = formResponseService;
        this.formResponseRepositoryPort = formResponseRepositoryPort;
    }

    public Form create(String title, String description, Status status, String createdBy) {
        return formRepositoryPort.save(new Form(title, description, status, createdBy));
    }

    public Form get(UUID id) {
        return formRepositoryPort.findById(id)
                .orElseThrow(() -> new FormNotFoundException(id));
    }

    /** Returns all forms. For admin use only. */
    public List<Form> list() {
        return formRepositoryPort.findAll();
    }

    /** Returns forms created by the specified counselor. */
    public List<Form> listForCounselor(String username) {
        return formRepositoryPort.findByCreatedBy(username);
    }

    /**
     * Returns ACTIVE forms distributed to the team of the given employee (identified by username).
     * Falls back to all ACTIVE forms if the employee or team cannot be resolved.
     */
    public List<Form> findActiveForEmployee(String username) {
        return userRepositoryPort.findByUsername(username)
                .flatMap(user -> employeeRepositoryPort.findByAppUserId(user.getId()))
                .map(employee -> formRepositoryPort.findByStatus(Status.ACTIVE).stream()
                        .filter(f -> f.getTeamIds().contains(employee.getTeamId()))
                        .toList())
                .orElseGet(() -> formRepositoryPort.findByStatus(Status.ACTIVE));
    }

    public Optional<Form> findByTitle(String title) {
        return formRepositoryPort.findByTitle(title);
    }

    public List<Form> findByStatus(Status status) {
        return formRepositoryPort.findByStatus(status);
    }

    public Form update(UUID id, String title, String description, Status status, List<UUID> teamIds) {
        Form existing = get(id);
        if (formResponseRepositoryPort.existsByFormId(id)) {
            throw new IllegalArgumentException("Form already has submissions and cannot be edited");
        }
        return formRepositoryPort.save(
                Form.reconstitute(id, title, description, status,
                        existing.getCreatedBy(),
                        teamIds != null ? teamIds : existing.getTeamIds(),
                        existing.getQuestions()));
    }

    public Form closeForm(UUID id) {
                Form existing = get(id);
                if (existing.getStatus() == Status.ENDED) {
                    return existing;
                }

                Set<UUID> teamEmployeeIds = existing.getTeamIds().stream()
                    .flatMap(teamId -> employeeRepositoryPort.findByTeamId(teamId).stream())
                    .map(employee -> employee.getId())
                    .collect(Collectors.toSet());

                var closedAt = java.time.Instant.now();
                teamEmployeeIds.stream()
                    .filter(employeeId -> !formResponseService.hasSubmitted(id, employeeId))
                    .forEach(employeeId -> formResponseService.markNoResponse(id, employeeId, closedAt));

                return formRepositoryPort.save(
                    Form.reconstitute(
                        existing.getId(),
                        existing.getTitle(),
                        existing.getDescription(),
                        Status.ENDED,
                        existing.getCreatedBy(),
                        existing.getTeamIds(),
                        existing.getQuestions()));
    }

    public Form duplicate(UUID id, String newTitle, String createdBy) {
        Form existing = get(id);
        UUID newFormId = java.util.UUID.randomUUID();
        List<Question> duplicatedQuestions = existing.getQuestions().stream()
            .map(q -> new Question(newFormId, q.getText(), q.getType(), q.isRequired(), q.getConfig(), q.getOrder()))
            .collect(Collectors.toList());
        return formRepositoryPort.save(
            Form.reconstitute(newFormId, newTitle, existing.getDescription(),
                Status.CREATED, createdBy, List.of(), duplicatedQuestions));
    }

    public void delete(UUID id) {
        get(id);
        formRepositoryPort.deleteById(id);
    }
}

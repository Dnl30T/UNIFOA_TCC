package com.psytrack.unformulieren.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.psytrack.unformulieren.domain.model.TherapistEvaluation;

public interface TherapistEvaluationRepositoryPort {

    TherapistEvaluation save(TherapistEvaluation evaluation);

    Optional<TherapistEvaluation> findByFormIdAndEmployeeId(UUID formId, UUID employeeId);

    List<TherapistEvaluation> findAllByFormId(UUID formId);

    List<TherapistEvaluation> findPublishedByFormId(UUID formId);

    boolean existsByFormIdAndEmployeeId(UUID formId, UUID employeeId);

    void deleteByFormIdAndEmployeeId(UUID formId, UUID employeeId);
}

package com.psytrack.unformulieren.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.psytrack.unformulieren.domain.model.Report;

public interface ReportRepositoryPort {

    Report save(Report report);

    Optional<Report> findById(UUID teamId, UUID id);

    List<Report> findByTeamId(UUID teamId);

    List<Report> findByFormId(UUID formId);
}

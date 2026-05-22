package com.psytrack.unformulieren.adapter.out.persistence.report.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Repository;

import com.psytrack.unformulieren.adapter.out.persistence.report.entity.ReportPrimaryKey;
import com.psytrack.unformulieren.adapter.out.persistence.report.mapper.ReportMapper;
import com.psytrack.unformulieren.adapter.out.persistence.report.repository.ReportCassandraRepository;
import com.psytrack.unformulieren.application.port.out.ReportRepositoryPort;
import com.psytrack.unformulieren.domain.model.Report;

@Repository
public class ReportPersistenceAdapter implements ReportRepositoryPort {

    private final ReportCassandraRepository repository;
    private final ReportMapper mapper = new ReportMapper();

    public ReportPersistenceAdapter(ReportCassandraRepository repository) {
        this.repository = repository;
    }

    @Override
    public Report save(Report report) {
        return mapper.toDomain(repository.save(mapper.toEntity(report)));
    }

    @Override
    public Optional<Report> findById(UUID teamId, UUID id) {
        return repository.findById(new ReportPrimaryKey(teamId, id)).map(mapper::toDomain);
    }

    @Override
    public List<Report> findByTeamId(UUID teamId) {
        return repository.findByKeyTeamId(teamId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Report> findByFormId(UUID formId) {
        return StreamSupport.stream(repository.findAll().spliterator(), false)
                .filter(e -> formId.equals(e.getFormId()))
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}

package com.psytrack.unformulieren.adapter.out.persistence.therapistEvaluation.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.stereotype.Repository;
import com.psytrack.unformulieren.adapter.out.persistence.therapistEvaluation.entity.TherapistEvaluationPrimaryKey;
import com.psytrack.unformulieren.adapter.out.persistence.therapistEvaluation.mapper.TherapistEvaluationMapper;
import com.psytrack.unformulieren.adapter.out.persistence.therapistEvaluation.repository.TherapistEvaluationCassandraRepository;
import com.psytrack.unformulieren.application.port.out.TherapistEvaluationRepositoryPort;
import com.psytrack.unformulieren.domain.model.TherapistEvaluation;

@Repository
public class TherapistEvaluationPersistenceAdapter implements TherapistEvaluationRepositoryPort {

    private final TherapistEvaluationCassandraRepository repository;
    private final TherapistEvaluationMapper mapper = new TherapistEvaluationMapper();

    public TherapistEvaluationPersistenceAdapter(TherapistEvaluationCassandraRepository repository) {
        this.repository = repository;
    }

    @Override
    public TherapistEvaluation save(TherapistEvaluation evaluation) {
        return mapper.toDomain(repository.save(mapper.toEntity(evaluation)));
    }

    @Override
    public Optional<TherapistEvaluation> findByFormIdAndEmployeeId(UUID formId, UUID employeeId) {
        return repository.findByKeyFormIdAndKeyEmployeeId(formId, employeeId).map(mapper::toDomain);
    }

    @Override
    public List<TherapistEvaluation> findAllByFormId(UUID formId) {
        return StreamSupport.stream(repository.findAll().spliterator(), false)
                .filter(e -> formId.equals(e.getKey().getFormId()))
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TherapistEvaluation> findPublishedByFormId(UUID formId) {
        return StreamSupport.stream(repository.findAll().spliterator(), false)
                .filter(e -> formId.equals(e.getKey().getFormId()) && 
                           "PUBLISHED".equals(e.getStatus()))
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByFormIdAndEmployeeId(UUID formId, UUID employeeId) {
        return repository.existsById(new TherapistEvaluationPrimaryKey(formId, employeeId));
    }

    @Override
    public void deleteByFormIdAndEmployeeId(UUID formId, UUID employeeId) {
        repository.deleteById(new TherapistEvaluationPrimaryKey(formId, employeeId));
    }
}

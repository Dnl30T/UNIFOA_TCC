package com.psytrack.unformulieren.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.psytrack.unformulieren.application.port.out.EmployeeResultRepositoryPort;
import com.psytrack.unformulieren.domain.enums.RiskLevel;
import com.psytrack.unformulieren.domain.exception.EmployeeResultNotFoundException;
import com.psytrack.unformulieren.domain.model.EmployeeResult;

@Service
public class EmployeeResultService {

    private final EmployeeResultRepositoryPort employeeResultRepositoryPort;

    public EmployeeResultService(EmployeeResultRepositoryPort employeeResultRepositoryPort) {
        this.employeeResultRepositoryPort = employeeResultRepositoryPort;
    }

    public EmployeeResult create(UUID employeeId, UUID formId, int score, RiskLevel riskLevel, Instant calculatedAt) {
        return employeeResultRepositoryPort.save(new EmployeeResult(employeeId, formId, score, null, riskLevel, calculatedAt));
    }

        public EmployeeResult createOrUpdateHelperScore(UUID employeeId, UUID formId, int helperScore, Instant calculatedAt) {
        RiskLevel riskLevel = classifyBurnoutRisk(helperScore);
        return employeeResultRepositoryPort.findByEmployeeIdAndFormId(employeeId, formId)
                .map(existing -> EmployeeResult.reconstitute(
                        existing.getId(),
                        employeeId,
                        formId,
                helperScore,
                existing.getFinalScore(),
                        riskLevel,
                        calculatedAt))
                .map(employeeResultRepositoryPort::save)
            .orElseGet(() -> create(employeeId, formId, helperScore, riskLevel, calculatedAt));
        }

        public EmployeeResult setFinalScore(UUID employeeId, UUID formId, int finalScore, Instant updatedAt) {
        EmployeeResult existing = employeeResultRepositoryPort.findByEmployeeIdAndFormId(employeeId, formId)
            .orElseGet(() -> createOrUpdateHelperScore(employeeId, formId, 0, updatedAt));

        return employeeResultRepositoryPort.save(EmployeeResult.reconstitute(
            existing.getId(),
            employeeId,
            formId,
            existing.getHelperScore(),
            finalScore,
            classifyBurnoutRisk(finalScore),
            updatedAt));
    }

    public Optional<EmployeeResult> findByEmployeeIdAndFormId(UUID employeeId, UUID formId) {
        return employeeResultRepositoryPort.findByEmployeeIdAndFormId(employeeId, formId);
    }

    public RiskLevel classifyBurnoutRisk(int score) {
        if (score >= 70) {
            return RiskLevel.HIGH;
        }
        if (score >= 40) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    public EmployeeResult get(UUID id) {
        return employeeResultRepositoryPort.findById(id)
                .orElseThrow(() -> new EmployeeResultNotFoundException(id));
    }

    public EmployeeResult getByEmployeeId(UUID employeeId) {
        return employeeResultRepositoryPort.findByEmployeeId(employeeId)
                .orElseThrow(() -> new EmployeeResultNotFoundException(employeeId));
    }

    public EmployeeResult getByFormId(UUID formId) {
        return employeeResultRepositoryPort.findByFormId(formId)
                .orElseThrow(() -> new EmployeeResultNotFoundException(formId));
    }

    public List<EmployeeResult> list() {
        return employeeResultRepositoryPort.findAll();
    }

    public List<EmployeeResult> listByFormId(UUID formId) {
        return employeeResultRepositoryPort.findAllByFormId(formId);
    }

    public EmployeeResult update(UUID id, UUID employeeId, UUID formId, int score, RiskLevel riskLevel, Instant calculatedAt) {
        get(id);
        return employeeResultRepositoryPort.save(EmployeeResult.reconstitute(id, employeeId, formId, score, null, riskLevel, calculatedAt));
    }

    public void delete(UUID id) {
        get(id);
        employeeResultRepositoryPort.deleteById(id);
    }
}

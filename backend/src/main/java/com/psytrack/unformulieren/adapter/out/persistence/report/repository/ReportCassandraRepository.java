package com.psytrack.unformulieren.adapter.out.persistence.report.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.cassandra.repository.CassandraRepository;

import com.psytrack.unformulieren.adapter.out.persistence.report.entity.ReportEntity;
import com.psytrack.unformulieren.adapter.out.persistence.report.entity.ReportPrimaryKey;

public interface ReportCassandraRepository extends CassandraRepository<ReportEntity, ReportPrimaryKey> {

    List<ReportEntity> findByKeyTeamId(UUID teamId);
}

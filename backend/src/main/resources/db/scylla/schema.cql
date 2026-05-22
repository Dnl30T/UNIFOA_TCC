-- =============================================================================
-- PsyTrack - ScyllaDB Wide-Column Schema
-- =============================================================================
-- Strategy: denormalise forms so the Form Renderer can fetch the entire form
-- (metadata + all questions) in a single partition read.
-- =============================================================================

CREATE KEYSPACE IF NOT EXISTS psytrack
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}
    AND durable_writes = true;

USE psytrack;

-- ---------------------------------------------------------------------------
-- UDT: question_structure
-- Embeds all fields from the former 'questions' and 'question_configs' tables
-- so that every question + its configuration live inside the parent form row.
-- ---------------------------------------------------------------------------
CREATE TYPE IF NOT EXISTS question_structure (
    id            uuid,
    form_id       uuid,
    text          text,
    type          text,
    required      boolean,
    config        map<text, text>,
    display_order int
);

-- ---------------------------------------------------------------------------
-- Table: forms
-- Partition key : form_id  — one partition per form, perfect for point reads.
-- The 'questions' column is a LIST of frozen UDTs, enabling a zero-JOIN read
-- that returns all form data (metadata + questions) to the Form Renderer.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS forms (
    form_id     uuid,
    title       text,
    description text,
    status      text,
    created_by  text,
    team_ids    list<uuid>,
    questions   list<frozen<question_structure>>,
    PRIMARY KEY (form_id)
);

-- Secondary index for title-based lookups (used by the duplicate-title guard).
CREATE INDEX IF NOT EXISTS forms_title_idx ON forms (title);

-- ---------------------------------------------------------------------------
-- Table: form_responses
-- Partition key : (form_id, employee_id) — one row per employee submission.
-- All question answers are stored in a single `answers` map<uuid,int>,
-- keeping each submission as a single row with no clustering columns.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS form_responses (
    form_id       uuid,
    employee_id   uuid,
    submission_id uuid,
    answers       map<uuid, int>,
    text_answers  map<uuid, text>,
    submitted_at  timestamp,
    response_status text,
    closed_at     timestamp,
    PRIMARY KEY (form_id, employee_id)
);

-- ---------------------------------------------------------------------------
-- Table: therapist_evaluations
-- One evaluation per (form_id, employee_id) pair — written by the COUNSELOR.
-- Holds free-text clinical notes, dimension scores + justifications,
-- a private internal note and a sanitised HR summary.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS therapist_evaluations (
    form_id                          uuid,
    employee_id                      uuid,
    -- Module 1: Stressors
    occupational_context             text,
    team_dynamics                    text,
    -- Module 2: Symptomatology
    psychosomatic_symptoms           text,
    cognitive_emotional_changes      text,
    -- Module 3: Dimensions
    exhaustion_score                 int,
    exhaustion_justification         text,
    depersonalization_score          int,
    depersonalization_justification  text,
    -- Module 4: Clinical Opinion
    differential_diagnosis           text,
    intervention_plan                text,
    -- Module 5: Visibility
    internal_note                    text,
    hr_summary                       text,
    closing_commentary               text,
    stress_score                     int,
    sleep_score                      int,
    overload_score                   int,
    fatigue_score                    int,
    disengagement_score              int,
    isolation_score                  int,
    status                           text,
    published_at                     timestamp,
    -- Metadata
    created_by                       text,
    created_at                       timestamp,
    updated_at                       timestamp,
    PRIMARY KEY (form_id, employee_id)
);

-- ---------------------------------------------------------------------------
-- Table: reports
-- One report per counselor-generated aggregation of a closed form.
-- Partition key : team_id — efficient team-scoped queries.
-- Clustering key: id — unique report identifier within the team partition.
-- Metrics are calculated once at generation time and stored immutably.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reports (
    team_id           uuid,
    id                uuid,
    form_id           uuid,
    name              text,
    respondent_ids    list<uuid>,
    respondent_count  int,
    average_score     double,
    general_risk      text,
    risk_distribution map<text, int>,
    generated_at      timestamp,
    created_by        text,
    PRIMARY KEY (team_id, id)
);

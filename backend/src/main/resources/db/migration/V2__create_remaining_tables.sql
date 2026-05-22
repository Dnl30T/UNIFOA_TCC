CREATE TABLE teams (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    CONSTRAINT uk_team_name UNIQUE (name)
);

CREATE TABLE forms (
    id UUID PRIMARY KEY,
    title VARCHAR(140) NOT NULL,
    description VARCHAR(400) NOT NULL DEFAULT '',
    status VARCHAR(24) NOT NULL,
    CONSTRAINT uk_form_title UNIQUE (title)
);

CREATE TABLE questions (
    id UUID PRIMARY KEY,
    text VARCHAR(500) NOT NULL,
    type VARCHAR(32) NOT NULL,
    required_flag BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE question_configs (
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    config_key VARCHAR(120) NOT NULL,
    config_value VARCHAR(600),
    PRIMARY KEY (question_id, config_key)
);

CREATE TABLE form_responses (
    id UUID PRIMARY KEY,
    question_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    value INTEGER NOT NULL,
    response_timestamp TIMESTAMPTZ NOT NULL
);

CREATE TABLE employee_results (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL,
    form_id UUID NOT NULL,
    score INTEGER NOT NULL CHECK (score BETWEEN 0 AND 100),
    risk_level VARCHAR(24) NOT NULL,
    calculated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE team_results (
    id UUID PRIMARY KEY,
    team_id UUID NOT NULL,
    form_id UUID NOT NULL,
    average_score DOUBLE PRECISION NOT NULL,
    calculated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE team_result_risk_level_distribution (
    team_result_id UUID NOT NULL REFERENCES team_results(id) ON DELETE CASCADE,
    risk_level VARCHAR(24) NOT NULL,
    quantity INTEGER NOT NULL,
    PRIMARY KEY (team_result_id, risk_level)
);

CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    username VARCHAR(80) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(180) NOT NULL,
    role VARCHAR(24) NOT NULL,
    CONSTRAINT uk_app_user_username UNIQUE (username)
);

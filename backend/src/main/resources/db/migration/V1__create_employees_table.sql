CREATE TABLE employees (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL,
    team_id UUID NOT NULL,
    status VARCHAR(24) NOT NULL,
    CONSTRAINT uk_employee_email UNIQUE (email)
);
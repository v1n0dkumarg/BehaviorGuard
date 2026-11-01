CREATE TABLE user_risk_profiles (
    user_id VARCHAR(80) PRIMARY KEY,
    display_name VARCHAR(160) NOT NULL,
    department VARCHAR(120) NOT NULL,
    score SMALLINT NOT NULL CHECK (score BETWEEN 0 AND 100),
    tier VARCHAR(24) NOT NULL,
    baseline VARCHAR(500) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_user_risk_score ON user_risk_profiles (score DESC, user_id);

CREATE TABLE risk_factors (
    id UUID PRIMARY KEY,
    user_id VARCHAR(80) NOT NULL REFERENCES user_risk_profiles(user_id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    code VARCHAR(80) NOT NULL,
    label VARCHAR(160) NOT NULL,
    points SMALLINT NOT NULL CHECK (points >= 0),
    explanation VARCHAR(600) NOT NULL,
    UNIQUE (user_id, code),
    UNIQUE (user_id, position)
);

CREATE TABLE risk_factor_evidence (
    factor_id UUID NOT NULL REFERENCES risk_factors(id) ON DELETE CASCADE,
    event_id VARCHAR(120) NOT NULL,
    position INTEGER NOT NULL,
    PRIMARY KEY (factor_id, event_id),
    UNIQUE (factor_id, position)
);

CREATE TABLE investigation_cases (
    id UUID PRIMARY KEY,
    user_id VARCHAR(80) NOT NULL REFERENCES user_risk_profiles(user_id),
    title VARCHAR(240) NOT NULL,
    severity VARCHAR(24) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_investigation_cases_created ON investigation_cases (created_at DESC);

CREATE TABLE case_evidence (
    case_id UUID NOT NULL REFERENCES investigation_cases(id) ON DELETE CASCADE,
    event_id VARCHAR(120) NOT NULL,
    position INTEGER NOT NULL,
    PRIMARY KEY (case_id, event_id),
    UNIQUE (case_id, position)
);
ALTER TABLE colombia_bets
    ADD COLUMN IF NOT EXISTS principal_global_prediction BOOLEAN NOT NULL DEFAULT FALSE;

WITH first_bets AS (
    SELECT DISTINCT ON (user_id, match_id) id
    FROM colombia_bets
    WHERE status NOT IN ('ANNULLED')
    ORDER BY user_id, match_id, registered_at ASC
)
UPDATE colombia_bets cb
SET principal_global_prediction = TRUE
FROM first_bets fb
WHERE cb.id = fb.id
  AND NOT EXISTS (
      SELECT 1
      FROM colombia_bets other
      WHERE other.user_id = cb.user_id
        AND other.match_id = cb.match_id
        AND other.principal_global_prediction = TRUE
  );

CREATE UNIQUE INDEX IF NOT EXISTS uk_colombia_bets_one_principal
    ON colombia_bets (user_id, match_id)
    WHERE principal_global_prediction = TRUE
      AND status <> 'ANNULLED';

INSERT INTO system_parameters (parameter_key, parameter_value, description, value_type)
VALUES
    ('WORLD_CHAMPION_POINTS', '10', 'Puntos extra por acertar el campeon del Mundial.', 'INTEGER'),
    ('WORLD_CHAMPION_LOCK_AT', '2026-06-11T00:00:00Z', 'Fecha y hora UTC de cierre para elegir campeon mundial.', 'TEXT')
ON CONFLICT (parameter_key) DO NOTHING;

CREATE TABLE IF NOT EXISTS world_champion_predictions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    team_code VARCHAR(10) NOT NULL REFERENCES teams(fifa_code),
    status VARCHAR(30) NOT NULL DEFAULT 'VALID' CHECK (status IN ('VALID', 'LOCKED', 'EVALUATED')),
    points INTEGER NOT NULL DEFAULT 0 CHECK (points >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_world_champion_predictions_team
    ON world_champion_predictions (team_code);

CREATE TABLE IF NOT EXISTS audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    username VARCHAR(80),
    role VARCHAR(20),
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(120),
    request_summary VARCHAR(1000),
    response_summary VARCHAR(1000),
    ip_address VARCHAR(80),
    user_agent VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_events_created_at ON audit_events (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_events_username ON audit_events (username);
CREATE INDEX IF NOT EXISTS idx_audit_events_action ON audit_events (action);
CREATE INDEX IF NOT EXISTS idx_audit_events_entity ON audit_events (entity_type, entity_id);

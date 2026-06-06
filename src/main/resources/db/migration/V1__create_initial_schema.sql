CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(120) NOT NULL,
    full_name VARCHAR(160) NOT NULL,
    email VARCHAR(160) UNIQUE,
    phone VARCHAR(40),
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'PLAYER')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING')),
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uk_users_single_admin ON users ((role)) WHERE role = 'ADMIN';
CREATE INDEX idx_users_role_status ON users (role, status);

CREATE TABLE auth_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    refresh_token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_auth_sessions_user_active ON auth_sessions (user_id, revoked_at, expires_at);

CREATE TABLE system_parameters (
    parameter_key VARCHAR(80) PRIMARY KEY,
    parameter_value VARCHAR(250) NOT NULL,
    description VARCHAR(500),
    value_type VARCHAR(30) NOT NULL CHECK (value_type IN ('INTEGER', 'DECIMAL', 'BOOLEAN', 'TEXT')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE teams (
    fifa_code VARCHAR(10) PRIMARY KEY,
    api_football_id INTEGER UNIQUE,
    name VARCHAR(120) NOT NULL,
    country VARCHAR(120),
    confederation VARCHAR(80),
    flag_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE competitions (
    id BIGSERIAL PRIMARY KEY,
    api_football_league_id INTEGER UNIQUE,
    name VARCHAR(160) NOT NULL,
    season INTEGER NOT NULL,
    type VARCHAR(80),
    country VARCHAR(120),
    logo_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_competitions_name_season UNIQUE (name, season)
);

CREATE TABLE matches (
    id BIGINT PRIMARY KEY,
    competition_id BIGINT REFERENCES competitions(id),
    stage VARCHAR(120),
    group_name VARCHAR(80),
    round_name VARCHAR(120),
    kickoff_at_utc TIMESTAMPTZ NOT NULL,
    timezone VARCHAR(80) DEFAULT 'UTC',
    venue_name VARCHAR(160),
    venue_city VARCHAR(120),
    venue_country VARCHAR(120),
    home_team_code VARCHAR(10) NOT NULL REFERENCES teams(fifa_code),
    away_team_code VARCHAR(10) NOT NULL REFERENCES teams(fifa_code),
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED', 'LIVE', 'FINISHED', 'POSTPONED', 'CANCELLED', 'SUSPENDED')),
    api_status_short VARCHAR(10),
    api_status_long VARCHAR(80),
    last_synced_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_matches_different_teams CHECK (home_team_code <> away_team_code)
);

CREATE INDEX idx_matches_kickoff ON matches (kickoff_at_utc);
CREATE INDEX idx_matches_status ON matches (status);
CREATE INDEX idx_matches_home_away ON matches (home_team_code, away_team_code);

CREATE TABLE match_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id BIGINT NOT NULL UNIQUE REFERENCES matches(id) ON DELETE CASCADE,
    home_goals_90 INTEGER NOT NULL CHECK (home_goals_90 >= 0),
    away_goals_90 INTEGER NOT NULL CHECK (away_goals_90 >= 0),
    winner_90 VARCHAR(20) NOT NULL CHECK (winner_90 IN ('HOME', 'AWAY', 'DRAW')),
    source VARCHAR(80) NOT NULL DEFAULT 'API_FOOTBALL',
    confirmed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE colombia_bets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    match_id BIGINT NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    predicted_home_goals INTEGER NOT NULL CHECK (predicted_home_goals >= 0),
    predicted_away_goals INTEGER NOT NULL CHECK (predicted_away_goals >= 0),
    amount_cop NUMERIC(12,2) NOT NULL CHECK (amount_cop > 0),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT' CHECK (status IN ('PENDING_PAYMENT', 'VALID', 'ANNULLED', 'WON', 'LOST')),
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (payment_status IN ('PENDING', 'PAID')),
    valid BOOLEAN NOT NULL DEFAULT FALSE,
    registered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    prize_amount_cop NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (prize_amount_cop >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_colombia_bets_user_match ON colombia_bets (user_id, match_id);
CREATE INDEX idx_colombia_bets_match_status ON colombia_bets (match_id, status);

CREATE TABLE global_predictions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    match_id BIGINT NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    predicted_home_goals INTEGER NOT NULL CHECK (predicted_home_goals >= 0),
    predicted_away_goals INTEGER NOT NULL CHECK (predicted_away_goals >= 0),
    status VARCHAR(30) NOT NULL DEFAULT 'VALID' CHECK (status IN ('VALID', 'ANNULLED', 'EVALUATED')),
    points INTEGER NOT NULL DEFAULT 0 CHECK (points >= 0),
    exact_hit BOOLEAN NOT NULL DEFAULT FALSE,
    winner_hit BOOLEAN NOT NULL DEFAULT FALSE,
    registered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    evaluated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_global_predictions_user_match UNIQUE (user_id, match_id)
);

CREATE INDEX idx_global_predictions_match_status ON global_predictions (match_id, status);

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    system VARCHAR(20) NOT NULL CHECK (system IN ('COLOMBIA', 'GLOBAL')),
    match_id BIGINT REFERENCES matches(id) ON DELETE SET NULL,
    colombia_bet_id UUID REFERENCES colombia_bets(id) ON DELETE SET NULL,
    amount_cop NUMERIC(12,2) NOT NULL CHECK (amount_cop > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PAID', 'REJECTED')),
    payment_method VARCHAR(80),
    reference VARCHAR(160),
    paid_at TIMESTAMPTZ,
    confirmed_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_user_status ON payments (user_id, status);
CREATE INDEX idx_payments_system_status ON payments (system, status);

CREATE TABLE ranking_points (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    total_points INTEGER NOT NULL DEFAULT 0 CHECK (total_points >= 0),
    exact_hits INTEGER NOT NULL DEFAULT 0 CHECK (exact_hits >= 0),
    winner_hits INTEGER NOT NULL DEFAULT 0 CHECK (winner_hits >= 0),
    colombia_points INTEGER NOT NULL DEFAULT 0 CHECK (colombia_points >= 0),
    predicted_matches INTEGER NOT NULL DEFAULT 0 CHECK (predicted_matches >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE prizes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    system VARCHAR(20) NOT NULL CHECK (system IN ('COLOMBIA', 'GLOBAL')),
    match_id BIGINT REFERENCES matches(id) ON DELETE SET NULL,
    ranking_position INTEGER CHECK (ranking_position BETWEEN 1 AND 3),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    amount_cop NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (amount_cop >= 0),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PAID', 'ROLLED_OVER', 'SHARED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE api_sync_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sync_type VARCHAR(40) NOT NULL CHECK (sync_type IN ('FIXTURES', 'RESULTS', 'TEAMS', 'COMPETITIONS')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('SUCCESS', 'FAILED', 'SKIPPED')),
    request_path VARCHAR(500),
    records_processed INTEGER NOT NULL DEFAULT 0 CHECK (records_processed >= 0),
    message TEXT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at TIMESTAMPTZ
);

CREATE INDEX idx_api_sync_logs_type_started ON api_sync_logs (sync_type, started_at DESC);

INSERT INTO system_parameters (parameter_key, parameter_value, description, value_type) VALUES
('COLOMBIA_BET_AMOUNT_COP', '5000', 'Valor de cada apuesta de Colombia Especial en COP.', 'DECIMAL'),
('COLOMBIA_MAX_BETS_PER_MATCH', '3', 'Maximo de apuestas por jugador en cada partido de Colombia.', 'INTEGER'),
('GLOBAL_REGISTRATION_AMOUNT_COP', '20000', 'Inscripcion unica sugerida para la Polla Global.', 'DECIMAL'),
('CLOSING_MINUTES_BEFORE_MATCH', '10', 'Minutos antes del inicio oficial para cerrar apuestas y pronosticos.', 'INTEGER'),
('GLOBAL_EXACT_POINTS', '5', 'Puntos por marcador exacto en Polla Global.', 'INTEGER'),
('GLOBAL_WINNER_POINTS', '2', 'Puntos por ganador correcto o empate correcto sin exacto.', 'INTEGER'),
('GLOBAL_PRIZE_FIRST_PERCENT', '50', 'Porcentaje del pozo global para el primer puesto.', 'INTEGER'),
('GLOBAL_PRIZE_SECOND_PERCENT', '30', 'Porcentaje del pozo global para el segundo puesto.', 'INTEGER'),
('GLOBAL_PRIZE_THIRD_PERCENT', '20', 'Porcentaje del pozo global para el tercer puesto.', 'INTEGER'),
('GLOBAL_RESERVE_PERCENT', '5', 'Reserva sugerida del pozo global para redondeos o imprevistos.', 'INTEGER');

INSERT INTO users (username, password_hash, full_name, email, role, status) VALUES
('andres.administrador', '$2a$10$j4fnIjXL1f7klIUMy8pVbe5NbjRBUkP9dxX2gh7deU3j.FrGaeRFu', 'Andres Administrador', 'andres.administrador@famicup.local', 'ADMIN', 'ACTIVE'),
('tia.maria', '$2a$10$9csp5BTxMMNx2I2omKq1ie699W39JSSPTw1OI7hpbGasOvQrAsviG', 'Tia Maria', 'tia.maria@famicup.local', 'PLAYER', 'ACTIVE');

INSERT INTO ranking_points (user_id)
SELECT id FROM users WHERE role = 'PLAYER';

INSERT INTO payments (user_id, system, amount_cop, status, payment_method, paid_at)
SELECT id, 'GLOBAL', 20000, 'PENDING', 'Por confirmar', NULL
FROM users
WHERE role = 'PLAYER';

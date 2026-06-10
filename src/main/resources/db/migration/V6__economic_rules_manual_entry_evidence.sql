INSERT INTO system_parameters (parameter_key, parameter_value, description, value_type)
VALUES
    ('ORGANIZER_FEE_AMOUNT_COP', '10000', 'Valor por participante destinado al organizador y administracion de FamiCup.', 'DECIMAL'),
    ('GLOBAL_PRIZE_POOL_AMOUNT_COP', '40000', 'Valor por participante que ingresa al pozo premiable de la Polla Global.', 'DECIMAL')
ON CONFLICT (parameter_key) DO UPDATE
SET parameter_value = EXCLUDED.parameter_value,
    description = EXCLUDED.description,
    value_type = EXCLUDED.value_type;

UPDATE system_parameters
SET parameter_value = '50000',
    description = 'Inscripcion global parametrizable. Configuracion actual: 50000 COP; 10000 COP para organizador y 40000 COP para el pozo premiable global.'
WHERE parameter_key = 'GLOBAL_REGISTRATION_AMOUNT_COP';

UPDATE system_parameters
SET parameter_value = '2026-06-11T14:00:00',
    description = 'Fecha y hora America/Bogota de cierre para elegir campeon mundial.'
WHERE parameter_key = 'WORLD_CHAMPION_LOCK_AT';

UPDATE payments
SET amount_cop = 50000
WHERE system = 'GLOBAL'
  AND status = 'PENDING'
  AND amount_cop IN (20000, 60000);

ALTER TABLE colombia_bets
    ADD COLUMN IF NOT EXISTS entry_origin VARCHAR(20) NOT NULL DEFAULT 'PLAYER',
    ADD COLUMN IF NOT EXISTS created_by_admin_id UUID REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS updated_by_admin_id UUID REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE global_predictions
    ADD COLUMN IF NOT EXISTS entry_origin VARCHAR(20) NOT NULL DEFAULT 'PLAYER',
    ADD COLUMN IF NOT EXISTS created_by_admin_id UUID REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS updated_by_admin_id UUID REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE colombia_bets
    DROP CONSTRAINT IF EXISTS ck_colombia_bets_entry_origin,
    ADD CONSTRAINT ck_colombia_bets_entry_origin CHECK (entry_origin IN ('PLAYER', 'ADMIN'));

ALTER TABLE global_predictions
    DROP CONSTRAINT IF EXISTS ck_global_predictions_entry_origin,
    ADD CONSTRAINT ck_global_predictions_entry_origin CHECK (entry_origin IN ('PLAYER', 'ADMIN'));

CREATE INDEX IF NOT EXISTS idx_colombia_bets_entry_origin ON colombia_bets (entry_origin);
CREATE INDEX IF NOT EXISTS idx_global_predictions_entry_origin ON global_predictions (entry_origin);

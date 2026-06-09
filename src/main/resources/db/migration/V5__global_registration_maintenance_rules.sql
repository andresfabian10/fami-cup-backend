UPDATE system_parameters
SET parameter_value = '60000',
    description = 'Inscripcion unica para la Polla Global. Incluye 30000 COP de mantenimiento de la app y 30000 COP para el pozo premiable global.'
WHERE parameter_key = 'GLOBAL_REGISTRATION_AMOUNT_COP';

UPDATE system_parameters
SET parameter_value = '0',
    description = 'Reserva porcentual del pozo global. La regla vigente usa aporte fijo de mantenimiento por participante.'
WHERE parameter_key = 'GLOBAL_RESERVE_PERCENT';

UPDATE payments
SET amount_cop = 60000
WHERE system = 'GLOBAL'
  AND status = 'PENDING'
  AND amount_cop = 20000;

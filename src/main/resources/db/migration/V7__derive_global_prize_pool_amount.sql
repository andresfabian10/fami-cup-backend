UPDATE system_parameters
SET parameter_value = GREATEST(
    0,
    (
        SELECT parameter_value::numeric
        FROM system_parameters
        WHERE parameter_key = 'GLOBAL_REGISTRATION_AMOUNT_COP'
    ) - (
        SELECT parameter_value::numeric
        FROM system_parameters
        WHERE parameter_key = 'ORGANIZER_FEE_AMOUNT_COP'
    )
)::text,
    description = 'Valor calculado por participante que ingresa al pozo premiable de la Polla Global: inscripcion global menos aporte del organizador.'
WHERE parameter_key = 'GLOBAL_PRIZE_POOL_AMOUNT_COP';

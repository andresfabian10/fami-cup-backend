UPDATE system_parameters
SET parameter_value = '5000',
    description = 'Valor oficial de cada apuesta de Colombia Especial en COP.'
WHERE parameter_key = 'COLOMBIA_BET_AMOUNT_COP';

UPDATE system_parameters
SET parameter_value = '3',
    description = 'Maximo oficial de apuestas por jugador en cada partido de Colombia.'
WHERE parameter_key = 'COLOMBIA_MAX_BETS_PER_MATCH';

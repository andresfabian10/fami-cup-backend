---
name: reglas-apuestas
description: Reglas de apuestas - gestión de pronósticos, quinielas y resultados
---

# Reglamento y modelo de juego — App familiar de apuestas · Mundial 2026

Versión recomendada para MVP familiar — Sistema equilibrado de dinero + puntos

| Objetivo | Definir reglas claras, simples y transparentes para lanzar una PWA familiar de apuestas. |
| :---- | :---- |
| **Alcance** | Dos sistemas en la misma app: apuestas por dinero para partidos de Colombia y quiniela general por puntos para todo el Mundial. |
| **Base de diseño** | Esta propuesta se apoya en la información de la API [api-football](https://www.api-football.com/news/post/fifa-world-cup-2026-guide-to-using-data-with-api-sports) la cual tiene la siguiente documentación [DOCUMENTATION API](https://www.api-football.com/documentation-v3#section/Introduction). |

Documento preparado para uso interno y para compartir con la familia.

---

## 1. Diagnóstico de la idea actual

### Fortalezas

- La dinámica actual ya está validada en tu familia: es simple, emocionante y fácil de entender.
- El marcador exacto en partidos de Colombia genera mucha expectativa y mantiene el interés hasta el final.
- Permitir varias apuestas por persona aumenta la emoción y el recaudo por partido.
- Combinar dinero en partidos de Colombia con un ranking por puntos durante todo el Mundial mantiene activa a la familia durante todo el torneo.
- La información base del torneo ya permite modelar grupos, fases y los tres partidos iniciales de Colombia.

### Debilidades

- Si el pago no queda bien definido antes del cierre, puede generar reclamos.
- Permitir muchas apuestas por una sola persona puede percibirse como injusto si no hay límite.
- Si en el sistema de dinero solo gana el marcador exacto, muchas personas pueden quedarse sin premio con frecuencia.
- Si se mezcla el dinero del sistema 1 con el sistema 2, la administración se vuelve confusa.
- Si no se comunica bien la regla de 90 minutos, alargue y penales, pueden aparecer discusiones.

### Vacíos en las reglas

- Mantener visible que el sistema 2 tiene inscripción global parametrizable. Para la configuración actual, cada jugador paga 50.000 COP: 10.000 COP corresponden al organizador y 40.000 COP ingresan a la bolsa de premios de la polla.
- Definir qué pasa si una persona registró una apuesta pero no pagó a tiempo.
- Definir cuántas apuestas máximas se permiten por persona en partidos de Colombia.
- Comunicar claramente que en eliminación directa solo cuentan los 90 minutos más reposición.
- Definir cómo se resuelven los empates en el ranking final.
- Definir cómo actuar ante partidos aplazados, suspendidos o cancelados.

### Riesgos operativos y conflictos de interpretación

- Confusión por horarios si no existe una única hora oficial de cierre.
- Errores manuales al gestionar pagos, apuestas y resultados desde varias herramientas.
- Reclamos del tipo "yo sí envié antes" o "ya te había pagado".
- Reclamos si alguien interpreta que alargue o penales cambian puntos, aunque el reglamento indique que no cuentan.
- Confusión con futuros cruces de Colombia si se muestran antes de estar oficialmente confirmados.

---

## 2. Propuesta del modelo de juego

La recomendación es manejar dos sistemas en la misma app, pero separados en sus reglas y en su dinero:

- **Sistema 1:** apuestas por dinero únicamente para los partidos de Colombia.
- **Sistema 2:** quiniela general por puntos para todo el Mundial, incluyendo también los partidos de Colombia.

Operativamente deben ser sistemas independientes, pero visualmente integrados dentro de la misma app.

| Tema | Recomendación |
| ----- | ----- |
| Relación entre sistemas | Separados en reglas y en dinero; unidos en la experiencia del usuario |
| Partidos de Colombia | Participan en ambos sistemas al mismo tiempo |
| Sistema 1 | Pozo por partido de Colombia |
| Sistema 2 | Pozo global del ranking general |
| Recomendación final | Mantener dinero y puntos por separado para evitar confusión y facilitar la administración |

---

## 3. Reglamento oficial sugerido

| Tema | Regla propuesta |
| ----- | ----- |
| Quién puede participar | Solo familiares invitados y registrados en la app o en la lista oficial del juego. |
| Costo por apuesta en Sistema 1 | Cada apuesta en un partido de Colombia cuesta 5.000 COP. |
| Inscripción Sistema 2 | La inscripción global tiene un valor parametrizable. Para la configuración actual, cada jugador paga 50.000 COP. De ese valor, 10.000 COP corresponden al organizador y 40.000 COP ingresan a la bolsa de premios de la polla. |
| Cantidad de apuestas en Sistema 1 | Se permiten varias apuestas por persona en un mismo partido de Colombia, con recomendación de máximo 3 apuestas por usuario por partido. |
| Cantidad de apuestas en Sistema 2 | Solo se permite 1 pronóstico por usuario por partido. |
| Hora de cierre | Todas las apuestas y pronósticos se cierran 10 minutos antes de la hora oficial de inicio del partido. |
| Qué pasa si no ha pagado | En el sistema 1, una apuesta no pagada antes del cierre queda anulada. |
| Cómo se valida una apuesta | Debe quedar registrada con usuario, partido, marcador, hora de registro y estado de pago, si aplica. |
| Qué resultados cuentan | Cuenta el marcador oficial al final del tiempo reglamentario: 90 minutos más reposición. |
| Alargue y penales | No cuentan para marcador, ganador ni puntos del MVP. Si un partido de eliminación directa llega a penales, para la Polla Global sigue contando el resultado de los 90 minutos más reposición; no se otorgan puntos por acertar el ganador de la tanda de penales. |
| Cómo se define un ganador en Sistema 1 | Gana quien acierte el marcador exacto del partido de Colombia. |
| Cómo se reparte el dinero | El pozo del partido se reparte en partes iguales entre todos los acertantes exactos. |
| Qué pasa si nadie acierta en Sistema 1 | El pozo se acumula para el siguiente partido de Colombia. |
| Cómo se calculan los puntos | Se aplica el sistema equilibrado recomendado: exacto 5 puntos, ganador correcto 2 puntos, empate correcto 2 puntos. |
| Apuesta principal en partidos de Colombia | Si un jugador registra varias apuestas para un partido de Colombia, debe escoger una como pronóstico principal; esa será la que suma puntos en la Polla Global. Si solo registra una, puede marcarse automáticamente como principal. |
| Campeón del mundo | Cada jugador puede escoger y editar un único campeón mundial antes del inicio oficial del Mundial; la app usará el cierre configurado para bloquear esta selección. Acertarlo otorga 10 puntos extra en el ranking general. |
| Empate en puntos al final | Se desempata por más exactos, luego por más aciertos de resultado, luego por mejor desempeño en partidos de Colombia; si persiste, se comparte el premio correspondiente. |
| Partidos cancelados, aplazados o suspendidos | Se reprograma el cierre cuando exista nueva hora oficial; si el resultado oficial es anulado, la apuesta y el pronóstico del partido también se anulan. |
| Diferencias entre fase de grupos y eliminación directa | En ambas etapas cuenta el resultado al final del tiempo reglamentario para efectos del MVP. |
| Tratamiento especial para Colombia | Los partidos de Colombia participan tanto en el sistema 1 como en el sistema 2. |
| Reglas anti-discusión | La app será la fuente válida de cierre, estado de pago, registro de apuesta y resultado oficial. |

---

## 4. Sistema de puntos

Se evaluaron tres alternativas para el ranking general por puntos:

| Opción | Exacto | Ganador | Empate | Ventaja principal | Desventaja principal |
| :---: | :---: | :---: | :---: | ----- | ----- |
| Conservadora | 3 | 1 | 1 | Muy simple y fácil de explicar | Premia poco el marcador exacto |
| **Equilibrada** | **5** | **2** | **2** | Buen balance entre emoción y simplicidad | Ligeramente menos competitiva que la opción avanzada |
| Competitiva | 7 | 3 | 3 | Muy emocionante | Más difícil de explicar y administrar |

**Recomendación final:** usar la opción equilibrada para partidos y agregar solo el extra de campeón mundial.

**Sistema recomendado:**
- Marcador exacto = **5 puntos**
- Ganador correcto (sin exacto) = **2 puntos**
- Empate correcto (sin exacto) = **2 puntos**
- Cualquier otro caso = **0 puntos**
- Campeón mundial acertado = **10 puntos extra**

**Regla para eliminación directa:** si un partido se define en alargue o penales, esos goles o esa tanda no modifican el puntaje de la Polla Global. Si el partido terminó empatado al cierre de los 90 minutos más reposición, cuenta como empate para puntos, aunque después haya un ganador por penales.

---

## 5. Premios del sistema 2 para los 3 primeros puestos

- Inscripción del MVP / Polla Global: **50.000 COP** por participante en la configuración actual.
- Aporte del organizador / administración: **10.000 COP** por participante.
- Aporte al pozo premiable global: **40.000 COP** por participante.
- Separar completamente el dinero del sistema 1 y del sistema 2.
- Los premios del sistema 2 se calculan únicamente sobre el pozo premiable global, es decir, después de descontar el aporte del organizador / administración.

| Modelo | 1ro | 2do | 3ro | Ventaja | Cuándo conviene |
| ----- | :---: | :---: | :---: | ----- | ----- |
| Agresivo | 60% | 25% | 15% | Premia mucho al 1ro | Grupos muy competitivos |
| **Balanceado** | **50%** | **30%** | **20%** | Buen equilibrio entre premio y armonía | Recomendado para familia |
| Familiar / equitativo | 45% | 30% | 25% | Más sensación de justicia | Cuando se prioriza la convivencia |

**Modelo recomendado para la familia:** balanceado — 50% para el primer puesto, 30% para el segundo y 20% para el tercero.

---

## 6. Simulación simple

### 6.1 Ejemplo de partido de Colombia por dinero

Ejemplo: Colombia vs RD Congo

| Participante | Apuesta | Valor (COP) |
| :---: | ----- | :---: |
| Andrés | Colombia 2-0 RD Congo | 5.000 |
| Emilio | Colombia 1-0 RD Congo | 5.000 |
| Emilio | Colombia 2-0 RD Congo | 5.000 |
| Laura | Colombia 3-1 RD Congo | 5.000 |
| Camila | Colombia 2-0 RD Congo | 5.000 |
| Jhon | Colombia 1-1 RD Congo | 5.000 |

Pozo total del partido: **30.000 COP**

Resultado real: Colombia 2-0 RD Congo.

Ganadores exactos: Andrés, Emilio y Camila. Cada uno recibe **10.000 COP**.

### 6.2 Ejemplo de acumulación de puntos

| Participante | Partido 1 | Partido 2 | Partido 3 | Total |
| :---: | :---: | :---: | :---: | :---: |
| Andrés | 5 | 5 | 0 | 10 |
| Emilio | 2 | 5 | 5 | 12 |
| Laura | 0 | 2 | 2 | 4 |
| Camila | 2 | 2 | 0 | 4 |
| Jhon | 0 | 0 | 5 | 5 |

Ranking final del ejemplo: 1) Emilio con 12 puntos, 2) Andrés con 10, 3) Jhon con 5.

### 6.3 Ejemplo de premios del top 3

Supuesto: 10 familiares pagan una inscripción de 50.000 COP al sistema 2.

- Recaudo total: 500.000 COP
- Aporte del organizador / administración: 100.000 COP
- Pozo premiable global: 400.000 COP
- Modelo balanceado: 1ro **200.000 COP**, 2do **120.000 COP**, 3ro **80.000 COP**

---

## 7. Reglas funcionales para la futura app PWA

- El sistema debe permitir registrar usuarios participantes y marcar si están activos.
- El sistema debe permitir manejar en paralelo apuestas por dinero y pronósticos por puntos.
- El sistema debe permitir múltiples apuestas por usuario en partidos de Colombia, con límite configurable.
- El sistema debe bloquear registros nuevos 10 minutos antes del inicio oficial del partido.
- El sistema debe guardar sello de tiempo de cada apuesta o pronóstico.
- El sistema debe marcar una apuesta como válida solo cuando cumpla las reglas y, si aplica, tenga pago confirmado.
- El sistema debe permitir solo un pronóstico por usuario por partido en el sistema de puntos.
- El sistema debe permitir marcar una sola apuesta Colombia como principal por usuario y partido para sumar puntos en la Polla Global.
- El sistema debe permitir guardar y editar un único campeón mundial por jugador antes del inicio oficial del Mundial, usando el cierre configurado como hora de bloqueo.
- El sistema debe calcular automáticamente el pozo de cada partido de Colombia.
- El sistema debe repartir automáticamente el premio entre los acertantes exactos.
- El sistema debe acumular puntos y mostrar ranking general actualizado.
- El sistema debe reprogramar cierres cuando la hora oficial de un partido cambie.
- El sistema debe mostrar reglas, estado de pago, resultados y desempates de manera transparente.

---

## 8. Datos mínimos que debería usar la app

### Entidades mínimas

- Usuario
- Equipo
- Partido
- ApuestaDinero
- PronosticoPuntos
- Pago
- ResultadoOficial
- Ranking
- Premio
- Configuración

### Campos clave recomendados

| Entidad | Campos clave |
| ----- | ----- |
| Usuario | user_id, nombre, alias, teléfono, activo |
| Equipo | fifa_code, nombre, confederación |
| Partido | match_id, fase, grupo_id, fecha_hora_utc, fecha_hora_colombia, local, visitante, estadio, ciudad, país, estado |
| ApuestaDinero | bet_id, user_id, match_id, marcador_predicho, valor_apuesta, fecha_registro, estado_pago, válida, premio_ganado |
| PronosticoPuntos | prediction_id, user_id, match_id, marcador_predicho, fecha_registro, válida, puntos_obtenidos |
| Pago | payment_id, user_id, sistema, referencia_partido, valor, fecha_pago, estado_confirmación, medio_pago |
| ResultadoOficial | result_id, match_id, goles_local_90, goles_visitante_90, ganador_90, fuente, fecha_confirmación |
| Ranking | user_id, puntos_totales, exactos_acertados, ganadores_acertados, partidos_pronosticados |
| Premio | prize_id, sistema, puesto, user_id, valor, estado_entrega |

### Criterios de datos

- Usar `fifa_code` como clave primaria de equipo.
- Usar `match_id` como clave primaria de partido.
- Guardar fechas en UTC y derivar hora Colombia en el frontend o en capa de servicio.
- Separar datos estables del torneo de datos dinámicos como standings, marcadores y estados del partido.
- No inferir cruces futuros de terceros hasta que la asignación oficial esté confirmada.

---

## 9. Recomendación final para lanzar el MVP

- Sistema 1 solo para partidos de Colombia.
- Cada apuesta cuesta **5.000 COP**.
- Máximo **3 apuestas** por persona por partido de Colombia.
- Cierre **10 minutos** antes del inicio del partido.
- Solo gana el marcador exacto en el sistema 1.
- Si nadie acierta en el sistema 1, el pozo pasa al siguiente partido de Colombia.
- Sistema 2 con inscripción global parametrizable. Configuración actual: **50.000 COP**.
- De cada inscripción del sistema 2, **10.000 COP** corresponden al organizador / administración y **40.000 COP** entran al pozo premiable global.
- Sistema equilibrado de puntos: exacto 5, ganador 2, empate 2.
- Los partidos de Colombia suman dinero y puntos al mismo tiempo.
- Premios del sistema 2 sobre el pozo premiable global: **50% / 30% / 20%** para el top 3.
- No contar alargue ni penales en el MVP; tampoco se dan puntos por ganador de tanda de penales.
- Mantener separados los fondos del sistema 1 y del sistema 2.

---

## Anexo A. Versión corta para compartir por WhatsApp

```
Familia, para el Mundial 2026 vamos a jugar con 2 modalidades en la app:

1) Partidos de Colombia por plata
- Cada apuesta vale 5.000 COP
- Cada persona puede hacer máximo 3 apuestas por partido
- Cierre: 10 minutos antes del inicio
- Solo gana quien acierte el marcador exacto
- Si hay varios ganadores, el premio se reparte por igual
- Si nadie acierta, el pozo pasa al siguiente partido de Colombia
- Apuesta no pagada antes del cierre = no válida

2) Quiniela general por puntos
- Inscripción global parametrizable. Configuración actual: 50.000 COP
- De esa inscripción, 10.000 COP corresponden al organizador / administración y 40.000 COP entran al pozo de premios global.
- Se pronostica 1 marcador por partido durante todo el Mundial
- Puntaje: Exacto 5, ganador correcto 2, empate correcto 2
- Los partidos de Colombia también suman en esta tabla
- Al final ganan los 3 primeros del ranking

Premios quiniela general:
- 1ro: 50%
- 2do: 30%
- 3ro: 20%
Estos porcentajes se aplican sobre el pozo premiable global, después de descontar el aporte del organizador / administración.

Importante:
- Cuenta el marcador al final de los 90 minutos + adición
- No cuentan alargue ni penales
- Si un partido se va a penales, no hay puntos por acertar el ganador de la tanda; cuenta el marcador de los 90 minutos + adición.
- La app será la única válida para hora de cierre, apuestas y resultados
```

---

## Anexo B. Tabla resumen del sistema de puntos recomendado

| Regla | Puntos |
| ----- | :---: |
| Acertar marcador exacto | 5 |
| Acertar ganador sin exacto | 2 |
| Acertar empate sin exacto | 2 |
| Fallar resultado | 0 |

---

## Anexo C. Tabla resumen del modelo de premios recomendado

| Puesto | Porcentaje del pozo |
| :---: | :---: |
| 1ro | 50% |
| 2do | 30% |
| 3ro | 20% |

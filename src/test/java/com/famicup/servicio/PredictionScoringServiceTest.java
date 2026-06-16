package com.famicup.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import com.famicup.modelo.dto.ParametrosApuestasResponse;
import com.famicup.repositorio.PartidoRepository;
import com.famicup.repositorio.PronosticoGlobalRepository;
import com.famicup.repositorio.ResultadoPartidoRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PredictionScoringServiceTest {

    @Mock
    private PronosticoGlobalRepository predictionRepository;
    @Mock
    private ResultadoPartidoRepository resultRepository;
    @Mock
    private PartidoRepository partidoRepository;
    @Mock
    private BettingParametersService parametersService;
    @Mock
    private RankingService rankingService;
    @Mock
    private AuditService auditService;

    private PredictionScoringService service;

    @BeforeEach
    void setUp() {
        service = new PredictionScoringService(
                predictionRepository,
                resultRepository,
                partidoRepository,
                parametersService,
                rankingService,
                auditService);
        lenient().when(parametersService.getParameters()).thenReturn(parameters());
    }

    @Test
    void calculatesOfficialScoringCases() {
        assertPoints(2, 1, 2, 1, 5);
        assertPoints(2, 1, 1, 0, 2);
        assertPoints(2, 1, 0, 1, 0);
        assertPoints(5, 1, 2, 1, 2);
        assertPoints(1, 5, 1, 2, 2);
        assertPoints(1, 5, 2, 1, 0);
        assertPoints(0, 0, 0, 0, 5);
        assertPoints(0, 0, 1, 1, 2);
        assertPoints(2, 2, 1, 1, 2);
        assertPoints(2, 2, 2, 2, 5);
        assertPoints(2, 1, 1, 1, 0);
    }

    @Test
    void exposesReasonAndStatusForWinnerAndDrawHits() {
        var winner = service.calculate(5, 1, 2, 1);
        var draw = service.calculate(2, 2, 1, 1);

        assertThat(winner.points()).isEqualTo(2);
        assertThat(winner.reason()).isEqualTo("Ganador acertado");
        assertThat(winner.status()).isEqualTo("CALCULATED");
        assertThat(draw.points()).isEqualTo(2);
        assertThat(draw.reason()).isEqualTo("Empate acertado");
        assertThat(draw.status()).isEqualTo("CALCULATED");
    }

    private void assertPoints(int realHome, int realAway, int predictedHome, int predictedAway, int expectedPoints) {
        assertThat(service.calculate(realHome, realAway, predictedHome, predictedAway).points()).isEqualTo(expectedPoints);
    }

    private ParametrosApuestasResponse parameters() {
        return new ParametrosApuestasResponse(
                BigDecimal.valueOf(5000),
                3,
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(40000),
                10,
                5,
                2,
                50,
                30,
                20,
                0,
                10,
                OffsetDateTime.parse("2026-06-11T19:00:00Z"),
                "573163353115",
                "Recuperar",
                "Acceso",
                "Recuperar",
                "Acceso",
                false,
                "",
                "",
                "Banner",
                12);
    }
}

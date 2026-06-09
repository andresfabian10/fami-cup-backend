package com.famicup.modelo.mapper;

import com.famicup.modelo.dto.ApuestaColombiaResponse;
import com.famicup.modelo.dto.PronosticoGlobalResponse;
import com.famicup.modelo.entidad.ApuestaColombia;
import com.famicup.modelo.entidad.Partido;
import com.famicup.modelo.entidad.PronosticoGlobal;
import com.famicup.util.NombreEquipoUtil;
import org.springframework.stereotype.Component;

@Component
public class ApuestaMapper {

    private final PartidoMapper partidoMapper;

    public ApuestaMapper(PartidoMapper partidoMapper) {
        this.partidoMapper = partidoMapper;
    }

    public ApuestaColombiaResponse toColombiaResponse(ApuestaColombia bet) {
        Partido match = bet.getMatch();
        String label = NombreEquipoUtil.displayName(match.getHomeTeam().getName())
                + " vs "
                + NombreEquipoUtil.displayName(match.getAwayTeam().getName());
        return new ApuestaColombiaResponse(
                bet.getId(),
                match.getId(),
                label,
                partidoMapper.toDto(match),
                bet.getPredictedHomeGoals(),
                bet.getPredictedAwayGoals(),
                bet.getAmountCop(),
                bet.getStatus(),
                bet.getPaymentStatus(),
                bet.isValid(),
                bet.isPrincipalGlobalPrediction(),
                bet.getRegisteredAt(),
                bet.getPrizeAmountCop());
    }

    public PronosticoGlobalResponse toGlobalResponse(PronosticoGlobal prediction) {
        return new PronosticoGlobalResponse(
                prediction.getId(),
                partidoMapper.toDto(prediction.getMatch()),
                prediction.getPredictedHomeGoals(),
                prediction.getPredictedAwayGoals(),
                prediction.getStatus(),
                prediction.getPoints(),
                prediction.isExactHit(),
                prediction.isWinnerHit(),
                prediction.getRegisteredAt());
    }
}

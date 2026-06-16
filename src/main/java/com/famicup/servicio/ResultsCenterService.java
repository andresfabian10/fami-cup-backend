package com.famicup.servicio;

import com.famicup.modelo.dto.ResultsCenterResponse;
import com.famicup.modelo.entidad.Partido;
import com.famicup.modelo.entidad.PronosticoGlobal;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.modelo.mapper.PartidoMapper;
import com.famicup.repositorio.PartidoRepository;
import com.famicup.repositorio.PronosticoGlobalRepository;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResultsCenterService {

    private static final Pattern GROUP_LETTER_PATTERN = Pattern.compile("\\bgroup\\s+([a-h])\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern GROUP_STAGE_DATE_PATTERN = Pattern.compile("\\bgroup\\s+stage\\s*-?\\s*(\\d+)?\\b", Pattern.CASE_INSENSITIVE);

    private final PartidoRepository partidoRepository;
    private final PronosticoGlobalRepository predictionRepository;
    private final PartidoMapper partidoMapper;
    private final PredictionScoringService scoringService;

    public ResultsCenterService(
            PartidoRepository partidoRepository,
            PronosticoGlobalRepository predictionRepository,
            PartidoMapper partidoMapper,
            PredictionScoringService scoringService) {
        this.partidoRepository = partidoRepository;
        this.predictionRepository = predictionRepository;
        this.partidoMapper = partidoMapper;
        this.scoringService = scoringService;
    }

    @Transactional
    public ResultsCenterResponse getResultsCenter(Usuario user) {
        scoringService.repairUserPredictionsWithResults(user);
        List<Partido> matches = partidoRepository.findAllWithTeamsOrderByKickoffAtUtcAsc();
        Map<Long, PronosticoGlobal> predictionsByMatchId = predictionsByMatchId(user, matches);
        Map<GroupKey, List<ResultsCenterResponse.Match>> groupedMatches = new LinkedHashMap<>();

        for (Partido match : matches) {
            GroupKey groupKey = groupKeyFor(match);
            groupedMatches.computeIfAbsent(groupKey, ignored -> new ArrayList<>())
                    .add(new ResultsCenterResponse.Match(
                            partidoMapper.toDto(match),
                            toPrediction(predictionsByMatchId.get(match.getId()))));
        }

        List<ResultsCenterResponse.Group> groups = groupedMatches.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new ResultsCenterResponse.Group(
                        entry.getKey().id(),
                        entry.getKey().name(),
                        entry.getValue()))
                .toList();

        return new ResultsCenterResponse(groups);
    }

    private Map<Long, PronosticoGlobal> predictionsByMatchId(Usuario user, List<Partido> matches) {
        if (matches.isEmpty()) {
            return Map.of();
        }

        Map<Long, PronosticoGlobal> predictions = new LinkedHashMap<>();
        predictionRepository.findByUserAndMatchIn(user, matches)
                .forEach(prediction -> predictions.put(prediction.getMatch().getId(), prediction));
        return predictions;
    }

    private ResultsCenterResponse.UserPrediction toPrediction(PronosticoGlobal prediction) {
        if (prediction == null) {
            return null;
        }

        UUID id = prediction.getId();
        return new ResultsCenterResponse.UserPrediction(
                id,
                prediction.getPredictedHomeGoals(),
                prediction.getPredictedAwayGoals(),
                prediction.getStatus(),
                prediction.getPoints(),
                prediction.isExactHit(),
                prediction.isWinnerHit(),
                prediction.getRegisteredAt());
    }

    private GroupKey groupKeyFor(Partido match) {
        String displayName = displayGroupName(firstPresent(match.getGroupName(), match.getStage(), match.getRoundName()));
        return new GroupKey(slug(displayName), displayName, groupOrder(displayName));
    }

    private String displayGroupName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return "Otros partidos";
        }

        Matcher groupMatcher = GROUP_LETTER_PATTERN.matcher(rawName);
        if (groupMatcher.find()) {
            return "Grupo " + groupMatcher.group(1).toUpperCase(Locale.ROOT);
        }

        Matcher groupStageMatcher = GROUP_STAGE_DATE_PATTERN.matcher(rawName);
        if (groupStageMatcher.find()) {
            String matchday = groupStageMatcher.group(1);
            return matchday == null || matchday.isBlank()
                    ? "Fase de grupos"
                    : "Fase de grupos - Fecha " + matchday;
        }

        String compactName = compact(rawName);
        if (compactName.contains("roundof16")) {
            return "Octavos de final";
        }
        if (compactName.contains("quarterfinal")) {
            return "Cuartos de final";
        }
        if (compactName.contains("semifinal")) {
            return "Semifinales";
        }
        if (compactName.equals("final")) {
            return "Final";
        }
        if (compactName.contains("3rdplace") || compactName.contains("thirdplace")) {
            return "Tercer puesto";
        }

        return rawName;
    }

    private int groupOrder(String displayName) {
        String normalized = compact(displayName);
        Matcher groupMatcher = Pattern.compile("grupo([a-h])").matcher(normalized);
        if (groupMatcher.find()) {
            return groupMatcher.group(1).charAt(0) - 'a';
        }
        if (normalized.startsWith("fasedegruposfecha")) {
            return 100 + numberSuffix(normalized);
        }
        if (normalized.equals("fasedegrupos")) {
            return 100;
        }
        if (normalized.contains("octavos")) {
            return 200;
        }
        if (normalized.contains("cuartos")) {
            return 210;
        }
        if (normalized.contains("semifinal")) {
            return 220;
        }
        if (normalized.contains("tercerpuesto")) {
            return 230;
        }
        if (normalized.equals("final")) {
            return 240;
        }
        return 500;
    }

    private int numberSuffix(String value) {
        String digits = value.replaceAll("\\D+", "");
        if (digits.isBlank()) {
            return 0;
        }
        return Integer.parseInt(digits);
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String slug(String value) {
        return normalize(value).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private String compact(String value) {
        return normalize(value).replaceAll("[^a-z0-9]+", "");
    }

    private String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private record GroupKey(String id, String name, int order) implements Comparable<GroupKey> {

        @Override
        public int compareTo(GroupKey other) {
            return Comparator.comparingInt(GroupKey::order)
                    .thenComparing(GroupKey::name)
                    .compare(this, other);
        }
    }
}

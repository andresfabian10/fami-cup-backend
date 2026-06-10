package com.famicup.servicio;

import com.famicup.excepcion.ReglaNegocioException;
import com.famicup.modelo.entidad.ApuestaColombia;
import com.famicup.modelo.entidad.Pago;
import com.famicup.modelo.entidad.Partido;
import com.famicup.modelo.entidad.PronosticoCampeonMundial;
import com.famicup.modelo.entidad.PronosticoGlobal;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.modelo.mapper.PartidoMapper;
import com.famicup.repositorio.ApuestaColombiaRepository;
import com.famicup.repositorio.PagoRepository;
import com.famicup.repositorio.PartidoRepository;
import com.famicup.repositorio.PronosticoCampeonMundialRepository;
import com.famicup.repositorio.PronosticoGlobalRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvidenceExportService {

    private static final ZoneId BOGOTA_ZONE = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final PartidoRepository partidoRepository;
    private final PronosticoGlobalRepository globalPredictionRepository;
    private final ApuestaColombiaRepository colombiaBetRepository;
    private final PagoRepository pagoRepository;
    private final PronosticoCampeonMundialRepository championPredictionRepository;
    private final PartidoMapper partidoMapper;
    private final AuditService auditService;

    public EvidenceExportService(
            PartidoRepository partidoRepository,
            PronosticoGlobalRepository globalPredictionRepository,
            ApuestaColombiaRepository colombiaBetRepository,
            PagoRepository pagoRepository,
            PronosticoCampeonMundialRepository championPredictionRepository,
            PartidoMapper partidoMapper,
            AuditService auditService) {
        this.partidoRepository = partidoRepository;
        this.globalPredictionRepository = globalPredictionRepository;
        this.colombiaBetRepository = colombiaBetRepository;
        this.pagoRepository = pagoRepository;
        this.championPredictionRepository = championPredictionRepository;
        this.partidoMapper = partidoMapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public byte[] exportDailyEvidence(LocalDate date, boolean includePayments, boolean includeChampion, Usuario admin) {
        OffsetDateTime from = date.atStartOfDay(BOGOTA_ZONE).toOffsetDateTime();
        OffsetDateTime to = date.plusDays(1).atStartOfDay(BOGOTA_ZONE).toOffsetDateTime();
        List<Partido> matches = partidoRepository.findByKickoffAtUtcBetweenWithTeams(from, to);
        if (matches.isEmpty()) {
            throw new ReglaNegocioException("No hay partidos registrados para la fecha seleccionada.");
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            CellStyle headerStyle = headerStyle(workbook);
            createMatchesSheet(workbook, headerStyle, matches);
            createGlobalPredictionsSheet(workbook, headerStyle, globalPredictionRepository.findByMatchInOrderByRegisteredAtDesc(matches));
            createColombiaBetsSheet(workbook, headerStyle, colombiaBetRepository.findByMatchInOrderByRegisteredAtDesc(matches));
            if (includePayments) {
                createPaymentsSheet(workbook, headerStyle, pagoRepository.findAll());
            }
            if (includeChampion) {
                createChampionSheet(workbook, headerStyle, championPredictionRepository.findAllByOrderByUpdatedAtDesc());
            }
            createMetadataSheet(workbook, headerStyle, date);

            workbook.write(output);
            auditService.record(
                    admin,
                    "EVIDENCE_EXPORT",
                    "DAILY_EVIDENCE",
                    date.toString(),
                    "Exporto evidencia de partidos del dia " + date,
                    "Excel de evidencia generado");
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible generar el Excel de evidencia.", exception);
        }
    }

    private void createMatchesSheet(Workbook workbook, CellStyle headerStyle, List<Partido> matches) {
        Sheet sheet = createSheet(workbook, headerStyle, "Partidos del dia", "Fecha", "Hora", "Grupo/Fase", "Equipo local", "Equipo visitante", "Estado", "Resultado");
        int rowIndex = 1;
        for (Partido match : matches) {
            Row row = sheet.createRow(rowIndex++);
            var dto = partidoMapper.toDto(match);
            row.createCell(0).setCellValue(formatDate(match.getKickoffAtUtc()));
            row.createCell(1).setCellValue(formatTime(match.getKickoffAtUtc()));
            row.createCell(2).setCellValue(firstNonBlank(match.getGroupName(), match.getRoundName(), match.getStage()));
            row.createCell(3).setCellValue(dto.homeTeam().displayName());
            row.createCell(4).setCellValue(dto.awayTeam().displayName());
            row.createCell(5).setCellValue(match.getStatus().name());
            row.createCell(6).setCellValue(dto.result() == null ? "" : dto.result().homeGoals90() + " - " + dto.result().awayGoals90());
        }
        autoSize(sheet, 7);
    }

    private void createGlobalPredictionsSheet(Workbook workbook, CellStyle headerStyle, List<PronosticoGlobal> predictions) {
        Sheet sheet = createSheet(workbook, headerStyle, "Pronosticos globales", "Usuario", "Partido", "Equipo local", "Equipo visitante", "Pronostico", "Fecha de registro", "Ultima modificacion", "Estado", "Origen");
        int rowIndex = 1;
        for (PronosticoGlobal prediction : predictions) {
            Row row = sheet.createRow(rowIndex++);
            var match = partidoMapper.toDto(prediction.getMatch());
            row.createCell(0).setCellValue(prediction.getUser().getFullName());
            row.createCell(1).setCellValue(match.homeTeam().displayName() + " vs " + match.awayTeam().displayName());
            row.createCell(2).setCellValue(match.homeTeam().displayName());
            row.createCell(3).setCellValue(match.awayTeam().displayName());
            row.createCell(4).setCellValue(score(prediction.getPredictedHomeGoals(), prediction.getPredictedAwayGoals()));
            row.createCell(5).setCellValue(formatDateTime(prediction.getRegisteredAt()));
            row.createCell(6).setCellValue(formatDateTime(prediction.getUpdatedAt()));
            row.createCell(7).setCellValue(prediction.getStatus().name());
            row.createCell(8).setCellValue(prediction.getEntryOrigin().name());
        }
        autoSize(sheet, 9);
    }

    private void createColombiaBetsSheet(Workbook workbook, CellStyle headerStyle, List<ApuestaColombia> bets) {
        Sheet sheet = createSheet(workbook, headerStyle, "Apuestas Colombia", "Usuario", "Partido Colombia", "Apuesta", "Principal para Polla Global", "Fecha de registro", "Ultima modificacion", "Estado", "Pago", "Origen");
        int rowIndex = 1;
        for (ApuestaColombia bet : bets) {
            Row row = sheet.createRow(rowIndex++);
            var match = partidoMapper.toDto(bet.getMatch());
            row.createCell(0).setCellValue(bet.getUser().getFullName());
            row.createCell(1).setCellValue(match.homeTeam().displayName() + " vs " + match.awayTeam().displayName());
            row.createCell(2).setCellValue(score(bet.getPredictedHomeGoals(), bet.getPredictedAwayGoals()));
            row.createCell(3).setCellValue(bet.isPrincipalGlobalPrediction() ? "Si" : "No");
            row.createCell(4).setCellValue(formatDateTime(bet.getRegisteredAt()));
            row.createCell(5).setCellValue(formatDateTime(bet.getUpdatedAt()));
            row.createCell(6).setCellValue(bet.getStatus().name());
            row.createCell(7).setCellValue(bet.getPaymentStatus().name());
            row.createCell(8).setCellValue(bet.getEntryOrigin().name());
        }
        autoSize(sheet, 9);
    }

    private void createPaymentsSheet(Workbook workbook, CellStyle headerStyle, List<Pago> payments) {
        Sheet sheet = createSheet(workbook, headerStyle, "Pagos", "Usuario", "Concepto", "Valor", "Estado", "Fecha aprobacion");
        int rowIndex = 1;
        for (Pago payment : payments) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(payment.getUser().getFullName());
            row.createCell(1).setCellValue(payment.getSystem().name());
            row.createCell(2).setCellValue(payment.getAmountCop().doubleValue());
            row.createCell(3).setCellValue(payment.getStatus().name());
            row.createCell(4).setCellValue(formatDateTime(payment.getPaidAt()));
        }
        autoSize(sheet, 5);
    }

    private void createChampionSheet(Workbook workbook, CellStyle headerStyle, List<PronosticoCampeonMundial> predictions) {
        Sheet sheet = createSheet(workbook, headerStyle, "Campeon mundial", "Usuario", "Seleccion campeona", "Fecha seleccion", "Estado", "Puntos");
        int rowIndex = 1;
        for (PronosticoCampeonMundial prediction : predictions) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(prediction.getUser().getFullName());
            row.createCell(1).setCellValue(prediction.getTeam().getName());
            row.createCell(2).setCellValue(formatDateTime(prediction.getCreatedAt()));
            row.createCell(3).setCellValue(prediction.getStatus().name());
            row.createCell(4).setCellValue(prediction.getPoints());
        }
        autoSize(sheet, 5);
    }

    private void createMetadataSheet(Workbook workbook, CellStyle headerStyle, LocalDate date) {
        Sheet sheet = createSheet(workbook, headerStyle, "Evidencia", "Campo", "Valor");
        sheet.createRow(1).createCell(0).setCellValue("Fecha de partidos");
        sheet.getRow(1).createCell(1).setCellValue(date.toString());
        sheet.createRow(2).createCell(0).setCellValue("Generado en");
        sheet.getRow(2).createCell(1).setCellValue(formatDateTime(OffsetDateTime.now(BOGOTA_ZONE)));
        sheet.createRow(3).createCell(0).setCellValue("Observacion");
        sheet.getRow(3).createCell(1).setCellValue("FamiCup es un MVP familiar; este Excel funciona como respaldo operativo.");
        autoSize(sheet, 2);
    }

    private Sheet createSheet(Workbook workbook, CellStyle headerStyle, String name, String... headers) {
        Sheet sheet = workbook.createSheet(name);
        Row header = sheet.createRow(0);
        for (int index = 0; index < headers.length; index++) {
            header.createCell(index).setCellValue(headers[index]);
            header.getCell(index).setCellStyle(headerStyle);
        }
        return sheet;
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        return headerStyle;
    }

    private void autoSize(Sheet sheet, int columnCount) {
        for (int index = 0; index < columnCount; index++) {
            sheet.autoSizeColumn(index);
        }
    }

    private String score(Integer homeGoals, Integer awayGoals) {
        return homeGoals + " - " + awayGoals;
    }

    private String formatDateTime(OffsetDateTime value) {
        return value == null ? "" : DATE_TIME_FORMAT.format(value.atZoneSameInstant(BOGOTA_ZONE));
    }

    private String formatDate(OffsetDateTime value) {
        return value == null ? "" : DateTimeFormatter.ISO_LOCAL_DATE.format(value.atZoneSameInstant(BOGOTA_ZONE));
    }

    private String formatTime(OffsetDateTime value) {
        return value == null ? "" : DateTimeFormatter.ofPattern("HH:mm").format(value.atZoneSameInstant(BOGOTA_ZONE));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}

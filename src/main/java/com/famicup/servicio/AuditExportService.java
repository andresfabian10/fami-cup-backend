package com.famicup.servicio;

import com.famicup.modelo.dto.AuditEventResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class AuditExportService {

    private static final String[] HEADERS = {
            "Fecha",
            "Usuario",
            "Rol",
            "Accion",
            "Entidad",
            "ID entidad",
            "Resumen solicitud",
            "Resumen respuesta",
            "IP",
            "User-Agent"
    };

    public byte[] toExcel(List<AuditEventResponse> entries) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Auditoria");
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
                header.getCell(i).setCellStyle(headerStyle);
            }

            for (int index = 0; index < entries.size(); index++) {
                AuditEventResponse entry = entries.get(index);
                Row row = sheet.createRow(index + 1);
                row.createCell(0).setCellValue(entry.createdAt() == null ? "" : DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(entry.createdAt()));
                row.createCell(1).setCellValue(nullToEmpty(entry.username()));
                row.createCell(2).setCellValue(nullToEmpty(entry.role()));
                row.createCell(3).setCellValue(entry.action());
                row.createCell(4).setCellValue(entry.entityType());
                row.createCell(5).setCellValue(nullToEmpty(entry.entityId()));
                row.createCell(6).setCellValue(nullToEmpty(entry.requestSummary()));
                row.createCell(7).setCellValue(nullToEmpty(entry.responseSummary()));
                row.createCell(8).setCellValue(nullToEmpty(entry.ipAddress()));
                row.createCell(9).setCellValue(nullToEmpty(entry.userAgent()));
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible generar el Excel de auditoria.", exception);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

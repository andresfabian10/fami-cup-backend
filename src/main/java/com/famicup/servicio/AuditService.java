package com.famicup.servicio;

import com.famicup.modelo.dto.AuditEventResponse;
import com.famicup.modelo.entidad.AuditEvent;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.repositorio.AuditEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            Usuario user,
            String action,
            String entityType,
            String entityId,
            String requestSummary,
            String responseSummary) {
        AuditEvent event = new AuditEvent();
        if (user != null) {
            event.setUserId(user.getId());
            event.setUsername(user.getUsername());
            event.setRole(user.getRole() == null ? null : user.getRole().name());
        }
        event.setAction(action);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setRequestSummary(safeSummary(requestSummary));
        event.setResponseSummary(safeSummary(responseSummary));

        HttpServletRequest request = currentRequest();
        if (request != null) {
            event.setIpAddress(clientIp(request));
            event.setUserAgent(safeSummary(request.getHeader("User-Agent"), 500));
        }

        auditEventRepository.save(event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAnonymous(
            String username,
            String action,
            String entityType,
            String entityId,
            String requestSummary,
            String responseSummary) {
        AuditEvent event = new AuditEvent();
        event.setUsername(username);
        event.setAction(action);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setRequestSummary(safeSummary(requestSummary));
        event.setResponseSummary(safeSummary(responseSummary));
        HttpServletRequest request = currentRequest();
        if (request != null) {
            event.setIpAddress(clientIp(request));
            event.setUserAgent(safeSummary(request.getHeader("User-Agent"), 500));
        }
        auditEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> list(String username, String action, String entityType, OffsetDateTime from, OffsetDateTime to) {
        String usernameFilter = blankToNull(username);
        String actionFilter = blankToNull(action);
        String entityFilter = blankToNull(entityType);
        return auditEventRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(event -> usernameFilter == null || containsIgnoreCase(event.getUsername(), usernameFilter))
                .filter(event -> actionFilter == null || actionFilter.equals(event.getAction()))
                .filter(event -> entityFilter == null || entityFilter.equals(event.getEntityType()))
                .filter(event -> from == null || !event.getCreatedAt().isBefore(from))
                .filter(event -> to == null || !event.getCreatedAt().isAfter(to))
                .map(this::toResponse)
                .toList();
    }

    private AuditEventResponse toResponse(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getUserId(),
                event.getUsername(),
                event.getRole(),
                event.getAction(),
                event.getEntityType(),
                event.getEntityId(),
                event.getRequestSummary(),
                event.getResponseSummary(),
                event.getIpAddress(),
                event.getUserAgent(),
                event.getCreatedAt());
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String safeSummary(String value) {
        return safeSummary(value, 1000);
    }

    private String safeSummary(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String sanitized = value
                .replaceAll("(?i)password\\s*[:=]\\s*[^,;}\\s]+", "password=***")
                .replaceAll("(?i)token\\s*[:=]\\s*[^,;}\\s]+", "token=***")
                .replaceAll("(?i)api[-_ ]?key\\s*[:=]\\s*[^,;}\\s]+", "apiKey=***");
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean containsIgnoreCase(String value, String filter) {
        return value != null && value.toLowerCase(java.util.Locale.ROOT).contains(filter.toLowerCase(java.util.Locale.ROOT));
    }
}

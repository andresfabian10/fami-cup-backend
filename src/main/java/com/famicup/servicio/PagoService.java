package com.famicup.servicio;

import com.famicup.excepcion.RecursoNoEncontradoException;
import com.famicup.modelo.dto.PagoResponse;
import com.famicup.modelo.entidad.ApuestaColombia;
import com.famicup.modelo.entidad.Pago;
import com.famicup.modelo.entidad.Usuario;
import com.famicup.modelo.enumeracion.EstadoApuestaColombia;
import com.famicup.modelo.enumeracion.EstadoPago;
import com.famicup.modelo.mapper.PagoMapper;
import com.famicup.repositorio.PagoRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PagoService {

    private final PagoRepository pagoRepository;
    private final PagoMapper pagoMapper;
    private final UsuarioService usuarioService;
    private final PartidoService partidoService;
    private final AuditService auditService;

    public PagoService(
            PagoRepository pagoRepository,
            PagoMapper pagoMapper,
            UsuarioService usuarioService,
            PartidoService partidoService,
            AuditService auditService) {
        this.pagoRepository = pagoRepository;
        this.pagoMapper = pagoMapper;
        this.usuarioService = usuarioService;
        this.partidoService = partidoService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PagoResponse> listPayments() {
        return pagoRepository.findAll().stream()
                .map(pagoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PagoResponse> listPaymentsByUser(Usuario user) {
        return pagoRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(pagoMapper::toResponse)
                .toList();
    }

    @Transactional
    public PagoResponse markPaid(UUID paymentId, Usuario admin) {
        Pago payment = getPayment(paymentId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        payment.setStatus(EstadoPago.PAID);
        payment.setPaidAt(now);
        payment.setConfirmedBy(admin);
        applyPaymentToColombiaBet(payment, now);
        auditService.record(
                admin,
                "PAYMENT_MARK_PAID",
                "PAYMENT",
                payment.getId().toString(),
                "Confirmo pago " + payment.getSystem() + " de " + payment.getUser().getUsername(),
                "Pago marcado como pagado");
        return pagoMapper.toResponse(payment);
    }

    @Transactional
    public PagoResponse markPending(UUID paymentId) {
        Pago payment = getPayment(paymentId);
        payment.setStatus(EstadoPago.PENDING);
        payment.setPaidAt(null);
        payment.setConfirmedBy(null);
        ApuestaColombia bet = payment.getColombiaBet();
        if (bet != null) {
            bet.setPaymentStatus(EstadoPago.PENDING);
            bet.setStatus(EstadoApuestaColombia.PENDING_PAYMENT);
            bet.setValid(false);
        }
        auditService.record(
                null,
                "PAYMENT_MARK_PENDING",
                "PAYMENT",
                payment.getId().toString(),
                "Marco pago como pendiente: " + payment.getSystem() + " de " + payment.getUser().getUsername(),
                "Pago marcado como pendiente");
        return pagoMapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalPaid() {
        return pagoRepository.totalPaid();
    }

    private Pago getPayment(UUID paymentId) {
        return pagoRepository.findById(paymentId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado."));
    }

    private void applyPaymentToColombiaBet(Pago payment, OffsetDateTime now) {
        ApuestaColombia bet = payment.getColombiaBet();
        if (bet == null) {
            return;
        }
        bet.setPaymentStatus(EstadoPago.PAID);
        if (partidoService.isClosedForBetting(bet.getMatch(), now)) {
            bet.setStatus(EstadoApuestaColombia.ANNULLED);
            bet.setValid(false);
        } else {
            bet.setStatus(EstadoApuestaColombia.VALID);
            bet.setValid(true);
        }
    }
}

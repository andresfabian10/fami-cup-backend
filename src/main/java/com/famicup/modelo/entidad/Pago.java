package com.famicup.modelo.entidad;

import com.famicup.modelo.enumeracion.EstadoPago;
import com.famicup.modelo.enumeracion.SistemaPago;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payments")
public class Pago extends Auditable {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Usuario user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SistemaPago system;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Partido match;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colombia_bet_id")
    private ApuestaColombia colombiaBet;

    @Column(name = "amount_cop", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountCop;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago status = EstadoPago.PENDING;

    @Column(name = "payment_method", length = 80)
    private String paymentMethod;

    @Column(length = 160)
    private String reference;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private Usuario confirmedBy;
}

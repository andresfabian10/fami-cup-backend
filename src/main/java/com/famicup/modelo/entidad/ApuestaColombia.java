package com.famicup.modelo.entidad;

import com.famicup.modelo.enumeracion.EstadoApuestaColombia;
import com.famicup.modelo.enumeracion.EstadoPago;
import com.famicup.modelo.enumeracion.OrigenRegistro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "colombia_bets")
public class ApuestaColombia extends Auditable {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Usuario user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Partido match;

    @Column(name = "predicted_home_goals", nullable = false)
    private Integer predictedHomeGoals;

    @Column(name = "predicted_away_goals", nullable = false)
    private Integer predictedAwayGoals;

    @Column(name = "amount_cop", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountCop;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoApuestaColombia status = EstadoApuestaColombia.PENDING_PAYMENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private EstadoPago paymentStatus = EstadoPago.PENDING;

    @Column(nullable = false)
    private boolean valid;

    @Column(name = "principal_global_prediction", nullable = false)
    private boolean principalGlobalPrediction;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_origin", nullable = false, length = 20)
    private OrigenRegistro entryOrigin = OrigenRegistro.PLAYER;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_admin_id")
    private Usuario createdByAdmin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_admin_id")
    private Usuario updatedByAdmin;

    @Column(name = "registered_at", nullable = false)
    private OffsetDateTime registeredAt;

    @Column(name = "prize_amount_cop", nullable = false, precision = 12, scale = 2)
    private BigDecimal prizeAmountCop = BigDecimal.ZERO;
}

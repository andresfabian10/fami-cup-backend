package com.famicup.modelo.entidad;

import com.famicup.modelo.enumeracion.GanadorPartido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "match_results")
public class ResultadoPartido extends Auditable {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false, unique = true)
    private Partido match;

    @Column(name = "home_goals_90", nullable = false)
    private Integer homeGoals90;

    @Column(name = "away_goals_90", nullable = false)
    private Integer awayGoals90;

    @Enumerated(EnumType.STRING)
    @Column(name = "winner_90", nullable = false, length = 20)
    private GanadorPartido winner90;

    @Column(nullable = false, length = 80)
    private String source = "API_FOOTBALL";

    @Column(name = "confirmed_at", nullable = false)
    private OffsetDateTime confirmedAt;
}

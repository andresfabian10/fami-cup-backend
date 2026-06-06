package com.famicup.modelo.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
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
@Table(name = "ranking_points")
public class PuntosRanking {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private Usuario user;

    @Column(name = "total_points", nullable = false)
    private Integer totalPoints = 0;

    @Column(name = "exact_hits", nullable = false)
    private Integer exactHits = 0;

    @Column(name = "winner_hits", nullable = false)
    private Integer winnerHits = 0;

    @Column(name = "colombia_points", nullable = false)
    private Integer colombiaPoints = 0;

    @Column(name = "predicted_matches", nullable = false)
    private Integer predictedMatches = 0;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}

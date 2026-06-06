package com.famicup.modelo.entidad;

import com.famicup.modelo.enumeracion.EstadoPartido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "matches")
public class Partido extends Auditable {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id")
    private Competicion competition;

    @Column(length = 120)
    private String stage;

    @Column(name = "group_name", length = 80)
    private String groupName;

    @Column(name = "round_name", length = 120)
    private String roundName;

    @Column(name = "kickoff_at_utc", nullable = false)
    private OffsetDateTime kickoffAtUtc;

    @Column(length = 80)
    private String timezone = "UTC";

    @Column(name = "venue_name", length = 160)
    private String venueName;

    @Column(name = "venue_city", length = 120)
    private String venueCity;

    @Column(name = "venue_country", length = 120)
    private String venueCountry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "home_team_code", nullable = false)
    private Equipo homeTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "away_team_code", nullable = false)
    private Equipo awayTeam;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoPartido status = EstadoPartido.SCHEDULED;

    @Column(name = "api_status_short", length = 10)
    private String apiStatusShort;

    @Column(name = "api_status_long", length = 80)
    private String apiStatusLong;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;
}

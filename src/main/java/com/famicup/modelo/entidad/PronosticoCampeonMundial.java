package com.famicup.modelo.entidad;

import com.famicup.modelo.enumeracion.EstadoCampeonMundial;
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
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "world_champion_predictions")
public class PronosticoCampeonMundial extends Auditable {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Usuario user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_code", nullable = false)
    private Equipo team;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoCampeonMundial status = EstadoCampeonMundial.VALID;

    @Column(nullable = false)
    private Integer points = 0;
}

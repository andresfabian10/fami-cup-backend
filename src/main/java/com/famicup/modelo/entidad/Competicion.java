package com.famicup.modelo.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "competitions")
public class Competicion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_football_league_id", unique = true)
    private Integer apiFootballLeagueId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false)
    private Integer season;

    @Column(length = 80)
    private String type;

    @Column(length = 120)
    private String country;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;
}

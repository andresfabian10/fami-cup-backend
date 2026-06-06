package com.famicup.modelo.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "teams")
public class Equipo extends Auditable {

    @Id
    @Column(name = "fifa_code", length = 10)
    private String fifaCode;

    @Column(name = "api_football_id", unique = true)
    private Integer apiFootballId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 120)
    private String country;

    @Column(length = 80)
    private String confederation;

    @Column(name = "flag_url", length = 500)
    private String flagUrl;
}

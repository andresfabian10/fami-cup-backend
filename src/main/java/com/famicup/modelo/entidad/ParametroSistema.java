package com.famicup.modelo.entidad;

import com.famicup.modelo.enumeracion.TipoValorParametro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "system_parameters")
public class ParametroSistema extends Auditable {

    @Id
    @Column(name = "parameter_key", length = 80)
    private String parameterKey;

    @Column(name = "parameter_value", nullable = false, length = 250)
    private String parameterValue;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 30)
    private TipoValorParametro valueType;
}

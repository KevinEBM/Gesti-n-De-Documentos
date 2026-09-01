package com.plantarsas.gestiondocumental.usuarios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UsuarioAreaId implements Serializable {

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "area_id")
    private Long areaId;
}

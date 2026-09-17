package com.plantarsas.gestiondocumental.usuarios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Identificador compuesto de la relación entre un usuario y un área
 * (la pareja usuario-área), necesario porque esa relación no tiene un
 * id propio en la base de datos.
 */
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

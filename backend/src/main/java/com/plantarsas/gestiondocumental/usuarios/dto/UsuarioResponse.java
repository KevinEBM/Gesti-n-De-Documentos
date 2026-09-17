package com.plantarsas.gestiondocumental.usuarios.dto;

import com.plantarsas.gestiondocumental.areas.dto.AreaResponse;
import com.plantarsas.gestiondocumental.roles.dto.RolResponse;
import com.plantarsas.gestiondocumental.usuarios.entity.EstadoUsuario;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Datos de un usuario que se le muestran al administrador: su
 * información personal, su rol, su estado de cuenta y las áreas a las
 * que pertenece.
 */
public record UsuarioResponse(
        Long id,
        String nombres,
        String apellidos,
        String correo,
        RolResponse rol,
        EstadoUsuario estado,
        List<AreaResponse> areas,
        Long areaPrincipalId,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}

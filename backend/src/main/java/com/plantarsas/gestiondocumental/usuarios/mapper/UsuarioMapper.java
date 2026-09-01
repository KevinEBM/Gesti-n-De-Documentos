package com.plantarsas.gestiondocumental.usuarios.mapper;

import com.plantarsas.gestiondocumental.areas.dto.AreaResponse;
import com.plantarsas.gestiondocumental.areas.mapper.AreaMapper;
import com.plantarsas.gestiondocumental.roles.dto.RolResponse;
import com.plantarsas.gestiondocumental.roles.entity.Rol;
import com.plantarsas.gestiondocumental.usuarios.dto.UsuarioResponse;
import com.plantarsas.gestiondocumental.usuarios.entity.Usuario;
import com.plantarsas.gestiondocumental.usuarios.entity.UsuarioArea;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UsuarioMapper {

    private final AreaMapper areaMapper;

    public UsuarioResponse toResponse(Usuario usuario, List<UsuarioArea> asignaciones) {
        List<AreaResponse> areas = asignaciones.stream()
                .map(UsuarioArea::getArea)
                .map(areaMapper::toResponse)
                .sorted(Comparator.comparing(AreaResponse::nombre))
                .toList();

        Long areaPrincipalId = asignaciones.stream()
                .filter(UsuarioArea::isEsPrincipal)
                .map(ua -> ua.getArea().getId())
                .findFirst()
                .orElse(null);

        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getCorreo(),
                toRolResponse(usuario.getRol()),
                usuario.getEstado(),
                areas,
                areaPrincipalId,
                usuario.getFechaCreacion(),
                usuario.getFechaActualizacion()
        );
    }

    private RolResponse toRolResponse(Rol rol) {
        return new RolResponse(rol.getId(), rol.getNombre(), rol.getDescripcion(), rol.isActivo());
    }
}

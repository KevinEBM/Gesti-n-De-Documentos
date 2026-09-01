package com.plantarsas.gestiondocumental.security;

import com.plantarsas.gestiondocumental.exception.UnauthorizedException;
import com.plantarsas.gestiondocumental.shared.enums.RolEnum;
import com.plantarsas.gestiondocumental.usuarios.entity.UsuarioArea;
import com.plantarsas.gestiondocumental.usuarios.repository.UsuarioAreaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioAreaAutorizacionService {

    private final UsuarioAreaRepository usuarioAreaRepository;

    @Transactional(readOnly = true)
    public Set<Long> obtenerAreaIdsAutorizadas(AuthenticatedUser usuario) {
        List<UsuarioArea> asignaciones = usuarioAreaRepository.findByUsuario_Id(usuario.id());

        return switch (usuario.rol()) {
            case ADMINISTRATIVO, JEFE_AREA -> asignaciones.stream()
                    .filter(UsuarioArea::isEsPrincipal)
                    .map(asignacion -> asignacion.getArea().getId())
                    .collect(Collectors.toSet());
            case ADMINISTRADOR -> Set.of();
        };
    }

    public boolean esAdministrador(AuthenticatedUser usuario) {
        return usuario.rol() == RolEnum.ADMINISTRADOR;
    }

    public void validarAccesoArea(AuthenticatedUser usuario, Long areaId) {
        if (esAdministrador(usuario)) {
            return;
        }
        Set<Long> areaIds = obtenerAreaIdsAutorizadas(usuario);
        if (!areaIds.contains(areaId)) {
            throw new UnauthorizedException("No tiene permisos para consultar el área solicitada");
        }
    }
}

package com.plantarsas.gestiondocumental.usuarios.service;

import com.plantarsas.gestiondocumental.areas.entity.Area;
import com.plantarsas.gestiondocumental.areas.service.AreaLookupService;
import com.plantarsas.gestiondocumental.exception.BusinessException;
import com.plantarsas.gestiondocumental.exception.ResourceNotFoundException;
import com.plantarsas.gestiondocumental.roles.entity.Rol;
import com.plantarsas.gestiondocumental.roles.service.RolLookupService;
import com.plantarsas.gestiondocumental.shared.enums.RolEnum;
import com.plantarsas.gestiondocumental.usuarios.dto.UsuarioEstadoRequest;
import com.plantarsas.gestiondocumental.usuarios.dto.UsuarioRequest;
import com.plantarsas.gestiondocumental.usuarios.dto.UsuarioResponse;
import com.plantarsas.gestiondocumental.usuarios.dto.UsuarioUpdateRequest;
import com.plantarsas.gestiondocumental.usuarios.entity.Usuario;
import com.plantarsas.gestiondocumental.usuarios.entity.UsuarioArea;
import com.plantarsas.gestiondocumental.usuarios.mapper.UsuarioMapper;
import com.plantarsas.gestiondocumental.usuarios.repository.UsuarioAreaRepository;
import com.plantarsas.gestiondocumental.usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioAreaRepository usuarioAreaRepository;
    private final RolLookupService rolLookupService;
    private final AreaLookupService areaLookupService;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        String correoNormalizado = normalizarCorreo(request.correo());
        if (usuarioRepository.existsByCorreoIgnoreCase(correoNormalizado)) {
            throw new BusinessException(
                    "Ya existe un usuario con el correo '" + correoNormalizado + "'",
                    HttpStatus.CONFLICT
            );
        }

        Rol rol = rolLookupService.obtenerActivoPorId(request.rolId());
        validarAsignacionAreasPorRol(rol, request.areaIds(), request.areaPrincipalId());
        List<Area> areas = validarYObtenerAreas(request.areaIds());

        String passwordHash = passwordEncoder.encode(request.password());

        Usuario usuario = new Usuario(
                request.nombres(),
                request.apellidos(),
                request.correo(),
                passwordHash,
                rol
        );
        usuario = usuarioRepository.save(usuario);

        List<UsuarioArea> asignaciones = crearAsignaciones(usuario, areas, request.areaPrincipalId());
        usuarioAreaRepository.saveAll(asignaciones);

        return usuarioMapper.toResponse(usuario, asignaciones);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream()
                .map(usuario -> usuarioMapper.toResponse(
                        usuario,
                        usuarioAreaRepository.findByUsuario_Id(usuario.getId())
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(Long id) {
        Usuario usuario = obtenerEntidadPorId(id);
        List<UsuarioArea> asignaciones = usuarioAreaRepository.findByUsuario_Id(id);
        return usuarioMapper.toResponse(usuario, asignaciones);
    }

    @Override
    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = obtenerEntidadPorId(id);

        String correoNormalizado = normalizarCorreo(request.correo());
        if (usuarioRepository.existsByCorreoIgnoreCaseAndIdNot(correoNormalizado, id)) {
            throw new BusinessException(
                    "Ya existe un usuario con el correo '" + correoNormalizado + "'",
                    HttpStatus.CONFLICT
            );
        }

        Rol rol = rolLookupService.obtenerActivoPorId(request.rolId());
        validarAsignacionAreasPorRol(rol, request.areaIds(), request.areaPrincipalId());
        List<Area> areas = validarYObtenerAreas(request.areaIds());

        usuario.actualizarDatos(request.nombres(), request.apellidos(), request.correo());
        usuario.cambiarRol(rol);

        usuarioAreaRepository.deleteByUsuarioId(id);
        List<UsuarioArea> nuevasAsignaciones = crearAsignaciones(usuario, areas, request.areaPrincipalId());
        usuarioAreaRepository.saveAll(nuevasAsignaciones);

        return usuarioMapper.toResponse(usuario, nuevasAsignaciones);
    }

    @Override
    @Transactional
    public UsuarioResponse cambiarEstado(Long id, UsuarioEstadoRequest request) {
        Usuario usuario = obtenerEntidadPorId(id);
        usuario.cambiarEstado(request.estado());
        List<UsuarioArea> asignaciones = usuarioAreaRepository.findByUsuario_Id(id);
        return usuarioMapper.toResponse(usuario, asignaciones);
    }

    @Override
    @Transactional
    public void cambiarContrasena(Long usuarioId, String contrasenaActual, String nuevaContrasena) {
        Usuario usuario = obtenerEntidadPorId(usuarioId);

        if (!usuario.coincideConPassword(contrasenaActual, passwordEncoder)) {
            throw new BusinessException("La contraseña actual es incorrecta.");
        }

        if (usuario.coincideConPassword(nuevaContrasena, passwordEncoder)) {
            throw new BusinessException("La nueva contraseña debe ser diferente a la actual");
        }

        usuario.actualizarPassword(passwordEncoder.encode(nuevaContrasena));
    }

    private Usuario obtenerEntidadPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un usuario con id " + id
                ));
    }

    private List<Area> validarYObtenerAreas(Set<Long> areaIds) {
        return areaIds.stream()
                .map(areaLookupService::obtenerActivaPorId)
                .toList();
    }

    private void validarAsignacionAreasPorRol(Rol rol, Set<Long> areaIds, Long areaPrincipalId) {
        if (requiereUnicaArea(rol)) {
            if (areaIds == null || areaIds.size() != 1) {
                throw new BusinessException(
                        "El rol '" + rol.getNombre() + "' requiere exactamente un área asignada"
                );
            }
            if (areaPrincipalId == null) {
                throw new BusinessException(
                        "El rol '" + rol.getNombre() + "' requiere exactamente un área principal"
                );
            }
            if (!areaIds.contains(areaPrincipalId)) {
                throw new BusinessException(
                        "El área principal debe estar incluida en las áreas asignadas"
                );
            }
            return;
        }

        if (areaPrincipalId != null && (areaIds == null || !areaIds.contains(areaPrincipalId))) {
            throw new BusinessException(
                    "El área principal debe estar incluida en las áreas asignadas"
            );
        }
    }

    private boolean requiereUnicaArea(Rol rol) {
        return rol.getNombre() == RolEnum.JEFE_AREA
                || rol.getNombre() == RolEnum.ADMINISTRATIVO;
    }

    private List<UsuarioArea> crearAsignaciones(Usuario usuario, List<Area> areas, Long areaPrincipalId) {
        return areas.stream()
                .map(area -> new UsuarioArea(usuario, area, area.getId().equals(areaPrincipalId)))
                .toList();
    }

    private String normalizarCorreo(String correo) {
        return correo == null ? null : correo.trim().toLowerCase(Locale.ROOT);
    }
}

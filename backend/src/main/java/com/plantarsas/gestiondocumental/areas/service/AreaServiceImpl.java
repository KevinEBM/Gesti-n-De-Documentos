package com.plantarsas.gestiondocumental.areas.service;

import com.plantarsas.gestiondocumental.areas.dto.AreaEstadoRequest;
import com.plantarsas.gestiondocumental.areas.dto.AreaRequest;
import com.plantarsas.gestiondocumental.areas.dto.AreaResponse;
import com.plantarsas.gestiondocumental.areas.entity.Area;
import com.plantarsas.gestiondocumental.areas.mapper.AreaMapper;
import com.plantarsas.gestiondocumental.areas.repository.AreaRepository;
import com.plantarsas.gestiondocumental.exception.BusinessException;
import com.plantarsas.gestiondocumental.exception.ResourceNotFoundException;
import com.plantarsas.gestiondocumental.security.AuthenticatedUser;
import com.plantarsas.gestiondocumental.security.UsuarioAreaAutorizacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AreaServiceImpl implements AreaService, AreaLookupService {

    private final AreaRepository areaRepository;
    private final AreaMapper areaMapper;
    private final UsuarioAreaAutorizacionService usuarioAreaAutorizacionService;

    @Override
    @Transactional
    public AreaResponse crear(AreaRequest request) {
        Area area = new Area(request.codigo(), request.nombre(), request.descripcion());
        validarDuplicados(area.getCodigo(), area.getNombre(), null);
        return areaMapper.toResponse(areaRepository.save(area));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AreaResponse> listar() {
        return areaRepository.findAll().stream()
                .map(areaMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AreaResponse> listarParaUsuario(AuthenticatedUser usuario) {
        if (usuarioAreaAutorizacionService.esAdministrador(usuario)) {
            return listar();
        }

        var areaIds = usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(usuario);
        if (areaIds.isEmpty()) {
            return List.of();
        }

        return areaRepository.findByIdIn(areaIds).stream()
                .map(areaMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AreaResponse obtenerPorId(Long id) {
        return areaMapper.toResponse(obtenerEntidadPorId(id));
    }

    @Override
    @Transactional
    public AreaResponse actualizar(Long id, AreaRequest request) {
        Area area = obtenerEntidadPorId(id);

        String codigoNormalizado = request.codigo().trim();
        String nombreNormalizado = request.nombre().trim();

        validarDuplicados(codigoNormalizado, nombreNormalizado, id);

        area.actualizarDatos(
                request.codigo(),
                request.nombre(),
                request.descripcion()
        );

        return areaMapper.toResponse(area);
    }

    @Override
    @Transactional
    public AreaResponse cambiarEstado(Long id, AreaEstadoRequest request) {
        Area area = obtenerEntidadPorId(id);
        if (Boolean.TRUE.equals(request.activo())) {
            area.activar();
        } else {
            area.desactivar();
        }
        return areaMapper.toResponse(area);
    }

    @Override
    @Transactional(readOnly = true)
    public Area obtenerActivaPorId(Long id) {
        Area area = obtenerEntidadPorId(id);
        if (!area.isActivo()) {
            throw new BusinessException("El área '" + area.getNombre() + "' está inactiva y no puede utilizarse");
        }
        return area;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Area> obtenerActivasPorIds(List<Long> ids) {
        List<Area> encontradas = areaRepository.findByIdIn(ids);
        Map<Long, Area> porId = encontradas.stream()
                .collect(Collectors.toMap(Area::getId, area -> area));

        List<Long> inexistentes = ids.stream()
                .filter(id -> !porId.containsKey(id))
                .toList();
        if (!inexistentes.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No existen áreas con id " + inexistentes
            );
        }

        List<Area> inactivas = encontradas.stream()
                .filter(area -> !area.isActivo())
                .toList();
        if (!inactivas.isEmpty()) {
            String nombres = inactivas.stream()
                    .map(Area::getNombre)
                    .collect(Collectors.joining(", "));
            throw new BusinessException(
                    "Las siguientes áreas están inactivas y no pueden utilizarse: " + nombres
            );
        }

        return ids.stream()
                .map(porId::get)
                .toList();
    }

    private Area obtenerEntidadPorId(Long id) {
        return areaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un área con id " + id
                ));
    }

    private void validarDuplicados(String codigo, String nombre, Long idExcluido) {
        boolean codigoDuplicado = idExcluido == null
                ? areaRepository.existsByCodigoIgnoreCase(codigo)
                : areaRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, idExcluido);
        if (codigoDuplicado) {
            throw new BusinessException(
                    "Ya existe un área con el código '" + codigo + "'",
                    HttpStatus.CONFLICT
            );
        }

        boolean nombreDuplicado = idExcluido == null
                ? areaRepository.existsByNombreIgnoreCase(nombre)
                : areaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, idExcluido);
        if (nombreDuplicado) {
            throw new BusinessException(
                    "Ya existe un área con el nombre '" + nombre + "'",
                    HttpStatus.CONFLICT
            );
        }
    }
}

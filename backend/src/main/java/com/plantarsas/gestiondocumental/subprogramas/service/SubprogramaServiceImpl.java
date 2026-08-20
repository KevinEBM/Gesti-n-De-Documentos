package com.plantarsas.gestiondocumental.subprogramas.service;

import com.plantarsas.gestiondocumental.areas.entity.Area;
import com.plantarsas.gestiondocumental.areas.service.AreaLookupService;
import com.plantarsas.gestiondocumental.exception.BusinessException;
import com.plantarsas.gestiondocumental.exception.ResourceNotFoundException;
import com.plantarsas.gestiondocumental.security.AuthenticatedUser;
import com.plantarsas.gestiondocumental.security.UsuarioAreaAutorizacionService;
import com.plantarsas.gestiondocumental.subprogramas.dto.SubprogramaEstadoRequest;
import com.plantarsas.gestiondocumental.subprogramas.dto.SubprogramaRequest;
import com.plantarsas.gestiondocumental.subprogramas.dto.SubprogramaResponse;
import com.plantarsas.gestiondocumental.subprogramas.dto.SubprogramaUpdateRequest;
import com.plantarsas.gestiondocumental.subprogramas.entity.Subprograma;
import com.plantarsas.gestiondocumental.subprogramas.mapper.SubprogramaMapper;
import com.plantarsas.gestiondocumental.subprogramas.repository.SubprogramaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubprogramaServiceImpl implements SubprogramaService, SubprogramaLookupService {

    private final SubprogramaRepository subprogramaRepository;
    private final AreaLookupService areaLookupService;
    private final SubprogramaMapper subprogramaMapper;
    private final UsuarioAreaAutorizacionService usuarioAreaAutorizacionService;

    @Override
    @Transactional
    public SubprogramaResponse crear(SubprogramaRequest request) {
        Area area = areaLookupService.obtenerActivaPorId(request.areaId());

        String nombreNormalizado = normalizarTexto(request.nombre());
        if (subprogramaRepository.existsByAreaIdAndNombreIgnoreCase(area.getId(), nombreNormalizado)) {
            throw new BusinessException(
                    "Ya existe un subprograma con el nombre '" + nombreNormalizado + "' en esa área",
                    HttpStatus.CONFLICT
            );
        }

        Subprograma subprograma = new Subprograma(request.nombre(), request.descripcion(), area);
        return subprogramaMapper.toResponse(subprogramaRepository.save(subprograma));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubprogramaResponse> listar() {
        return subprogramaRepository.findAll().stream()
                .map(subprogramaMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubprogramaResponse> listarParaUsuario(AuthenticatedUser usuario) {
        if (usuarioAreaAutorizacionService.esAdministrador(usuario)) {
            return listar();
        }

        var areaIds = usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(usuario);
        if (areaIds.isEmpty()) {
            return List.of();
        }

        return subprogramaRepository.findByArea_IdInOrderByNombreAsc(areaIds).stream()
                .map(subprogramaMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SubprogramaResponse obtenerPorId(Long id) {
        return subprogramaMapper.toResponse(obtenerEntidadPorId(id));
    }

    @Override
    @Transactional
    public SubprogramaResponse actualizar(Long id, SubprogramaUpdateRequest request) {
        Subprograma subprograma = obtenerEntidadPorId(id);

        String nombreNormalizado = normalizarTexto(request.nombre());
        if (subprogramaRepository.existsByAreaIdAndNombreIgnoreCaseAndIdNot(
                subprograma.getArea().getId(), nombreNormalizado, id)) {
            throw new BusinessException(
                    "Ya existe un subprograma con el nombre '" + nombreNormalizado + "' en esa área",
                    HttpStatus.CONFLICT
            );
        }

        subprograma.actualizarDatos(request.nombre(), request.descripcion());
        return subprogramaMapper.toResponse(subprograma);
    }

    @Override
    @Transactional
    public SubprogramaResponse cambiarEstado(Long id, SubprogramaEstadoRequest request) {
        Subprograma subprograma = obtenerEntidadPorId(id);
        if (Boolean.TRUE.equals(request.activo())) {
            subprograma.activar();
        } else {
            subprograma.desactivar();
        }
        return subprogramaMapper.toResponse(subprograma);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubprogramaResponse> listarActivosPorArea(Long areaId, AuthenticatedUser usuario) {
        usuarioAreaAutorizacionService.validarAccesoArea(usuario, areaId);
        areaLookupService.obtenerActivaPorId(areaId);
        return subprogramaRepository.findByAreaIdAndActivoTrueOrderByNombreAsc(areaId).stream()
                .map(subprogramaMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Subprograma obtenerActivoPorId(Long id) {
        Subprograma subprograma = obtenerEntidadPorId(id);
        if (!subprograma.isActivo()) {
            throw new BusinessException(
                    "El subprograma '" + subprograma.getNombre() + "' está inactivo y no puede utilizarse"
            );
        }
        return subprograma;
    }

    private Subprograma obtenerEntidadPorId(Long id) {
        return subprogramaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un subprograma con id " + id
                ));
    }

    private String normalizarTexto(String valor) {
        return valor == null ? null : valor.trim();
    }
}

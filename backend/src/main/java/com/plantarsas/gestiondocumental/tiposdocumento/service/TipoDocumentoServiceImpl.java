package com.plantarsas.gestiondocumental.tiposdocumento.service;

import com.plantarsas.gestiondocumental.exception.BusinessException;
import com.plantarsas.gestiondocumental.exception.ResourceNotFoundException;
import com.plantarsas.gestiondocumental.tiposdocumento.dto.TipoDocumentoEstadoRequest;
import com.plantarsas.gestiondocumental.tiposdocumento.dto.TipoDocumentoRequest;
import com.plantarsas.gestiondocumental.tiposdocumento.dto.TipoDocumentoResponse;
import com.plantarsas.gestiondocumental.tiposdocumento.dto.TipoDocumentoUpdateRequest;
import com.plantarsas.gestiondocumental.tiposdocumento.entity.TipoDocumento;
import com.plantarsas.gestiondocumental.tiposdocumento.mapper.TipoDocumentoMapper;
import com.plantarsas.gestiondocumental.tiposdocumento.repository.TipoDocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TipoDocumentoServiceImpl implements TipoDocumentoService, TipoDocumentoLookupService {

    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final TipoDocumentoMapper tipoDocumentoMapper;

    @Override
    @Transactional
    public TipoDocumentoResponse crear(TipoDocumentoRequest request) {
        String nombreNormalizado = normalizarTexto(request.nombre());
        if (tipoDocumentoRepository.existsByNombreIgnoreCase(nombreNormalizado)) {
            throw new BusinessException(
                    "Ya existe un tipo de documento con el nombre '" + nombreNormalizado + "'",
                    HttpStatus.CONFLICT
            );
        }

        TipoDocumento tipoDocumento = new TipoDocumento(request.codigo(), request.nombre(), request.descripcion());
        return tipoDocumentoMapper.toResponse(tipoDocumentoRepository.save(tipoDocumento));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TipoDocumentoResponse> listar() {
        return tipoDocumentoRepository.findAllByOrderByNombreAsc().stream()
                .map(tipoDocumentoMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TipoDocumentoResponse obtenerPorId(Long id) {
        return tipoDocumentoMapper.toResponse(obtenerEntidadPorId(id));
    }

    @Override
    @Transactional
    public TipoDocumentoResponse actualizar(Long id, TipoDocumentoUpdateRequest request) {
        TipoDocumento tipoDocumento = obtenerEntidadPorId(id);

        String nombreNormalizado = normalizarTexto(request.nombre());
        if (tipoDocumentoRepository.existsByNombreIgnoreCaseAndIdNot(nombreNormalizado, id)) {
            throw new BusinessException(
                    "Ya existe un tipo de documento con el nombre '" + nombreNormalizado + "'",
                    HttpStatus.CONFLICT
            );
        }

        tipoDocumento.actualizarDatos(request.codigo(), request.nombre(), request.descripcion());
        return tipoDocumentoMapper.toResponse(tipoDocumento);
    }

    @Override
    @Transactional
    public TipoDocumentoResponse cambiarEstado(Long id, TipoDocumentoEstadoRequest request) {
        TipoDocumento tipoDocumento = obtenerEntidadPorId(id);
        if (Boolean.TRUE.equals(request.activo())) {
            tipoDocumento.activar();
        } else {
            tipoDocumento.desactivar();
        }
        return tipoDocumentoMapper.toResponse(tipoDocumento);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TipoDocumentoResponse> listarActivos() {
        return tipoDocumentoRepository.findByActivoTrueOrderByNombreAsc().stream()
                .map(tipoDocumentoMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TipoDocumento obtenerEntidadPorId(Long id) {
        return buscarTipoDocumentoPorId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public TipoDocumento obtenerActivoPorId(Long id) {
        TipoDocumento tipoDocumento = buscarTipoDocumentoPorId(id);
        if (!tipoDocumento.isActivo()) {
            throw new BusinessException(
                    "El tipo de documento '" + tipoDocumento.getNombre() + "' está inactivo y no puede utilizarse"
            );
        }
        return tipoDocumento;
    }

    private TipoDocumento buscarTipoDocumentoPorId(Long id) {
        return tipoDocumentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un tipo de documento con id " + id
                ));
    }

    private String normalizarTexto(String valor) {
        return valor == null ? null : valor.trim();
    }
}

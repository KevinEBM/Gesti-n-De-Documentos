package com.plantarsas.gestiondocumental.documentos.service;

import com.plantarsas.gestiondocumental.documentos.dto.DocumentoArchivoDescarga;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoFiltroRequest;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoResponse;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoResumenResponse;
import com.plantarsas.gestiondocumental.documentos.dto.VersionHistoricaResponse;
import com.plantarsas.gestiondocumental.documentos.entity.Documento;
import com.plantarsas.gestiondocumental.documentos.entity.DocumentoArea;
import com.plantarsas.gestiondocumental.documentos.entity.VersionDocumento;
import com.plantarsas.gestiondocumental.documentos.mapper.DocumentoMapper;
import com.plantarsas.gestiondocumental.documentos.repository.DocumentoAreaRepository;
import com.plantarsas.gestiondocumental.documentos.repository.DocumentoRepository;
import com.plantarsas.gestiondocumental.documentos.repository.VersionDocumentoRepository;
import com.plantarsas.gestiondocumental.documentos.specification.DocumentoSpecifications;
import com.plantarsas.gestiondocumental.exception.ResourceNotFoundException;
import com.plantarsas.gestiondocumental.security.AuthenticatedUser;
import com.plantarsas.gestiondocumental.security.UsuarioAreaAutorizacionService;
import com.plantarsas.gestiondocumental.shared.enums.RolEnum;
import com.plantarsas.gestiondocumental.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DocumentoConsultaServiceImpl implements DocumentoConsultaService {

    private final DocumentoRepository documentoRepository;
    private final DocumentoAreaRepository documentoAreaRepository;
    private final VersionDocumentoRepository versionDocumentoRepository;
    private final UsuarioAreaAutorizacionService usuarioAreaAutorizacionService;
    private final DocumentoMapper documentoMapper;
    private final StorageService storageService;

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentoResumenResponse> listar(
            AuthenticatedUser usuarioAutenticado, DocumentoFiltroRequest filtro, Pageable pageable
    ) {
        Set<Long> areaIds = obtenerAreaIds(usuarioAutenticado);

        Specification<Documento> base = DocumentoSpecifications.visiblePara(usuarioAutenticado.rol(), areaIds);
        base = aplicarFiltros(base, filtro);

        Specification<Documento> consulta = base.and(DocumentoSpecifications.conRelacionesDeResumen());

        return documentoRepository.findAll(consulta, base, pageable)
                .map(documentoMapper::toResumen);
    }

    private Specification<Documento> aplicarFiltros(Specification<Documento> base, DocumentoFiltroRequest filtro) {
        Specification<Documento> resultado = base;

        if (filtro.codigo() != null && !filtro.codigo().isBlank()) {
            resultado = resultado.and(DocumentoSpecifications.codigoContiene(filtro.codigo()));
        }
        if (filtro.titulo() != null && !filtro.titulo().isBlank()) {
            resultado = resultado.and(DocumentoSpecifications.tituloContiene(filtro.titulo()));
        }
        if (filtro.areaId() != null) {
            resultado = resultado.and(DocumentoSpecifications.deArea(filtro.areaId()));
        }
        if (filtro.subprogramaId() != null) {
            resultado = resultado.and(DocumentoSpecifications.deSubprograma(filtro.subprogramaId()));
        }
        if (filtro.tipoDocumentoId() != null) {
            resultado = resultado.and(DocumentoSpecifications.deTipoDocumento(filtro.tipoDocumentoId()));
        }
        if (filtro.estado() != null) {
            resultado = resultado.and(DocumentoSpecifications.conEstado(filtro.estado()));
        }
        if (filtro.fechaDesde() != null) {
            resultado = resultado.and(DocumentoSpecifications.creadoDesde(filtro.fechaDesde().atStartOfDay()));
        }
        if (filtro.fechaHasta() != null) {
            resultado = resultado.and(
                    DocumentoSpecifications.creadoAntesDe(filtro.fechaHasta().plusDays(1).atStartOfDay())
            );
        }

        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoResponse obtenerPorId(Long documentoId, AuthenticatedUser usuarioAutenticado) {
        Documento documento = buscarDocumentoVisible(documentoId, usuarioAutenticado);

        DocumentoArea principal = documentoAreaRepository.findByDocumento_IdAndEsPrincipalTrue(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un área principal asignada para el documento con id " + documentoId
                ));

        List<DocumentoArea> adicionales =
                documentoAreaRepository.findAllByDocumento_IdAndEsPrincipalFalse(documentoId);

        VersionDocumento versionVigente = versionDocumentoRepository
                .findByDocumento_IdAndVigenteTrue(documentoId)
                .orElseThrow(() -> new IllegalStateException(
                        "Inconsistencia de datos: el documento con id " + documentoId
                                + " está en estado PUBLICADO pero no tiene una versión vigente registrada"
                ));

        return documentoMapper.toResponse(documento, principal, adicionales, versionVigente);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoArchivoDescarga descargarVersionVigente(Long documentoId, AuthenticatedUser usuarioAutenticado) {
        buscarDocumentoVisible(documentoId, usuarioAutenticado);

        VersionDocumento versionVigente = versionDocumentoRepository
                .findByDocumento_IdAndVigenteTrue(documentoId)
                .orElseThrow(() -> new IllegalStateException(
                        "Inconsistencia de datos: el documento con id " + documentoId
                                + " está en estado PUBLICADO pero no tiene una versión vigente registrada"
                ));

        return aArchivoDescarga(versionVigente, "No se pudo leer el archivo de la versión vigente");
    }

    @Override
    @Transactional(readOnly = true)
    public List<VersionHistoricaResponse> listarHistorico(Long documentoId, AuthenticatedUser usuarioAutenticado) {
        buscarDocumentoVisible(documentoId, usuarioAutenticado);

        return versionDocumentoRepository.findByDocumento_IdOrderByNumeroVersionDesc(documentoId).stream()
                .map(documentoMapper::toHistorico)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoArchivoDescarga descargarVersionHistorica(
            Long documentoId, Long versionId, AuthenticatedUser usuarioAutenticado
    ) {
        buscarDocumentoVisible(documentoId, usuarioAutenticado);

        VersionDocumento version = versionDocumentoRepository.findByIdAndDocumento_Id(versionId, documentoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe la versión con id " + versionId + " para el documento con id " + documentoId
                ));

        return aArchivoDescarga(version, "No se pudo leer el archivo de la versión histórica");
    }

    private DocumentoArchivoDescarga aArchivoDescarga(VersionDocumento version, String mensajeErrorLectura) {
        InputStream contenido;
        try {
            contenido = storageService.cargar(version.getRutaArchivo());
        } catch (IOException e) {
            throw new UncheckedIOException(mensajeErrorLectura, e);
        }

        return new DocumentoArchivoDescarga(
                version.getNombreArchivoOriginal(),
                version.getTipoMime(),
                version.getTamanoBytes(),
                contenido
        );
    }

    private Documento buscarDocumentoVisible(Long documentoId, AuthenticatedUser usuarioAutenticado) {
        Set<Long> areaIds = obtenerAreaIds(usuarioAutenticado);

        Specification<Documento> specification = Specification
                .where(DocumentoSpecifications.idIgual(documentoId))
                .and(DocumentoSpecifications.visiblePara(usuarioAutenticado.rol(), areaIds));

        return documentoRepository.findOne(specification)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un documento con id " + documentoId
                ));
    }

    private Set<Long> obtenerAreaIds(AuthenticatedUser usuarioAutenticado) {
        if (usuarioAutenticado.rol() == RolEnum.ADMINISTRADOR) {
            return Set.of();
        }
        return usuarioAreaAutorizacionService.obtenerAreaIdsAutorizadas(usuarioAutenticado);
    }
}

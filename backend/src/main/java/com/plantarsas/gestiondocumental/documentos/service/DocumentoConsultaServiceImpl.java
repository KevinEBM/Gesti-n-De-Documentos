package com.plantarsas.gestiondocumental.documentos.service;

import com.plantarsas.gestiondocumental.documentos.dto.DocumentoResponse;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoResumenResponse;
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
import com.plantarsas.gestiondocumental.shared.enums.RolEnum;
import com.plantarsas.gestiondocumental.usuarios.repository.UsuarioAreaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentoConsultaServiceImpl implements DocumentoConsultaService {

    private final DocumentoRepository documentoRepository;
    private final DocumentoAreaRepository documentoAreaRepository;
    private final VersionDocumentoRepository versionDocumentoRepository;
    private final UsuarioAreaRepository usuarioAreaRepository;
    private final DocumentoMapper documentoMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentoResumenResponse> listar(AuthenticatedUser usuarioAutenticado, Pageable pageable) {
        Set<Long> areaIds = obtenerAreaIds(usuarioAutenticado);

        Specification<Documento> visibilidad = DocumentoSpecifications.visiblePara(usuarioAutenticado.rol(), areaIds);
        Specification<Documento> consulta = visibilidad.and(DocumentoSpecifications.conRelacionesDeResumen());

        return documentoRepository.findAll(consulta, visibilidad, pageable)
                .map(documentoMapper::toResumen);
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
        return usuarioAreaRepository.findByUsuario_Id(usuarioAutenticado.id()).stream()
                .map(usuarioArea -> usuarioArea.getArea().getId())
                .collect(Collectors.toSet());
    }
}

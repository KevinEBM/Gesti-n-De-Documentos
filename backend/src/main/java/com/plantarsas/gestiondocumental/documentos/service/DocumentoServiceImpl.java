package com.plantarsas.gestiondocumental.documentos.service;

import com.plantarsas.gestiondocumental.areas.entity.Area;
import com.plantarsas.gestiondocumental.areas.service.AreaLookupService;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoPublicacionInicialRequest;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoResponse;
import com.plantarsas.gestiondocumental.documentos.dto.NuevaVersionDocumentoRequest;
import com.plantarsas.gestiondocumental.documentos.entity.Documento;
import com.plantarsas.gestiondocumental.documentos.entity.DocumentoArea;
import com.plantarsas.gestiondocumental.documentos.entity.VersionDocumento;
import com.plantarsas.gestiondocumental.documentos.mapper.DocumentoMapper;
import com.plantarsas.gestiondocumental.documentos.repository.DocumentoAreaRepository;
import com.plantarsas.gestiondocumental.documentos.repository.DocumentoRepository;
import com.plantarsas.gestiondocumental.documentos.repository.VersionDocumentoRepository;
import com.plantarsas.gestiondocumental.exception.BusinessException;
import com.plantarsas.gestiondocumental.exception.ResourceNotFoundException;
import com.plantarsas.gestiondocumental.exception.UnauthorizedException;
import com.plantarsas.gestiondocumental.security.AuthenticatedUser;
import com.plantarsas.gestiondocumental.shared.enums.DocumentoAlcance;
import com.plantarsas.gestiondocumental.shared.enums.DocumentoEstado;
import com.plantarsas.gestiondocumental.shared.enums.RolEnum;
import com.plantarsas.gestiondocumental.storage.StorageService;
import com.plantarsas.gestiondocumental.storage.StoredFile;
import com.plantarsas.gestiondocumental.subprogramas.entity.Subprograma;
import com.plantarsas.gestiondocumental.subprogramas.service.SubprogramaLookupService;
import com.plantarsas.gestiondocumental.tiposdocumento.entity.TipoDocumento;
import com.plantarsas.gestiondocumental.tiposdocumento.service.TipoDocumentoLookupService;
import com.plantarsas.gestiondocumental.usuarios.entity.Usuario;
import com.plantarsas.gestiondocumental.usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentoServiceImpl implements DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final DocumentoAreaRepository documentoAreaRepository;
    private final VersionDocumentoRepository versionDocumentoRepository;
    private final AreaLookupService areaLookupService;
    private final SubprogramaLookupService subprogramaLookupService;
    private final TipoDocumentoLookupService tipoDocumentoLookupService;
    private final UsuarioRepository usuarioRepository;
    private final StorageService storageService;
    private final DocumentoMapper documentoMapper;

    @Override
    @Transactional
    public DocumentoResponse publicarInicial(
            DocumentoPublicacionInicialRequest request,
            AuthenticatedUser usuarioAutenticado,
            String nombreArchivoOriginal,
            InputStream contenidoArchivo,
            String tipoMimeArchivo,
            long tamanoBytesArchivo
    ) {
        if (usuarioAutenticado == null) {
            throw new UnauthorizedException(
                    "Se requiere un usuario autenticado para publicar un documento"
            );
        }
        if (usuarioAutenticado.rol() != RolEnum.ADMINISTRADOR) {
            throw new UnauthorizedException(
                    "Solo el administrador puede publicar documentos"
            );
        }

        String codigoNormalizado = request.codigo().trim();
        if (documentoRepository.existsByCodigoIgnoreCase(codigoNormalizado)) {
            throw new BusinessException(
                    "Ya existe un documento con el código '" + codigoNormalizado + "'",
                    HttpStatus.CONFLICT
            );
        }

        Area area = areaLookupService.obtenerActivaPorId(request.areaId());
        Subprograma subprograma = subprogramaLookupService.obtenerActivoPorId(request.subprogramaId());
        if (!subprograma.getArea().getId().equals(area.getId())) {
            throw new BusinessException(
                    "El subprograma '" + subprograma.getNombre() + "' no pertenece al área seleccionada"
            );
        }

        TipoDocumento tipoDocumento = tipoDocumentoLookupService.obtenerActivoPorId(request.tipoDocumentoId());

        Usuario usuario = usuarioRepository.findById(usuarioAutenticado.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un usuario con id " + usuarioAutenticado.id()
                ));

        List<Area> areasAdicionales = resolverAreasAdicionales(request, area);

        StoredFile archivoGuardado;
        try {
            archivoGuardado = storageService.guardar(
                    nombreArchivoOriginal, contenidoArchivo, tipoMimeArchivo, tamanoBytesArchivo
            );
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo almacenar el archivo del documento", e);
        }

        try {
            registrarCompensacionSiFallaLaTransaccion(archivoGuardado.ruta());

            Documento documento = new Documento(
                    codigoNormalizado,
                    request.titulo(),
                    request.descripcion(),
                    subprograma,
                    tipoDocumento,
                    usuario,
                    request.alcance()
            );
            documentoRepository.save(documento);

            DocumentoArea principal = DocumentoArea.principal(documento, area);
            documentoAreaRepository.save(principal);

            List<DocumentoArea> asociacionesAdicionales = crearAreasAdicionales(documento, areasAdicionales);

            VersionDocumento version = new VersionDocumento(
                    documento,
                    1,
                    archivoGuardado.nombreOriginal(),
                    archivoGuardado.ruta(),
                    archivoGuardado.ruta(),
                    archivoGuardado.mimeType(),
                    archivoGuardado.tamanoBytes(),
                    request.descripcionVersionInicial().trim(),
                    usuario
            );
            versionDocumentoRepository.save(version);

            documentoRepository.flush();

            return documentoMapper.toResponse(documento, principal, asociacionesAdicionales, version);
        } catch (RuntimeException e) {
            eliminarSilenciosamente(archivoGuardado.ruta());
            throw e;
        }
    }

    @Override
    @Transactional
    public DocumentoResponse publicarNuevaVersion(
            Long documentoId,
            NuevaVersionDocumentoRequest request,
            AuthenticatedUser usuarioAutenticado,
            String nombreArchivoOriginal,
            InputStream contenidoArchivo,
            String tipoMimeArchivo,
            long tamanoBytesArchivo
    ) {
        if (usuarioAutenticado == null) {
            throw new UnauthorizedException(
                    "Se requiere un usuario autenticado para publicar una nueva versión"
            );
        }
        if (usuarioAutenticado.rol() != RolEnum.ADMINISTRADOR) {
            throw new UnauthorizedException(
                    "Solo el administrador puede publicar nuevas versiones"
            );
        }
        if (documentoId == null || documentoId <= 0) {
            throw new BusinessException(
                    "El identificador del documento debe ser un valor válido",
                    HttpStatus.BAD_REQUEST
            );
        }

        Documento documento = documentoRepository.buscarPorIdConBloqueoPesimista(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un documento con id " + documentoId
                ));

        if (documento.getEstado() != DocumentoEstado.PUBLICADO) {
            throw new BusinessException(
                    "El documento '" + documento.getCodigo()
                            + "' no permite publicar nuevas versiones en su estado actual"
            );
        }

        DocumentoArea principal = documentoAreaRepository.findByDocumento_IdAndEsPrincipalTrue(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un área principal asignada para el documento con id " + documentoId
                ));

        List<DocumentoArea> adicionales = documentoAreaRepository.findAllByDocumento_IdAndEsPrincipalFalse(documentoId);

        VersionDocumento vigenteActual = versionDocumentoRepository.findByDocumento_IdAndVigenteTrue(documentoId)
                .orElseThrow(() -> new IllegalStateException(
                        "Inconsistencia de datos: el documento con id " + documentoId
                                + " está en estado PUBLICADO pero no tiene una versión vigente registrada"
                ));

        Usuario usuario = usuarioRepository.findById(usuarioAutenticado.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un usuario con id " + usuarioAutenticado.id()
                ));

        int siguienteNumeroVersion = versionDocumentoRepository.obtenerUltimoNumeroVersion(documentoId) + 1;

        StoredFile archivoGuardado;
        try {
            archivoGuardado = storageService.guardar(
                    nombreArchivoOriginal, contenidoArchivo, tipoMimeArchivo, tamanoBytesArchivo
            );
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo almacenar el archivo de la nueva versión", e);
        }

        try {
            registrarCompensacionSiFallaLaTransaccion(archivoGuardado.ruta());

            vigenteActual.marcarNoVigente();
            versionDocumentoRepository.flush();

            VersionDocumento nuevaVersion = new VersionDocumento(
                    documento,
                    siguienteNumeroVersion,
                    archivoGuardado.nombreOriginal(),
                    archivoGuardado.ruta(),
                    archivoGuardado.ruta(),
                    archivoGuardado.mimeType(),
                    archivoGuardado.tamanoBytes(),
                    request.descripcionCambio().trim(),
                    usuario
            );
            versionDocumentoRepository.save(nuevaVersion);

            versionDocumentoRepository.flush();

            return documentoMapper.toResponse(documento, principal, adicionales, nuevaVersion);
        } catch (RuntimeException e) {
            eliminarSilenciosamente(archivoGuardado.ruta());
            throw e;
        }
    }

    private List<Area> resolverAreasAdicionales(
            DocumentoPublicacionInicialRequest request,
            Area areaPrincipal
    ) {
        List<Long> idsAdicionales = request.areasAdicionalesIds();

        if (request.alcance() != DocumentoAlcance.AREAS_ESPECIFICAS) {
            if (!idsAdicionales.isEmpty()) {
                throw new BusinessException(
                        "El alcance '" + request.alcance() + "' no admite áreas adicionales"
                );
            }
            return List.of();
        }

        if (idsAdicionales.isEmpty()) {
            throw new BusinessException(
                    "El alcance AREAS_ESPECIFICAS requiere al menos un área adicional"
            );
        }

        if (new HashSet<>(idsAdicionales).size() != idsAdicionales.size()) {
            throw new BusinessException(
                    "La lista de áreas adicionales no puede contener identificadores duplicados"
            );
        }

        if (idsAdicionales.contains(areaPrincipal.getId())) {
            throw new BusinessException(
                    "El área responsable no puede repetirse como área adicional"
            );
        }

        return areaLookupService.obtenerActivasPorIds(idsAdicionales);
    }

    private List<DocumentoArea> crearAreasAdicionales(Documento documento, List<Area> areasAdicionales) {
        List<DocumentoArea> asociaciones = areasAdicionales.stream()
                .map(areaAdicional -> DocumentoArea.adicional(documento, areaAdicional))
                .toList();
        asociaciones.forEach(documentoAreaRepository::save);
        return asociaciones;
    }

    private void registrarCompensacionSiFallaLaTransaccion(String ruta) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != TransactionSynchronization.STATUS_COMMITTED) {
                            eliminarSilenciosamente(ruta);
                        }
                    }
                }
        );
    }

    private void eliminarSilenciosamente(String ruta) {
        try {
            storageService.eliminar(ruta);
        } catch (IOException e) {
            log.error(
                    "No se pudo eliminar el archivo huérfano en ruta '{}' tras un fallo de publicación",
                    ruta, e
            );
        }
    }
}

package com.plantarsas.gestiondocumental.documentos.mapper;

import com.plantarsas.gestiondocumental.documentos.dto.AreaResumenResponse;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoResponse;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoResumenResponse;
import com.plantarsas.gestiondocumental.documentos.dto.VersionHistoricaResponse;
import com.plantarsas.gestiondocumental.documentos.entity.Documento;
import com.plantarsas.gestiondocumental.documentos.entity.DocumentoArea;
import com.plantarsas.gestiondocumental.documentos.entity.VersionDocumento;
import com.plantarsas.gestiondocumental.usuarios.entity.Usuario;
import com.plantarsas.gestiondocumental.shared.time.FechaHoraUtc;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DocumentoMapper {

    private final Clock clock;

    public DocumentoResponse toResponse(
            Documento documento,
            DocumentoArea documentoAreaPrincipal,
            List<DocumentoArea> documentoAreasAdicionales,
            VersionDocumento versionActual
    ) {
        LocalDateTime ahoraUtc = FechaHoraUtc.ahoraDesde(clock);

        return new DocumentoResponse(
                documento.getId(),
                documento.getCodigo(),
                documento.getTitulo(),
                documento.getDescripcion(),
                documento.getEstado(),
                documentoAreaPrincipal.getArea().getId(),
                documentoAreaPrincipal.getArea().getNombre(),
                documento.getSubprograma().getId(),
                documento.getSubprograma().getNombre(),
                documento.getTipoDocumento().getId(),
                documento.getTipoDocumento().getNombre(),
                documento.getCreadoPor().getId(),
                versionActual.getNumeroVersion(),
                versionActual.getNombreArchivoOriginal(),
                versionActual.getTipoMime(),
                versionActual.getTamanoBytes(),
                versionActual.getDescripcionCambio(),
                versionActual.getPublicadoPor().getId(),
                FechaHoraUtc.aInstant(versionActual.getFechaPublicacion()),
                FechaHoraUtc.aInstant(documento.getFechaCreacion()),
                FechaHoraUtc.aInstant(documento.getFechaActualizacion()),
                FechaHoraUtc.aInstantONulo(documento.getFechaObsolescencia()),
                FechaHoraUtc.aInstantONulo(documento.fechaDisponibleEliminacion()),
                documento.esAptoParaEliminacion(ahoraUtc),
                documento.getAlcance(),
                documentoAreasAdicionales.stream()
                        .map(documentoArea -> new AreaResumenResponse(
                                documentoArea.getArea().getId(),
                                documentoArea.getArea().getNombre()
                        ))
                        .toList()
        );
    }

    public DocumentoResumenResponse toResumen(Documento documento) {
        return new DocumentoResumenResponse(
                documento.getId(),
                documento.getCodigo(),
                documento.getTitulo(),
                documento.getEstado(),
                documento.getAlcance(),
                documento.getSubprograma().getNombre(),
                documento.getTipoDocumento().getNombre(),
                FechaHoraUtc.aInstant(documento.getFechaActualizacion())
        );
    }

    public VersionHistoricaResponse toHistorico(VersionDocumento version) {
        Usuario publicadoPor = version.getPublicadoPor();
        return new VersionHistoricaResponse(
                version.getId(),
                version.getNumeroVersion(),
                version.getNombreArchivoOriginal(),
                version.getTipoMime(),
                version.getTamanoBytes(),
                version.getDescripcionCambio(),
                FechaHoraUtc.aInstant(version.getFechaPublicacion()),
                publicadoPor != null ? publicadoPor.getId() : null,
                nombrePublicador(publicadoPor),
                version.isVigente()
        );
    }

    private String nombrePublicador(Usuario usuario) {
        if (usuario == null) {
            return "Usuario no disponible";
        }

        String nombres = usuario.getNombres() == null ? "" : usuario.getNombres().trim();
        String apellidos = usuario.getApellidos() == null ? "" : usuario.getApellidos().trim();
        String nombreCompleto = (nombres + " " + apellidos).trim();

        return nombreCompleto.isBlank() ? "Usuario no disponible" : nombreCompleto;
    }
}

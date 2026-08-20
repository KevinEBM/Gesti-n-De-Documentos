package com.plantarsas.gestiondocumental.documentos.mapper;

import com.plantarsas.gestiondocumental.documentos.dto.AreaResumenResponse;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoResponse;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoResumenResponse;
import com.plantarsas.gestiondocumental.documentos.dto.VersionHistoricaResponse;
import com.plantarsas.gestiondocumental.documentos.entity.Documento;
import com.plantarsas.gestiondocumental.documentos.entity.DocumentoArea;
import com.plantarsas.gestiondocumental.documentos.entity.VersionDocumento;
import com.plantarsas.gestiondocumental.usuarios.entity.Usuario;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DocumentoMapper {

    public DocumentoResponse toResponse(
            Documento documento,
            DocumentoArea documentoAreaPrincipal,
            List<DocumentoArea> documentoAreasAdicionales,
            VersionDocumento versionActual
    ) {
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
                versionActual.getFechaPublicacion(),
                documento.getFechaCreacion(),
                documento.getFechaActualizacion(),
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
                documento.getFechaActualizacion()
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
                version.getFechaPublicacion(),
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

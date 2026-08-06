package com.plantarsas.gestiondocumental.documentos.mapper;

import com.plantarsas.gestiondocumental.documentos.dto.DocumentoResponse;
import com.plantarsas.gestiondocumental.documentos.entity.Documento;
import com.plantarsas.gestiondocumental.documentos.entity.DocumentoArea;
import com.plantarsas.gestiondocumental.documentos.entity.VersionDocumento;
import org.springframework.stereotype.Component;

@Component
public class DocumentoMapper {

    public DocumentoResponse toResponse(
            Documento documento,
            DocumentoArea documentoArea,
            VersionDocumento versionActual
    ) {
        return new DocumentoResponse(
                documento.getId(),
                documento.getCodigo(),
                documento.getTitulo(),
                documento.getDescripcion(),
                documento.getEstado(),
                documentoArea.getArea().getId(),
                documentoArea.getArea().getNombre(),
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
                documento.getFechaActualizacion()
        );
    }
}

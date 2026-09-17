package com.plantarsas.gestiondocumental.tiposdocumento.mapper;

import com.plantarsas.gestiondocumental.tiposdocumento.dto.TipoDocumentoResponse;
import com.plantarsas.gestiondocumental.tiposdocumento.entity.TipoDocumento;
import org.springframework.stereotype.Component;

/**
 * Convierte un tipo de documento, tal como se guarda en la base de
 * datos, al formato que se le muestra al usuario.
 */
@Component
public class TipoDocumentoMapper {

    public TipoDocumentoResponse toResponse(TipoDocumento tipoDocumento) {
        return new TipoDocumentoResponse(
                tipoDocumento.getId(),
                tipoDocumento.getCodigo(),
                tipoDocumento.getNombre(),
                tipoDocumento.getDescripcion(),
                tipoDocumento.isActivo(),
                tipoDocumento.getFechaCreacion(),
                tipoDocumento.getFechaActualizacion()
        );
    }
}

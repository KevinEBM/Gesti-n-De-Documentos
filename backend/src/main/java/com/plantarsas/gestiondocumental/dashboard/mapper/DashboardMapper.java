package com.plantarsas.gestiondocumental.dashboard.mapper;

import com.plantarsas.gestiondocumental.dashboard.dto.ActividadDocumentalResponse;
import com.plantarsas.gestiondocumental.dashboard.dto.TipoActividad;
import com.plantarsas.gestiondocumental.documentos.entity.VersionDocumento;
import org.springframework.stereotype.Component;

@Component
public class DashboardMapper {

    public ActividadDocumentalResponse toActividad(VersionDocumento version) {
        TipoActividad tipoActividad = version.getNumeroVersion() == 1
                ? TipoActividad.NUEVA_PUBLICACION
                : TipoActividad.NUEVA_VERSION;

        return new ActividadDocumentalResponse(
                tipoActividad,
                version.getDocumento().getId(),
                version.getDocumento().getCodigo(),
                version.getDocumento().getTitulo(),
                version.getNumeroVersion(),
                version.getDescripcionCambio(),
                version.getFechaPublicacion(),
                version.getPublicadoPor().getId(),
                version.getPublicadoPor().getNombres(),
                version.getPublicadoPor().getApellidos()
        );
    }
}

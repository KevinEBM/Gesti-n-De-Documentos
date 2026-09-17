package com.plantarsas.gestiondocumental.areas.mapper;

import com.plantarsas.gestiondocumental.areas.dto.AreaResponse;
import com.plantarsas.gestiondocumental.areas.entity.Area;
import org.springframework.stereotype.Component;

/**
 * Convierte un área, tal como se guarda en la base de datos, al
 * formato que se le muestra al usuario.
 */
@Component
public class AreaMapper {

    public AreaResponse toResponse(Area area) {
        return new AreaResponse(
                area.getId(),
                area.getCodigo(),
                area.getNombre(),
                area.getDescripcion(),
                area.isActivo(),
                area.getFechaCreacion(),
                area.getFechaActualizacion()
        );
    }
}

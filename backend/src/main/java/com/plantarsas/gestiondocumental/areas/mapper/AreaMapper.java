package com.plantarsas.gestiondocumental.areas.mapper;

import com.plantarsas.gestiondocumental.areas.dto.AreaResponse;
import com.plantarsas.gestiondocumental.areas.entity.Area;
import org.springframework.stereotype.Component;

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

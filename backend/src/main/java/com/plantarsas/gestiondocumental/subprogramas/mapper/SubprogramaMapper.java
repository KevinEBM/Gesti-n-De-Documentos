package com.plantarsas.gestiondocumental.subprogramas.mapper;

import com.plantarsas.gestiondocumental.areas.mapper.AreaMapper;
import com.plantarsas.gestiondocumental.subprogramas.dto.SubprogramaResponse;
import com.plantarsas.gestiondocumental.subprogramas.entity.Subprograma;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubprogramaMapper {

    private final AreaMapper areaMapper;

    public SubprogramaResponse toResponse(Subprograma subprograma) {
        return new SubprogramaResponse(
                subprograma.getId(),
                subprograma.getNombre(),
                subprograma.getDescripcion(),
                areaMapper.toResponse(subprograma.getArea()),
                subprograma.isActivo(),
                subprograma.getFechaCreacion(),
                subprograma.getFechaActualizacion()
        );
    }
}

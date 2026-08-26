package com.plantarsas.gestiondocumental.subprogramas.mapper;

import com.plantarsas.gestiondocumental.areas.dto.AreaResponse;
import com.plantarsas.gestiondocumental.areas.entity.Area;
import com.plantarsas.gestiondocumental.areas.mapper.AreaMapper;
import com.plantarsas.gestiondocumental.subprogramas.dto.SubprogramaResponse;
import com.plantarsas.gestiondocumental.subprogramas.entity.Subprograma;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubprogramaMapperTest {

    @Mock
    private AreaMapper areaMapper;

    @InjectMocks
    private SubprogramaMapper subprogramaMapper;

    @Test
    void toResponse_debeExponerCodigoNormalizadoYArea() {
        Area area = mock(Area.class);
        AreaResponse areaResponse = new AreaResponse(
                7L,
                "GAMB",
                "Gestión Ambiental",
                null,
                true,
                null,
                null
        );
        Subprograma subprograma = new Subprograma(
                "  l&d  ",
                "  Limpieza y Desinfección  ",
                "Descripcion",
                area
        );

        when(areaMapper.toResponse(area)).thenReturn(areaResponse);

        SubprogramaResponse respuesta = subprogramaMapper.toResponse(subprograma);

        assertThat(respuesta.codigo()).isEqualTo("L&D");
        assertThat(respuesta.nombre()).isEqualTo("Limpieza y Desinfección");
        assertThat(respuesta.descripcion()).isEqualTo("Descripcion");
        assertThat(respuesta.area()).isEqualTo(areaResponse);
        assertThat(respuesta.activo()).isTrue();
    }
}

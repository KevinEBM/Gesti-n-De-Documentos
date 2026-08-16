package com.plantarsas.gestiondocumental.documentos.mapper;

import com.plantarsas.gestiondocumental.areas.entity.Area;
import com.plantarsas.gestiondocumental.documentos.dto.AreaResumenResponse;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoResponse;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoResumenResponse;
import com.plantarsas.gestiondocumental.documentos.entity.Documento;
import com.plantarsas.gestiondocumental.documentos.entity.DocumentoArea;
import com.plantarsas.gestiondocumental.documentos.entity.VersionDocumento;
import com.plantarsas.gestiondocumental.shared.enums.DocumentoAlcance;
import com.plantarsas.gestiondocumental.shared.enums.DocumentoEstado;
import com.plantarsas.gestiondocumental.subprogramas.entity.Subprograma;
import com.plantarsas.gestiondocumental.tiposdocumento.entity.TipoDocumento;
import com.plantarsas.gestiondocumental.usuarios.entity.Usuario;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentoMapperTest {

    private final DocumentoMapper documentoMapper = new DocumentoMapper();

    @Test
    void toResponse_debeMapearLosVeintiunCamposEnElOrdenCorrecto() {
        Area area = mock(Area.class);
        when(area.getId()).thenReturn(10L);
        when(area.getNombre()).thenReturn("Área de prueba");

        Subprograma subprograma = mock(Subprograma.class);
        when(subprograma.getId()).thenReturn(20L);
        when(subprograma.getNombre()).thenReturn("Subprograma de prueba");

        TipoDocumento tipoDocumento = mock(TipoDocumento.class);
        when(tipoDocumento.getId()).thenReturn(30L);
        when(tipoDocumento.getNombre()).thenReturn("Tipo de prueba");

        Usuario creadoPor = mock(Usuario.class);
        when(creadoPor.getId()).thenReturn(40L);

        Usuario publicadoPor = mock(Usuario.class);
        when(publicadoPor.getId()).thenReturn(50L);

        LocalDateTime fechaCreacion = LocalDateTime.of(2026, 1, 1, 8, 0);
        LocalDateTime fechaActualizacion = LocalDateTime.of(2026, 1, 2, 9, 0);
        LocalDateTime fechaPublicacionVersion = LocalDateTime.of(2026, 1, 1, 8, 5);

        Documento documento = mock(Documento.class);
        when(documento.getId()).thenReturn(1L);
        when(documento.getCodigo()).thenReturn("PROC-001");
        when(documento.getTitulo()).thenReturn("Título de prueba");
        when(documento.getDescripcion()).thenReturn("Descripción de prueba");
        when(documento.getEstado()).thenReturn(DocumentoEstado.PUBLICADO);
        when(documento.getSubprograma()).thenReturn(subprograma);
        when(documento.getTipoDocumento()).thenReturn(tipoDocumento);
        when(documento.getCreadoPor()).thenReturn(creadoPor);
        when(documento.getFechaCreacion()).thenReturn(fechaCreacion);
        when(documento.getFechaActualizacion()).thenReturn(fechaActualizacion);
        when(documento.getAlcance()).thenReturn(DocumentoAlcance.AREA_RESPONSABLE);

        DocumentoArea documentoArea = mock(DocumentoArea.class);
        when(documentoArea.getArea()).thenReturn(area);

        VersionDocumento version = mock(VersionDocumento.class);
        when(version.getNumeroVersion()).thenReturn(1);
        when(version.getNombreArchivoOriginal()).thenReturn("archivo.pdf");
        when(version.getTipoMime()).thenReturn("application/pdf");
        when(version.getTamanoBytes()).thenReturn(2048L);
        when(version.getDescripcionCambio()).thenReturn("Publicación inicial");
        when(version.getPublicadoPor()).thenReturn(publicadoPor);
        when(version.getFechaPublicacion()).thenReturn(fechaPublicacionVersion);

        DocumentoResponse resultado = documentoMapper.toResponse(
                documento,
                documentoArea,
                List.of(),
                version
        );

        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.codigo()).isEqualTo("PROC-001");
        assertThat(resultado.titulo()).isEqualTo("Título de prueba");
        assertThat(resultado.descripcion()).isEqualTo("Descripción de prueba");
        assertThat(resultado.estado()).isEqualTo(DocumentoEstado.PUBLICADO);
        assertThat(resultado.areaId()).isEqualTo(10L);
        assertThat(resultado.areaNombre()).isEqualTo("Área de prueba");
        assertThat(resultado.subprogramaId()).isEqualTo(20L);
        assertThat(resultado.subprogramaNombre()).isEqualTo("Subprograma de prueba");
        assertThat(resultado.tipoDocumentoId()).isEqualTo(30L);
        assertThat(resultado.tipoDocumentoNombre()).isEqualTo("Tipo de prueba");
        assertThat(resultado.creadoPorId()).isEqualTo(40L);
        assertThat(resultado.numeroVersionActual()).isEqualTo(1);
        assertThat(resultado.nombreArchivoOriginal()).isEqualTo("archivo.pdf");
        assertThat(resultado.tipoMime()).isEqualTo("application/pdf");
        assertThat(resultado.tamanoBytes()).isEqualTo(2048L);
        assertThat(resultado.descripcionVersionActual()).isEqualTo("Publicación inicial");
        assertThat(resultado.publicadoPorId()).isEqualTo(50L);
        assertThat(resultado.fechaPublicacionVersion()).isEqualTo(fechaPublicacionVersion);
        assertThat(resultado.fechaCreacion()).isEqualTo(fechaCreacion);
        assertThat(resultado.fechaActualizacion()).isEqualTo(fechaActualizacion);
        assertThat(resultado.alcance()).isEqualTo(DocumentoAlcance.AREA_RESPONSABLE);
        assertThat(resultado.areasAdicionales()).isEmpty();
    }

    @Test
    void toResponse_debeMapearAreasAdicionales() {
        Area areaPrincipal = mock(Area.class);
        when(areaPrincipal.getId()).thenReturn(10L);
        when(areaPrincipal.getNombre()).thenReturn("Área principal");

        Area areaAdicional1 = mock(Area.class);
        when(areaAdicional1.getId()).thenReturn(11L);
        when(areaAdicional1.getNombre()).thenReturn("Área adicional 1");

        Area areaAdicional2 = mock(Area.class);
        when(areaAdicional2.getId()).thenReturn(12L);
        when(areaAdicional2.getNombre()).thenReturn("Área adicional 2");

        Documento documento = mock(Documento.class);
        when(documento.getAlcance()).thenReturn(DocumentoAlcance.AREAS_ESPECIFICAS);
        when(documento.getSubprograma()).thenReturn(mock(Subprograma.class));
        when(documento.getTipoDocumento()).thenReturn(mock(TipoDocumento.class));
        when(documento.getCreadoPor()).thenReturn(mock(Usuario.class));

        DocumentoArea principal = mock(DocumentoArea.class);
        when(principal.getArea()).thenReturn(areaPrincipal);

        DocumentoArea adicional1 = mock(DocumentoArea.class);
        when(adicional1.getArea()).thenReturn(areaAdicional1);

        DocumentoArea adicional2 = mock(DocumentoArea.class);
        when(adicional2.getArea()).thenReturn(areaAdicional2);

        VersionDocumento version = mock(VersionDocumento.class);
        when(version.getPublicadoPor()).thenReturn(mock(Usuario.class));

        DocumentoResponse resultado = documentoMapper.toResponse(
                documento,
                principal,
                List.of(adicional1, adicional2),
                version
        );

        assertThat(resultado.alcance()).isEqualTo(DocumentoAlcance.AREAS_ESPECIFICAS);
        assertThat(resultado.areaId()).isEqualTo(10L);
        assertThat(resultado.areaNombre()).isEqualTo("Área principal");
        assertThat(resultado.areasAdicionales()).containsExactly(
                new AreaResumenResponse(11L, "Área adicional 1"),
                new AreaResumenResponse(12L, "Área adicional 2")
        );
    }

    @Test
    void toResumen_debeMapearLosOchoCamposCorrectamente() {
        Subprograma subprograma = mock(Subprograma.class);
        when(subprograma.getNombre()).thenReturn("Subprograma de prueba");

        TipoDocumento tipoDocumento = mock(TipoDocumento.class);
        when(tipoDocumento.getNombre()).thenReturn("Tipo de prueba");

        LocalDateTime fechaActualizacion = LocalDateTime.of(2026, 1, 2, 9, 0);

        Documento documento = mock(Documento.class);
        when(documento.getId()).thenReturn(1L);
        when(documento.getCodigo()).thenReturn("PROC-001");
        when(documento.getTitulo()).thenReturn("Título de prueba");
        when(documento.getEstado()).thenReturn(DocumentoEstado.PUBLICADO);
        when(documento.getAlcance()).thenReturn(DocumentoAlcance.GLOBAL);
        when(documento.getSubprograma()).thenReturn(subprograma);
        when(documento.getTipoDocumento()).thenReturn(tipoDocumento);
        when(documento.getFechaActualizacion()).thenReturn(fechaActualizacion);

        DocumentoResumenResponse resultado = documentoMapper.toResumen(documento);

        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.codigo()).isEqualTo("PROC-001");
        assertThat(resultado.titulo()).isEqualTo("Título de prueba");
        assertThat(resultado.estado()).isEqualTo(DocumentoEstado.PUBLICADO);
        assertThat(resultado.alcance()).isEqualTo(DocumentoAlcance.GLOBAL);
        assertThat(resultado.subprogramaNombre()).isEqualTo("Subprograma de prueba");
        assertThat(resultado.tipoDocumentoNombre()).isEqualTo("Tipo de prueba");
        assertThat(resultado.fechaActualizacion()).isEqualTo(fechaActualizacion);
    }
}

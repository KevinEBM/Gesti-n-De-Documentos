package com.plantarsas.gestiondocumental.documentos.mapper;

import com.plantarsas.gestiondocumental.areas.entity.Area;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoResponse;
import com.plantarsas.gestiondocumental.documentos.entity.Documento;
import com.plantarsas.gestiondocumental.documentos.entity.DocumentoArea;
import com.plantarsas.gestiondocumental.documentos.entity.VersionDocumento;
import com.plantarsas.gestiondocumental.shared.enums.DocumentoEstado;
import com.plantarsas.gestiondocumental.subprogramas.entity.Subprograma;
import com.plantarsas.gestiondocumental.tiposdocumento.entity.TipoDocumento;
import com.plantarsas.gestiondocumental.usuarios.entity.Usuario;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

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

        DocumentoResponse resultado = documentoMapper.toResponse(documento, documentoArea, version);

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
    }
}

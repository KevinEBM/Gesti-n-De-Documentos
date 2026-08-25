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
import com.plantarsas.gestiondocumental.shared.time.FechaHoraUtc;
import com.plantarsas.gestiondocumental.usuarios.entity.Usuario;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
        assertThat(resultado.fechaPublicacionVersion()).isEqualTo(FechaHoraUtc.aInstant(fechaPublicacionVersion));
        assertThat(resultado.fechaCreacion()).isEqualTo(FechaHoraUtc.aInstant(fechaCreacion));
        assertThat(resultado.fechaActualizacion()).isEqualTo(FechaHoraUtc.aInstant(fechaActualizacion));
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
        when(version.getFechaPublicacion()).thenReturn(LocalDateTime.of(2026, 1, 1, 8, 0));
        when(documento.getFechaCreacion()).thenReturn(LocalDateTime.of(2026, 1, 1, 8, 0));
        when(documento.getFechaActualizacion()).thenReturn(LocalDateTime.of(2026, 1, 1, 8, 0));

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
    void toResponse_debeExponerAreaResponsableYDescripcionDeVersionVigente() {
        Area area = mock(Area.class);
        when(area.getId()).thenReturn(10L);
        when(area.getNombre()).thenReturn("Área responsable");

        Documento documento = mock(Documento.class);
        when(documento.getSubprograma()).thenReturn(mock(Subprograma.class));
        when(documento.getTipoDocumento()).thenReturn(mock(TipoDocumento.class));
        when(documento.getCreadoPor()).thenReturn(mock(Usuario.class));
        when(documento.getAlcance()).thenReturn(DocumentoAlcance.AREA_RESPONSABLE);

        DocumentoArea documentoArea = mock(DocumentoArea.class);
        when(documentoArea.getArea()).thenReturn(area);

        VersionDocumento version = mock(VersionDocumento.class);
        when(version.getPublicadoPor()).thenReturn(mock(Usuario.class));
        when(version.getDescripcionCambio()).thenReturn("Motivo de la versión vigente");
        when(version.getFechaPublicacion()).thenReturn(LocalDateTime.of(2026, 1, 1, 8, 0));
        when(documento.getFechaCreacion()).thenReturn(LocalDateTime.of(2026, 1, 1, 8, 0));
        when(documento.getFechaActualizacion()).thenReturn(LocalDateTime.of(2026, 1, 1, 8, 0));

        DocumentoResponse resultado = documentoMapper.toResponse(
                documento,
                documentoArea,
                List.of(),
                version
        );

        assertThat(resultado.areaNombre()).isEqualTo("Área responsable");
        assertThat(resultado.descripcionVersionActual()).isEqualTo("Motivo de la versión vigente");
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
        assertThat(resultado.fechaActualizacion()).isEqualTo(FechaHoraUtc.aInstant(fechaActualizacion));
    }

    @Test
    void toHistorico_debeMapearPublicadorConNombreCompleto() {
        Usuario publicadoPor = mock(Usuario.class);
        when(publicadoPor.getId()).thenReturn(50L);
        when(publicadoPor.getNombres()).thenReturn("Test");
        when(publicadoPor.getApellidos()).thenReturn("Prueba");

        LocalDateTime fechaPublicacion = LocalDateTime.of(2026, 8, 20, 12, 8);

        VersionDocumento version = mock(VersionDocumento.class);
        when(version.getId()).thenReturn(10L);
        when(version.getNumeroVersion()).thenReturn(2);
        when(version.getNombreArchivoOriginal()).thenReturn("archivo.pdf");
        when(version.getTipoMime()).thenReturn("application/pdf");
        when(version.getTamanoBytes()).thenReturn(2048L);
        when(version.getDescripcionCambio()).thenReturn("prueba 2 inactiva");
        when(version.getFechaPublicacion()).thenReturn(fechaPublicacion);
        when(version.getPublicadoPor()).thenReturn(publicadoPor);
        when(version.isVigente()).thenReturn(true);

        var resultado = documentoMapper.toHistorico(version);

        assertThat(resultado.id()).isEqualTo(10L);
        assertThat(resultado.numeroVersion()).isEqualTo(2);
        assertThat(resultado.descripcionCambio()).isEqualTo("prueba 2 inactiva");
        assertThat(resultado.publicadoPorId()).isEqualTo(50L);
        assertThat(resultado.publicadoPorNombre()).isEqualTo("Test Prueba");
        assertThat(resultado.fechaPublicacion()).isEqualTo(FechaHoraUtc.aInstant(fechaPublicacion));
        assertThat(resultado.vigente()).isTrue();
    }

    @Test
    void toResponse_fechasUtcNaive_seSerializanComoInstantEquivalente() {
        LocalDateTime almacenado = LocalDateTime.of(2026, 8, 25, 13, 21, 57);
        Instant esperado = Instant.parse("2026-08-25T13:21:57Z");

        Documento documento = mock(Documento.class);
        when(documento.getSubprograma()).thenReturn(mock(Subprograma.class));
        when(documento.getTipoDocumento()).thenReturn(mock(TipoDocumento.class));
        when(documento.getCreadoPor()).thenReturn(mock(Usuario.class));
        when(documento.getAlcance()).thenReturn(DocumentoAlcance.AREA_RESPONSABLE);
        when(documento.getFechaCreacion()).thenReturn(almacenado);
        when(documento.getFechaActualizacion()).thenReturn(almacenado);

        DocumentoArea documentoArea = mock(DocumentoArea.class);
        when(documentoArea.getArea()).thenReturn(mock(Area.class));

        VersionDocumento version = mock(VersionDocumento.class);
        when(version.getPublicadoPor()).thenReturn(mock(Usuario.class));
        when(version.getFechaPublicacion()).thenReturn(almacenado);

        DocumentoResponse resultado = documentoMapper.toResponse(
                documento,
                documentoArea,
                List.of(),
                version
        );

        assertThat(resultado.fechaCreacion()).isEqualTo(esperado);
        assertThat(resultado.fechaActualizacion()).isEqualTo(esperado);
        assertThat(resultado.fechaPublicacionVersion()).isEqualTo(esperado);
    }

    @Test
    void toHistorico_conPublicadorSinNombresCargados_debeUsarFallbackAunConId() {
        Usuario publicadoPor = mock(Usuario.class);
        when(publicadoPor.getId()).thenReturn(23L);
        when(publicadoPor.getNombres()).thenReturn(null);
        when(publicadoPor.getApellidos()).thenReturn(null);

        VersionDocumento version = mock(VersionDocumento.class);
        when(version.getId()).thenReturn(10L);
        when(version.getNumeroVersion()).thenReturn(2);
        when(version.getNombreArchivoOriginal()).thenReturn("archivo.pdf");
        when(version.getTipoMime()).thenReturn("application/pdf");
        when(version.getTamanoBytes()).thenReturn(1024L);
        when(version.getDescripcionCambio()).thenReturn("prueba 2 inactiva");
        when(version.getFechaPublicacion()).thenReturn(LocalDateTime.now());
        when(version.getPublicadoPor()).thenReturn(publicadoPor);
        when(version.isVigente()).thenReturn(true);

        var resultado = documentoMapper.toHistorico(version);

        assertThat(resultado.publicadoPorId()).isEqualTo(23L);
        assertThat(resultado.publicadoPorNombre()).isEqualTo("Usuario no disponible");
    }

    @Test
    void toHistorico_conPublicadorNulo_debeUsarFallbackDeNombre() {
        VersionDocumento version = mock(VersionDocumento.class);
        when(version.getId()).thenReturn(10L);
        when(version.getNumeroVersion()).thenReturn(1);
        when(version.getNombreArchivoOriginal()).thenReturn("archivo.pdf");
        when(version.getTipoMime()).thenReturn("application/pdf");
        when(version.getTamanoBytes()).thenReturn(1024L);
        when(version.getDescripcionCambio()).thenReturn("Publicación inicial");
        when(version.getFechaPublicacion()).thenReturn(LocalDateTime.now());
        when(version.getPublicadoPor()).thenReturn(null);
        when(version.isVigente()).thenReturn(false);

        var resultado = documentoMapper.toHistorico(version);

        assertThat(resultado.publicadoPorId()).isNull();
        assertThat(resultado.publicadoPorNombre()).isEqualTo("Usuario no disponible");
    }
}

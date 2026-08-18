package com.plantarsas.gestiondocumental.dashboard.mapper;

import com.plantarsas.gestiondocumental.dashboard.dto.ActividadDocumentalResponse;
import com.plantarsas.gestiondocumental.dashboard.dto.TipoActividad;
import com.plantarsas.gestiondocumental.documentos.entity.Documento;
import com.plantarsas.gestiondocumental.documentos.entity.VersionDocumento;
import com.plantarsas.gestiondocumental.usuarios.entity.Usuario;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardMapperTest {

    private final DashboardMapper dashboardMapper = new DashboardMapper();

    @Test
    void toActividad_conNumeroVersionUno_debeAsignarNuevaPublicacion() {
        Documento documento = mock(Documento.class);
        Usuario publicadoPor = mock(Usuario.class);
        VersionDocumento version = mock(VersionDocumento.class);
        when(version.getNumeroVersion()).thenReturn(1);
        when(version.getDocumento()).thenReturn(documento);
        when(version.getPublicadoPor()).thenReturn(publicadoPor);

        ActividadDocumentalResponse resultado = dashboardMapper.toActividad(version);

        assertThat(resultado.tipoActividad()).isEqualTo(TipoActividad.NUEVA_PUBLICACION);
    }

    @Test
    void toActividad_conNumeroVersionMayorAUno_debeAsignarNuevaVersion() {
        Documento documento = mock(Documento.class);
        Usuario publicadoPor = mock(Usuario.class);
        VersionDocumento version = mock(VersionDocumento.class);
        when(version.getNumeroVersion()).thenReturn(2);
        when(version.getDocumento()).thenReturn(documento);
        when(version.getPublicadoPor()).thenReturn(publicadoPor);

        ActividadDocumentalResponse resultado = dashboardMapper.toActividad(version);

        assertThat(resultado.tipoActividad()).isEqualTo(TipoActividad.NUEVA_VERSION);
    }

    @Test
    void toActividad_debeMapearLosDiezCamposCorrectamente() {
        Documento documento = mock(Documento.class);
        when(documento.getId()).thenReturn(1L);
        when(documento.getCodigo()).thenReturn("PROC-001");
        when(documento.getTitulo()).thenReturn("Título de prueba");

        Usuario publicadoPor = mock(Usuario.class);
        when(publicadoPor.getId()).thenReturn(9L);
        when(publicadoPor.getNombres()).thenReturn("Ana");
        when(publicadoPor.getApellidos()).thenReturn("Pérez");

        LocalDateTime fechaPublicacion = LocalDateTime.of(2026, 1, 1, 8, 0);

        VersionDocumento version = mock(VersionDocumento.class);
        when(version.getNumeroVersion()).thenReturn(2);
        when(version.getDocumento()).thenReturn(documento);
        when(version.getDescripcionCambio()).thenReturn("Corrección de erratas");
        when(version.getFechaPublicacion()).thenReturn(fechaPublicacion);
        when(version.getPublicadoPor()).thenReturn(publicadoPor);

        ActividadDocumentalResponse resultado = dashboardMapper.toActividad(version);

        assertThat(resultado.tipoActividad()).isEqualTo(TipoActividad.NUEVA_VERSION);
        assertThat(resultado.documentoId()).isEqualTo(1L);
        assertThat(resultado.codigoDocumento()).isEqualTo("PROC-001");
        assertThat(resultado.tituloDocumento()).isEqualTo("Título de prueba");
        assertThat(resultado.numeroVersion()).isEqualTo(2);
        assertThat(resultado.descripcionCambio()).isEqualTo("Corrección de erratas");
        assertThat(resultado.fechaPublicacion()).isEqualTo(fechaPublicacion);
        assertThat(resultado.publicadoPorId()).isEqualTo(9L);
        assertThat(resultado.publicadoPorNombres()).isEqualTo("Ana");
        assertThat(resultado.publicadoPorApellidos()).isEqualTo("Pérez");
    }
}

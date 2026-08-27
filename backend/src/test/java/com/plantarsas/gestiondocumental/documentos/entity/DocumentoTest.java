package com.plantarsas.gestiondocumental.documentos.entity;

import com.plantarsas.gestiondocumental.shared.enums.DocumentoAlcance;
import com.plantarsas.gestiondocumental.shared.enums.DocumentoEstado;
import com.plantarsas.gestiondocumental.subprogramas.entity.Subprograma;
import com.plantarsas.gestiondocumental.tiposdocumento.entity.TipoDocumento;
import com.plantarsas.gestiondocumental.usuarios.entity.Usuario;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DocumentoTest {

    @Test
    void constructor_debeNormalizarCodigoTituloYDescripcion() {
        Documento documento = new Documento(
                "  FOR-SST-001  ",
                "  Formato de inspeccion  ",
                "  Descripcion de prueba  ",
                mock(Subprograma.class),
                mock(TipoDocumento.class),
                mock(Usuario.class)
        );

        assertThat(documento.getCodigo()).isEqualTo("FOR-SST-001");
        assertThat(documento.getTitulo()).isEqualTo("Formato de inspeccion");
        assertThat(documento.getDescripcion()).isEqualTo("Descripcion de prueba");
    }

    @Test
    void constructor_debeConvertirDescripcionVaciaEnNull() {
        Documento documento = new Documento(
                "COD-001",
                "Titulo",
                "   ",
                mock(Subprograma.class),
                mock(TipoDocumento.class),
                mock(Usuario.class)
        );

        assertThat(documento.getDescripcion()).isNull();
    }

    @Test
    void constructor_debeIniciarEnPublicado() {
        Documento documento = new Documento(
                "COD-001",
                "Titulo",
                "Descripcion",
                mock(Subprograma.class),
                mock(TipoDocumento.class),
                mock(Usuario.class)
        );

        assertThat(documento.getEstado()).isEqualTo(DocumentoEstado.PUBLICADO);
    }

    @Test
    void cambiarEstado_debeActualizarElEstado() {
        Documento documento = new Documento(
                "COD-001",
                "Titulo",
                "Descripcion",
                mock(Subprograma.class),
                mock(TipoDocumento.class),
                mock(Usuario.class)
        );

        LocalDateTime ahora = LocalDateTime.of(2026, 8, 25, 13, 21, 57);
        documento.cambiarEstado(DocumentoEstado.OBSOLETO, ahora);

        assertThat(documento.getEstado()).isEqualTo(DocumentoEstado.OBSOLETO);
        assertThat(documento.getFechaObsolescencia()).isEqualTo(ahora);
    }

    @Test
    void constructor_debeAsignarAlcanceAreaResponsable() {
        Documento documento = new Documento(
                "COD-001",
                "Titulo",
                "Descripcion",
                mock(Subprograma.class),
                mock(TipoDocumento.class),
                mock(Usuario.class)
        );

        assertThat(documento.getAlcance()).isEqualTo(DocumentoAlcance.AREA_RESPONSABLE);
    }

    @Test
    void constructorConAlcance_debePermitirLosTresValoresDelEnum() {
        Documento areaResponsable = new Documento(
                "COD-001", "Titulo", "Descripcion",
                mock(Subprograma.class), mock(TipoDocumento.class), mock(Usuario.class),
                DocumentoAlcance.AREA_RESPONSABLE
        );
        Documento areasEspecificas = new Documento(
                "COD-002", "Titulo", "Descripcion",
                mock(Subprograma.class), mock(TipoDocumento.class), mock(Usuario.class),
                DocumentoAlcance.AREAS_ESPECIFICAS
        );
        Documento global = new Documento(
                "COD-003", "Titulo", "Descripcion",
                mock(Subprograma.class), mock(TipoDocumento.class), mock(Usuario.class),
                DocumentoAlcance.GLOBAL
        );

        assertThat(areaResponsable.getAlcance()).isEqualTo(DocumentoAlcance.AREA_RESPONSABLE);
        assertThat(areasEspecificas.getAlcance()).isEqualTo(DocumentoAlcance.AREAS_ESPECIFICAS);
        assertThat(global.getAlcance()).isEqualTo(DocumentoAlcance.GLOBAL);
    }

    @Test
    void cambiarEstado_aObsoleto_debeMarcarFechaYNoReiniciarlaSiPermanece() {
        Documento documento = documentoNuevo();
        LocalDateTime primera = LocalDateTime.of(2026, 8, 25, 13, 21, 57);
        LocalDateTime posterior = primera.plusDays(10);

        documento.cambiarEstado(DocumentoEstado.OBSOLETO, primera);
        documento.cambiarEstado(DocumentoEstado.OBSOLETO, posterior);

        assertThat(documento.getFechaObsolescencia()).isEqualTo(primera);
    }

    @Test
    void cambiarEstado_alSalirDeObsoleto_debeLimpiarFecha() {
        Documento documento = documentoNuevo();
        LocalDateTime obsolescencia = LocalDateTime.of(2026, 8, 25, 13, 21, 57);
        documento.cambiarEstado(DocumentoEstado.OBSOLETO, obsolescencia);

        documento.cambiarEstado(DocumentoEstado.PUBLICADO, obsolescencia.plusDays(1));

        assertThat(documento.getEstado()).isEqualTo(DocumentoEstado.PUBLICADO);
        assertThat(documento.getFechaObsolescencia()).isNull();
        assertThat(documento.fechaDisponibleEliminacion()).isNull();
        assertThat(documento.esAptoParaEliminacion(obsolescencia.plusYears(3))).isFalse();
    }

    @Test
    void cambiarEstado_nuevaObsolescenciaTrasReactivar_debeRegistrarFechaNueva() {
        Documento documento = documentoNuevo();
        LocalDateTime primera = LocalDateTime.of(2024, 8, 25, 13, 21, 57);
        LocalDateTime reactivacion = LocalDateTime.of(2026, 8, 25, 13, 21, 57);
        LocalDateTime segunda = LocalDateTime.of(2026, 8, 26, 8, 0, 0);

        documento.cambiarEstado(DocumentoEstado.OBSOLETO, primera);
        documento.cambiarEstado(DocumentoEstado.PUBLICADO, reactivacion);
        documento.cambiarEstado(DocumentoEstado.OBSOLETO, segunda);

        assertThat(documento.getFechaObsolescencia()).isEqualTo(segunda);
        assertThat(documento.fechaDisponibleEliminacion()).isEqualTo(segunda.plusYears(2));
    }

    @Test
    void esAptoParaEliminacion_exactamenteAntesDeDosAnos_noEsApto() {
        Documento documento = documentoNuevo();
        LocalDateTime obsolescencia = LocalDateTime.of(2024, 8, 25, 13, 21, 57);
        documento.cambiarEstado(DocumentoEstado.OBSOLETO, obsolescencia);

        assertThat(documento.esAptoParaEliminacion(obsolescencia.plusYears(2).minusNanos(1))).isFalse();
    }

    @Test
    void esAptoParaEliminacion_exactamenteAlCumplirDosAnos_esApto() {
        Documento documento = documentoNuevo();
        LocalDateTime obsolescencia = LocalDateTime.of(2024, 8, 25, 13, 21, 57);
        documento.cambiarEstado(DocumentoEstado.OBSOLETO, obsolescencia);

        assertThat(documento.esAptoParaEliminacion(obsolescencia.plusYears(2))).isTrue();
    }

    @Test
    void esAptoParaEliminacion_despuesDeDosAnos_esApto() {
        Documento documento = documentoNuevo();
        LocalDateTime obsolescencia = LocalDateTime.of(2024, 8, 25, 13, 21, 57);
        documento.cambiarEstado(DocumentoEstado.OBSOLETO, obsolescencia);

        assertThat(documento.esAptoParaEliminacion(obsolescencia.plusYears(2).plusDays(1))).isTrue();
    }

    @Test
    void actualizarMetadatos_deUnObsoleto_noReiniciaElPlazo() {
        Documento documento = documentoNuevo();
        LocalDateTime obsolescencia = LocalDateTime.of(2024, 8, 25, 13, 21, 57);
        documento.cambiarEstado(DocumentoEstado.OBSOLETO, obsolescencia);

        documento.actualizarMetadatos(
                "COD-002",
                "Titulo nuevo",
                "Descripcion nueva",
                mock(Subprograma.class),
                mock(TipoDocumento.class),
                DocumentoAlcance.GLOBAL
        );

        assertThat(documento.getFechaObsolescencia()).isEqualTo(obsolescencia);
        assertThat(documento.getEstado()).isEqualTo(DocumentoEstado.OBSOLETO);
    }

    private Documento documentoNuevo() {
        return new Documento(
                "COD-001",
                "Titulo",
                "Descripcion",
                mock(Subprograma.class),
                mock(TipoDocumento.class),
                mock(Usuario.class)
        );
    }
}

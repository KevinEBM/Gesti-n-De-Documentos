package com.plantarsas.gestiondocumental.documentos.entity;

import com.plantarsas.gestiondocumental.shared.enums.DocumentoEstado;
import com.plantarsas.gestiondocumental.subprogramas.entity.Subprograma;
import com.plantarsas.gestiondocumental.tiposdocumento.entity.TipoDocumento;
import com.plantarsas.gestiondocumental.usuarios.entity.Usuario;
import org.junit.jupiter.api.Test;

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

        documento.cambiarEstado(DocumentoEstado.OBSOLETO);

        assertThat(documento.getEstado()).isEqualTo(DocumentoEstado.OBSOLETO);
    }
}

package com.plantarsas.gestiondocumental.documentos.entity;

import com.plantarsas.gestiondocumental.usuarios.entity.Usuario;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class VersionDocumentoTest {

    @Test
    void constructor_debeIniciarVigente() {
        VersionDocumento version = new VersionDocumento(
                mock(Documento.class),
                1,
                "informe.pdf",
                "b3f1c2.pdf",
                "b3f1c2.pdf",
                "application/pdf",
                1024L,
                "Publicacion inicial",
                mock(Usuario.class)
        );

        assertThat(version.isVigente()).isTrue();
    }

    @Test
    void marcarNoVigente_debeCambiarAFalse() {
        VersionDocumento version = new VersionDocumento(
                mock(Documento.class),
                1,
                "informe.pdf",
                "b3f1c2.pdf",
                "b3f1c2.pdf",
                "application/pdf",
                1024L,
                "Publicacion inicial",
                mock(Usuario.class)
        );

        version.marcarNoVigente();

        assertThat(version.isVigente()).isFalse();
    }

    @Test
    void constructor_debeConservarMetadatosDelArchivo() {
        Documento documento = mock(Documento.class);
        Usuario usuario = mock(Usuario.class);

        VersionDocumento version = new VersionDocumento(
                documento,
                2,
                "informe-original.docx",
                "a1b2c3.docx",
                "a1b2c3.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                2048L,
                "Actualizacion de encabezado",
                usuario
        );

        assertThat(version.getDocumento()).isSameAs(documento);
        assertThat(version.getNumeroVersion()).isEqualTo(2);
        assertThat(version.getNombreArchivoOriginal()).isEqualTo("informe-original.docx");
        assertThat(version.getNombreArchivoAlmacenado()).isEqualTo("a1b2c3.docx");
        assertThat(version.getRutaArchivo()).isEqualTo("a1b2c3.docx");
        assertThat(version.getTipoMime()).isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertThat(version.getTamanoBytes()).isEqualTo(2048L);
        assertThat(version.getDescripcionCambio()).isEqualTo("Actualizacion de encabezado");
        assertThat(version.getPublicadoPor()).isSameAs(usuario);
    }
}

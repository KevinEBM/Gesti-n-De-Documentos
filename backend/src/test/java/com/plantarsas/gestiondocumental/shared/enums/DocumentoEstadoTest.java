package com.plantarsas.gestiondocumental.shared.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentoEstadoTest {

    @Test
    void permiteTransicionA_debePermitirMismaEstado() {
        assertThat(DocumentoEstado.PUBLICADO.permiteTransicionA(DocumentoEstado.PUBLICADO)).isTrue();
        assertThat(DocumentoEstado.INACTIVO.permiteTransicionA(DocumentoEstado.INACTIVO)).isTrue();
        assertThat(DocumentoEstado.OBSOLETO.permiteTransicionA(DocumentoEstado.OBSOLETO)).isTrue();
    }

    @Test
    void permiteTransicionA_debePermitirTransicionesValidas() {
        assertThat(DocumentoEstado.PUBLICADO.permiteTransicionA(DocumentoEstado.INACTIVO)).isTrue();
        assertThat(DocumentoEstado.PUBLICADO.permiteTransicionA(DocumentoEstado.OBSOLETO)).isTrue();
        assertThat(DocumentoEstado.INACTIVO.permiteTransicionA(DocumentoEstado.PUBLICADO)).isTrue();
        assertThat(DocumentoEstado.INACTIVO.permiteTransicionA(DocumentoEstado.OBSOLETO)).isTrue();
        assertThat(DocumentoEstado.OBSOLETO.permiteTransicionA(DocumentoEstado.PUBLICADO)).isTrue();
    }

    @Test
    void permiteTransicionA_debeRechazarTransicionInvalida() {
        assertThat(DocumentoEstado.OBSOLETO.permiteTransicionA(DocumentoEstado.INACTIVO)).isFalse();
    }

    @Test
    void permitePublicarNuevaVersion_debePermitirPublicadoEInactivo() {
        assertThat(DocumentoEstado.PUBLICADO.permitePublicarNuevaVersion()).isTrue();
        assertThat(DocumentoEstado.INACTIVO.permitePublicarNuevaVersion()).isTrue();
    }

    @Test
    void permitePublicarNuevaVersion_debeRechazarObsoleto() {
        assertThat(DocumentoEstado.OBSOLETO.permitePublicarNuevaVersion()).isFalse();
    }

    @Test
    void permiteEditarPublicacion_debePermitirPublicadoEInactivo() {
        assertThat(DocumentoEstado.PUBLICADO.permiteEditarPublicacion()).isTrue();
        assertThat(DocumentoEstado.INACTIVO.permiteEditarPublicacion()).isTrue();
    }

    @Test
    void permiteEditarPublicacion_debeRechazarObsoleto() {
        assertThat(DocumentoEstado.OBSOLETO.permiteEditarPublicacion()).isFalse();
    }
}

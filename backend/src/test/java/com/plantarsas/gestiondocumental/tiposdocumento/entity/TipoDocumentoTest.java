package com.plantarsas.gestiondocumental.tiposdocumento.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TipoDocumentoTest {

    @Test
    void constructor_debeNormalizarCodigoAMayusculasYTrim() {
        TipoDocumento tipo = new TipoDocumento("  ode  ", "  Plantilla  ", "  Descripcion  ");

        assertThat(tipo.getCodigo()).isEqualTo("ODE");
        assertThat(tipo.getNombre()).isEqualTo("Plantilla");
        assertThat(tipo.getDescripcion()).isEqualTo("Descripcion");
        assertThat(tipo.isActivo()).isTrue();
    }

    @Test
    void actualizarDatos_debePermitirElMismoCodigoQueOtroTipo() {
        TipoDocumento plantilla = new TipoDocumento("ODE", "Plantilla", null);
        TipoDocumento imagenes = new TipoDocumento("IMG", "Imágenes", null);

        imagenes.actualizarDatos("ode", "Imágenes", null);

        assertThat(plantilla.getCodigo()).isEqualTo("ODE");
        assertThat(imagenes.getCodigo()).isEqualTo("ODE");
        assertThat(plantilla.getNombre()).isNotEqualTo(imagenes.getNombre());
    }
}

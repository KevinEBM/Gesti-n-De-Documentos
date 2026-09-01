package com.plantarsas.gestiondocumental.shared.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentoAlcanceTest {

    @Test
    void values_debeContenerExactamenteYEnOrdenLosTresAlcances() {
        assertThat(DocumentoAlcance.values()).containsExactly(
                DocumentoAlcance.AREA_RESPONSABLE,
                DocumentoAlcance.AREAS_ESPECIFICAS,
                DocumentoAlcance.GLOBAL
        );
    }
}

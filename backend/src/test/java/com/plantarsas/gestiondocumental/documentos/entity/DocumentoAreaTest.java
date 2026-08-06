package com.plantarsas.gestiondocumental.documentos.entity;

import com.plantarsas.gestiondocumental.areas.entity.Area;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DocumentoAreaTest {

    @Test
    void constructor_debeIniciarConEsPrincipalTrue() {
        DocumentoArea documentoArea = new DocumentoArea(mock(Documento.class), mock(Area.class));

        assertThat(documentoArea.isEsPrincipal()).isTrue();
    }

    @Test
    void constructor_debeConservarDocumentoYArea() {
        Documento documento = mock(Documento.class);
        Area area = mock(Area.class);

        DocumentoArea documentoArea = new DocumentoArea(documento, area);

        assertThat(documentoArea.getDocumento()).isSameAs(documento);
        assertThat(documentoArea.getArea()).isSameAs(area);
    }
}

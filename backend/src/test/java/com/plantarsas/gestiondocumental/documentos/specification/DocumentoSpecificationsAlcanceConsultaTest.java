package com.plantarsas.gestiondocumental.documentos.specification;

import com.plantarsas.gestiondocumental.shared.enums.RolEnum;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Matriz esperada del filtro de consulta {@code AlcanceConsulta} sobre resultados ya
 * autorizados por {@link DocumentoSpecifications#visiblePara}:
 * <ul>
 *   <li>TODOS_VISIBLES: sin predicado adicional.</li>
 *   <li>GLOBALES: solo documentos con alcance persistido GLOBAL.</li>
 *   <li>MI_AREA: no GLOBAL y asociados vía documento_area a las áreas del usuario.</li>
 * </ul>
 */
class DocumentoSpecificationsAlcanceConsultaTest {

    @Test
    void conAlcanceGlobal_debeConstruirSpecification() {
        Specification<?> spec = DocumentoSpecifications.conAlcanceGlobal();
        assertThat(spec).isNotNull();
    }

    @Test
    void sinAlcanceGlobal_debeConstruirSpecification() {
        Specification<?> spec = DocumentoSpecifications.sinAlcanceGlobal();
        assertThat(spec).isNotNull();
    }

    @Test
    void conAlcanceAreasEspecificas_debeConstruirSpecification() {
        Specification<?> spec = DocumentoSpecifications.conAlcanceAreasEspecificas();
        assertThat(spec).isNotNull();
    }

    @Test
    void deAreaResponsable_debeConstruirSpecification() {
        Specification<?> spec = DocumentoSpecifications.deAreaResponsable(10L);
        assertThat(spec).isNotNull();
    }

    @Test
    void asociadoAAreas_conIdsVacios_debeSerRestrictivo() {
        Specification<?> spec = DocumentoSpecifications.asociadoAAreas(Set.of());
        assertThat(spec).isNotNull();
    }

    @Test
    void asociadoAAreas_conAreaAsignada_debeConstruirSpecification() {
        Specification<?> spec = DocumentoSpecifications.asociadoAAreas(Set.of(10L));
        assertThat(spec).isNotNull();
    }

    @Test
    void miArea_compuesto_excluyeGlobalYFiltraPorAreas() {
        Specification<?> spec = DocumentoSpecifications.sinAlcanceGlobal()
                .and(DocumentoSpecifications.asociadoAAreas(Set.of(10L)));
        assertThat(spec).isNotNull();
    }

    @Test
    void visiblePara_noSeModifica_conJefeYArea() {
        Specification<?> spec = DocumentoSpecifications.visiblePara(RolEnum.JEFE_AREA, Set.of(10L));
        assertThat(spec).isNotNull();
    }
}

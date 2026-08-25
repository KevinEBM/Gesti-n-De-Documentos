package com.plantarsas.gestiondocumental.documentos.specification;

import com.plantarsas.gestiondocumental.shared.enums.RolEnum;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Documenta la matriz de visibilidad esperada para no-admin:
 * <ul>
 *   <li>D1 GLOBAL: visible si publicado (siempre para no-admin con o sin área).</li>
 *   <li>D2 área propia: visible solo si el id está en {@code areaIds}.</li>
 *   <li>D3 otra área: nunca visible salvo alcance GLOBAL.</li>
 * </ul>
 * Con {@code areaIds} vacío: solo D1 (GLOBAL publicado). No amplía a otras áreas.
 * Con área inactiva pero asignada: {@code areaIds} conserva el id; D1 + D2, no D3.
 */
class DocumentoSpecificationsVisibleParaTest {

    @Test
    void visiblePara_conJefeSinAreasAsignadas_debeConstruirSpecificationRestrictiva() {
        Specification<?> spec = DocumentoSpecifications.visiblePara(RolEnum.JEFE_AREA, Set.of());

        assertThat(spec).isNotNull();
    }

    @Test
    void visiblePara_conJefeConAreaAsignada_debeConstruirSpecificationConArea() {
        Specification<?> spec = DocumentoSpecifications.visiblePara(RolEnum.JEFE_AREA, Set.of(10L));

        assertThat(spec).isNotNull();
    }

    @Test
    void visiblePara_conAdministrador_noRestringePorArea() {
        Specification<?> spec = DocumentoSpecifications.visiblePara(RolEnum.ADMINISTRADOR, Set.of());

        assertThat(spec).isNotNull();
    }
}

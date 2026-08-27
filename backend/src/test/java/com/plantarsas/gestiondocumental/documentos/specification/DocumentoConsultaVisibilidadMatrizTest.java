package com.plantarsas.gestiondocumental.documentos.specification;

import com.plantarsas.gestiondocumental.shared.enums.AlcanceConsulta;
import com.plantarsas.gestiondocumental.shared.enums.DocumentoAlcance;
import com.plantarsas.gestiondocumental.shared.enums.DocumentoEstado;
import com.plantarsas.gestiondocumental.shared.enums.RolEnum;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Matriz de visibilidad + filtro de Área responsable alineada con
 * {@link DocumentoSpecifications#visiblePara} y {@link DocumentoSpecifications#deArea}.
 * No ejecuta SQL: fija el contrato que las Specifications deben implementar.
 */
class DocumentoConsultaVisibilidadMatrizTest {

    private static final long AREA_A = 1L;
    private static final long AREA_B = 2L;

    @Test
    void filtrarPorAreaResponsableB_noAmpliaPermisosDelUsuarioDeA() {
        assertThat(apareceEnListado(
                doc(AREA_B, DocumentoAlcance.GLOBAL, Set.of()),
                AREA_B
        )).isTrue();

        assertThat(apareceEnListado(
                doc(AREA_B, DocumentoAlcance.AREAS_ESPECIFICAS, Set.of(AREA_A)),
                AREA_B
        )).isTrue();

        assertThat(apareceEnListado(
                doc(AREA_B, DocumentoAlcance.AREAS_ESPECIFICAS, Set.of()),
                AREA_B
        )).isFalse();
    }

    @Test
    void areaAdicionalNoCuentaComoAreaResponsable() {
        DocumentoEscenario documento = new DocumentoEscenario(
                DocumentoEstado.PUBLICADO,
                DocumentoAlcance.AREAS_ESPECIFICAS,
                AREA_A,
                Set.of(AREA_B)
        );

        assertThat(apareceEnListado(documento, AREA_A)).isTrue();
        assertThat(apareceEnListado(documento, AREA_B)).isFalse();
    }

    @Test
    void alcanceTodosIncluyeLosTresAlcancesAutorizados() {
        assertThat(apareceEnListado(
                doc(AREA_A, DocumentoAlcance.AREA_RESPONSABLE, Set.of()),
                null,
                AlcanceConsulta.TODOS_VISIBLES
        )).isTrue();
        assertThat(apareceEnListado(
                doc(AREA_A, DocumentoAlcance.AREAS_ESPECIFICAS, Set.of()),
                null,
                AlcanceConsulta.TODOS_VISIBLES
        )).isTrue();
        assertThat(apareceEnListado(
                doc(AREA_B, DocumentoAlcance.GLOBAL, Set.of()),
                null,
                AlcanceConsulta.TODOS_VISIBLES
        )).isTrue();
    }

    @Test
    void alcanceGlobalExcluyeAreaResponsableYAreasEspecificas() {
        assertThat(apareceEnListado(
                doc(AREA_B, DocumentoAlcance.GLOBAL, Set.of()),
                null,
                AlcanceConsulta.GLOBALES
        )).isTrue();
        assertThat(apareceEnListado(
                doc(AREA_A, DocumentoAlcance.AREA_RESPONSABLE, Set.of()),
                null,
                AlcanceConsulta.GLOBALES
        )).isFalse();
        assertThat(apareceEnListado(
                doc(AREA_A, DocumentoAlcance.AREAS_ESPECIFICAS, Set.of()),
                null,
                AlcanceConsulta.GLOBALES
        )).isFalse();
    }

    @Test
    void alcanceAreasEspecificasExcluyeAreaResponsableYGlobal() {
        assertThat(apareceEnListado(
                doc(AREA_A, DocumentoAlcance.AREAS_ESPECIFICAS, Set.of()),
                null,
                AlcanceConsulta.AREAS_ESPECIFICAS
        )).isTrue();
        assertThat(apareceEnListado(
                doc(AREA_A, DocumentoAlcance.AREA_RESPONSABLE, Set.of()),
                null,
                AlcanceConsulta.AREAS_ESPECIFICAS
        )).isFalse();
        assertThat(apareceEnListado(
                doc(AREA_B, DocumentoAlcance.GLOBAL, Set.of()),
                null,
                AlcanceConsulta.AREAS_ESPECIFICAS
        )).isFalse();
    }

    @Test
    void administradorSigueViendoDocumentosNoPublicados() {
        DocumentoEscenario inactivo = new DocumentoEscenario(
                DocumentoEstado.INACTIVO,
                DocumentoAlcance.AREA_RESPONSABLE,
                AREA_B,
                Set.of()
        );

        assertThat(esVisible(RolEnum.ADMINISTRADOR, Set.of(), inactivo)).isTrue();
        assertThat(esVisible(RolEnum.JEFE_AREA, Set.of(AREA_A), inactivo)).isFalse();
        assertThat(esVisible(RolEnum.ADMINISTRATIVO, Set.of(AREA_A), inactivo)).isFalse();
    }

    @Test
    void deAreaResponsableConstruyeSpecification() {
        assertThat(DocumentoSpecifications.deArea(AREA_B)).isNotNull();
        assertThat(DocumentoSpecifications.deAreaResponsable(AREA_B)).isNotNull();
        assertThat(DocumentoSpecifications.conAlcanceAreasEspecificas()).isNotNull();
    }

    private static boolean apareceEnListado(DocumentoEscenario documento, Long filtroAreaResponsable) {
        return apareceEnListado(documento, filtroAreaResponsable, AlcanceConsulta.TODOS_VISIBLES);
    }

    private static boolean apareceEnListado(
            DocumentoEscenario documento,
            Long filtroAreaResponsable,
            AlcanceConsulta alcanceConsulta
    ) {
        if (!esVisible(RolEnum.JEFE_AREA, Set.of(AREA_A), documento)) {
            return false;
        }
        if (alcanceConsulta == AlcanceConsulta.GLOBALES
                && documento.alcance != DocumentoAlcance.GLOBAL) {
            return false;
        }
        if (alcanceConsulta == AlcanceConsulta.AREAS_ESPECIFICAS
                && documento.alcance != DocumentoAlcance.AREAS_ESPECIFICAS) {
            return false;
        }
        if (filtroAreaResponsable == null) {
            return true;
        }
        return documento.areaPrincipalId == filtroAreaResponsable;
    }

    /**
     * Réplica del contrato de {@code visiblePara} para no-admin:
     * PUBLICADO y (GLOBAL o asociado por documento_area, principal o adicional).
     */
    private static boolean esVisible(RolEnum rol, Set<Long> areaIdsUsuario, DocumentoEscenario documento) {
        if (rol == RolEnum.ADMINISTRADOR) {
            return true;
        }
        if (documento.estado != DocumentoEstado.PUBLICADO) {
            return false;
        }
        if (documento.alcance == DocumentoAlcance.GLOBAL) {
            return true;
        }
        if (areaIdsUsuario.contains(documento.areaPrincipalId)) {
            return true;
        }
        return documento.areasAdicionalesIds.stream().anyMatch(areaIdsUsuario::contains);
    }

    private static DocumentoEscenario doc(
            long areaPrincipalId,
            DocumentoAlcance alcance,
            Set<Long> areasAdicionalesIds
    ) {
        return new DocumentoEscenario(
                DocumentoEstado.PUBLICADO, alcance, areaPrincipalId, areasAdicionalesIds
        );
    }

    private record DocumentoEscenario(
            DocumentoEstado estado,
            DocumentoAlcance alcance,
            long areaPrincipalId,
            Set<Long> areasAdicionalesIds
    ) {
    }
}

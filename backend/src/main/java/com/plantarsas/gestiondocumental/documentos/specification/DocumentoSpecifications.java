package com.plantarsas.gestiondocumental.documentos.specification;

import com.plantarsas.gestiondocumental.documentos.entity.Documento;
import com.plantarsas.gestiondocumental.documentos.entity.DocumentoArea;
import com.plantarsas.gestiondocumental.shared.enums.DocumentoAlcance;
import com.plantarsas.gestiondocumental.shared.enums.DocumentoEstado;
import com.plantarsas.gestiondocumental.shared.enums.RolEnum;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

/**
 * Fuente única de verdad de las condiciones de consulta de {@link Documento}.
 * No autoriza fuera de aquí ni en ningún otro punto del código: tanto el listado
 * como el detalle por id deben componer sus consultas a partir de estos predicados.
 */
public final class DocumentoSpecifications {

    private static final char CARACTER_ESCAPE_LIKE = '\\';

    private DocumentoSpecifications() {
    }

    public static Specification<Documento> idIgual(Long documentoId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("id"), documentoId);
    }

    public static Specification<Documento> visiblePara(RolEnum rol, Set<Long> areaIds) {
        if (rol == RolEnum.ADMINISTRADOR) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        }

        return (root, query, criteriaBuilder) -> {
            Predicate publicado = criteriaBuilder.equal(root.get("estado"), DocumentoEstado.PUBLICADO);
            Predicate esGlobal = criteriaBuilder.equal(root.get("alcance"), DocumentoAlcance.GLOBAL);
            Predicate asociadoAAreaDelUsuario = areaIds.isEmpty()
                    ? criteriaBuilder.disjunction()
                    : criteriaBuilder.exists(
                            subconsultaAsociacionConAreas(root, query, criteriaBuilder, areaIds)
                    );

            return criteriaBuilder.and(publicado, criteriaBuilder.or(esGlobal, asociadoAAreaDelUsuario));
        };
    }

    /**
     * Agrega el fetch de subprograma/tipoDocumento necesario para el listado, evitando N+1.
     * No decide autorización ni conoce query de conteo: el llamador debe pasarla únicamente
     * como parte de la specification de contenido, nunca como countSpec (ver
     * DocumentoConsultaServiceImpl#listar, que usa findAll(spec, countSpec, pageable)).
     * INNER porque subprograma y tipoDocumento son asociaciones obligatorias en Documento
     * (@ManyToOne(optional = false), @JoinColumn(nullable = false)): nunca faltan.
     */
    public static Specification<Documento> conRelacionesDeResumen() {
        return (root, query, criteriaBuilder) -> {
            root.fetch("subprograma", JoinType.INNER);
            root.fetch("tipoDocumento", JoinType.INNER);
            return criteriaBuilder.conjunction();
        };
    }

    /**
     * Filtro de "Área responsable": únicamente {@code documento_area.es_principal = true}.
     * Las áreas adicionales no son responsables. La autorización sigue siendo
     * exclusivamente {@link #visiblePara}; este predicado se compone con AND sobre ella.
     */
    public static Specification<Documento> deArea(Long areaId) {
        return deAreaResponsable(areaId);
    }

    public static Specification<Documento> deAreaResponsable(Long areaId) {
        return (root, query, criteriaBuilder) -> {
            Subquery<Long> subconsulta = query.subquery(Long.class);
            Root<DocumentoArea> documentoArea = subconsulta.from(DocumentoArea.class);
            subconsulta.select(documentoArea.get("idDocumentoArea"));
            subconsulta.where(
                    criteriaBuilder.equal(documentoArea.get("documento").get("id"), root.get("id")),
                    criteriaBuilder.equal(documentoArea.get("area").get("id"), areaId),
                    criteriaBuilder.isTrue(documentoArea.get("esPrincipal"))
            );
            return criteriaBuilder.exists(subconsulta);
        };
    }

    public static Specification<Documento> conAlcance(DocumentoAlcance alcance) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("alcance"), alcance);
    }

    public static Specification<Documento> conAlcanceGlobal() {
        return conAlcance(DocumentoAlcance.GLOBAL);
    }

    public static Specification<Documento> conAlcanceAreasEspecificas() {
        return conAlcance(DocumentoAlcance.AREAS_ESPECIFICAS);
    }

    public static Specification<Documento> sinAlcanceGlobal() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.notEqual(root.get("alcance"), DocumentoAlcance.GLOBAL);
    }

    /**
     * Documento asociado estructuralmente a alguna de las áreas indicadas vía documento_area.
     * No implica visibilidad por sí solo; se compone con {@link #visiblePara}.
     */
    public static Specification<Documento> asociadoAAreas(Set<Long> areaIds) {
        if (areaIds.isEmpty()) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.disjunction();
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.exists(
                subconsultaAsociacionConAreas(root, query, criteriaBuilder, areaIds)
        );
    }

    public static Specification<Documento> deSubprograma(Long subprogramaId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("subprograma").get("id"), subprogramaId);
    }

    public static Specification<Documento> deTipoDocumento(Long tipoDocumentoId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("tipoDocumento").get("id"), tipoDocumentoId);
    }

    public static Specification<Documento> conEstado(DocumentoEstado estado) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("estado"), estado);
    }

    public static Specification<Documento> creadoDesde(LocalDateTime desde) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("fechaCreacion"), desde);
    }

    public static Specification<Documento> creadoAntesDe(LocalDateTime limiteExclusivo) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThan(root.get("fechaCreacion"), limiteExclusivo);
    }

    public static Specification<Documento> codigoContiene(String codigo) {
        return contieneTexto("codigo", codigo);
    }

    public static Specification<Documento> tituloContiene(String titulo) {
        return contieneTexto("titulo", titulo);
    }

    /**
     * LIKE '%valor%' case-insensitive con escape explícito de los comodines propios
     * de LIKE (%, _) mediante el mecanismo de escape del propio CriteriaBuilder, para
     * que un usuario que escriba "%" o "_" busque ese carácter literal y no lo use
     * accidentalmente como comodín.
     */
    private static Specification<Documento> contieneTexto(String campo, String valor) {
        String patron = "%" + escaparComodinesLike(valor.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(
                criteriaBuilder.lower(root.get(campo)), patron, CARACTER_ESCAPE_LIKE
        );
    }

    private static String escaparComodinesLike(String valor) {
        return valor
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private static Subquery<Long> subconsultaAsociacionConAreas(
            Root<Documento> root,
            CriteriaQuery<?> query,
            CriteriaBuilder criteriaBuilder,
            Set<Long> areaIds
    ) {
        Subquery<Long> subconsulta = query.subquery(Long.class);
        Root<DocumentoArea> documentoArea = subconsulta.from(DocumentoArea.class);
        subconsulta.select(documentoArea.get("idDocumentoArea"));
        subconsulta.where(
                criteriaBuilder.equal(documentoArea.get("documento").get("id"), root.get("id")),
                documentoArea.get("area").get("id").in(areaIds)
        );
        return subconsulta;
    }
}

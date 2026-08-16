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

import java.util.Set;

/**
 * Fuente única de verdad de las condiciones de consulta de {@link Documento}.
 * No autoriza fuera de aquí ni en ningún otro punto del código: tanto el listado
 * como el detalle por id deben componer sus consultas a partir de estos predicados.
 */
public final class DocumentoSpecifications {

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

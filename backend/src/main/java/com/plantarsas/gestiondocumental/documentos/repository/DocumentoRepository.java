package com.plantarsas.gestiondocumental.documentos.repository;

import com.plantarsas.gestiondocumental.documentos.entity.Documento;
import com.plantarsas.gestiondocumental.shared.enums.DocumentoEstado;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Acceso a la base de datos para los documentos: buscarlos por código
 * o id, comprobar que un código no esté repetido, y bloquear un
 * documento durante una operación para evitar que dos publicaciones
 * de una nueva versión choquen entre sí.
 */
public interface DocumentoRepository extends JpaRepository<Documento, Long>, JpaSpecificationExecutor<Documento> {

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    Optional<Documento> findByCodigoIgnoreCase(String codigo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Documento d WHERE d.id = :id")
    Optional<Documento> buscarPorIdConBloqueoPesimista(@Param("id") Long id);

    long countByEstado(DocumentoEstado estado);

    boolean existsBySubprograma_Id(Long subprogramaId);
}

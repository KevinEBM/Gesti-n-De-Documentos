package com.plantarsas.gestiondocumental.documentos.repository;

import com.plantarsas.gestiondocumental.documentos.entity.VersionDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VersionDocumentoRepository extends JpaRepository<VersionDocumento, Long> {

    Optional<VersionDocumento> findByDocumento_IdAndVigenteTrue(Long documentoId);

    List<VersionDocumento> findByDocumento_IdOrderByNumeroVersionDesc(Long documentoId);

    Optional<VersionDocumento> findByIdAndDocumento_Id(Long id, Long documentoId);

    List<VersionDocumento> findTop10ByOrderByFechaPublicacionDescIdDesc();

    @Query("SELECT COALESCE(MAX(v.numeroVersion), 0) FROM VersionDocumento v WHERE v.documento.id = :documentoId")
    int obtenerUltimoNumeroVersion(@Param("documentoId") Long documentoId);
}

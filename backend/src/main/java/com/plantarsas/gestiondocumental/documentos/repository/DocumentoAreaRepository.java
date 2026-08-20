package com.plantarsas.gestiondocumental.documentos.repository;

import com.plantarsas.gestiondocumental.documentos.entity.DocumentoArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentoAreaRepository extends JpaRepository<DocumentoArea, Long> {

    Optional<DocumentoArea> findByDocumento_IdAndEsPrincipalTrue(Long documentoId);

    List<DocumentoArea> findAllByDocumento_IdAndEsPrincipalFalse(Long documentoId);

    void deleteAllByDocumento_IdAndEsPrincipalFalse(Long documentoId);

    void deleteByDocumento_IdAndEsPrincipalTrue(Long documentoId);

    List<DocumentoArea> findByArea_Id(Long areaId);
}

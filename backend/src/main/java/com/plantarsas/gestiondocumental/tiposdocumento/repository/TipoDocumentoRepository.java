package com.plantarsas.gestiondocumental.tiposdocumento.repository;

import com.plantarsas.gestiondocumental.tiposdocumento.entity.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipoDocumentoRepository extends JpaRepository<TipoDocumento, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    List<TipoDocumento> findAllByOrderByNombreAsc();

    List<TipoDocumento> findByActivoTrueOrderByNombreAsc();
}

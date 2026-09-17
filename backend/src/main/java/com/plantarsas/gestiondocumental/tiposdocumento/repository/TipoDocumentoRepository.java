package com.plantarsas.gestiondocumental.tiposdocumento.repository;

import com.plantarsas.gestiondocumental.tiposdocumento.entity.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acceso a la base de datos para los tipos de documento: comprobar
 * que un nombre no esté repetido, y listar todos o solo los activos,
 * ordenados por nombre.
 */
public interface TipoDocumentoRepository extends JpaRepository<TipoDocumento, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    List<TipoDocumento> findAllByOrderByNombreAsc();

    List<TipoDocumento> findByActivoTrueOrderByNombreAsc();
}

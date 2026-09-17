package com.plantarsas.gestiondocumental.areas.repository;

import com.plantarsas.gestiondocumental.areas.entity.Area;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * Acceso a la base de datos para las áreas: comprobar que un código o
 * nombre no estén repetidos, y buscar varias áreas a la vez por sus id.
 */
public interface AreaRepository extends JpaRepository<Area, Long> {

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    List<Area> findByIdIn(Collection<Long> ids);
}

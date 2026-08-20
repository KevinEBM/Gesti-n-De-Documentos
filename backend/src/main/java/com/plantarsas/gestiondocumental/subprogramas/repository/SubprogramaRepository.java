package com.plantarsas.gestiondocumental.subprogramas.repository;

import com.plantarsas.gestiondocumental.subprogramas.entity.Subprograma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SubprogramaRepository extends JpaRepository<Subprograma, Long> {

    boolean existsByAreaIdAndNombreIgnoreCase(Long areaId, String nombre);

    boolean existsByAreaIdAndNombreIgnoreCaseAndIdNot(Long areaId, String nombre, Long id);

    List<Subprograma> findByAreaIdAndActivoTrueOrderByNombreAsc(Long areaId);

    List<Subprograma> findByArea_IdInOrderByNombreAsc(Collection<Long> areaIds);
}

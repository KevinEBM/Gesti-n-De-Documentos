package com.plantarsas.gestiondocumental.roles.repository;

import com.plantarsas.gestiondocumental.roles.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a la base de datos para los roles del sistema.
 */
public interface RolRepository extends JpaRepository<Rol, Long> {
}

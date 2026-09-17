package com.plantarsas.gestiondocumental.roles.service;

import com.plantarsas.gestiondocumental.roles.entity.Rol;

/**
 * Contrato mínimo para que otros módulos, como usuarios, obtengan un
 * rol activo por su id sin depender del resto de las operaciones de
 * consulta de roles.
 */
public interface RolLookupService {

    Rol obtenerActivoPorId(Long id);
}

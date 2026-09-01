package com.plantarsas.gestiondocumental.roles.service;

import com.plantarsas.gestiondocumental.roles.entity.Rol;

public interface RolLookupService {

    Rol obtenerActivoPorId(Long id);
}

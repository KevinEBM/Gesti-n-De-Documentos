package com.plantarsas.gestiondocumental.roles.service;

import com.plantarsas.gestiondocumental.exception.BusinessException;
import com.plantarsas.gestiondocumental.exception.ResourceNotFoundException;
import com.plantarsas.gestiondocumental.roles.dto.RolResponse;
import com.plantarsas.gestiondocumental.roles.entity.Rol;
import com.plantarsas.gestiondocumental.roles.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RolServiceImpl implements RolService, RolLookupService {

    private final RolRepository rolRepository;

    @Override
    public List<RolResponse> listar() {
        return rolRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public RolResponse obtenerPorId(Long id) {
        return toResponse(obtenerEntidadPorId(id));
    }

    @Override
    public Rol obtenerActivoPorId(Long id) {
        Rol rol = obtenerEntidadPorId(id);
        if (!rol.isActivo()) {
            throw new BusinessException("El rol '" + rol.getNombre() + "' está inactivo y no puede asignarse");
        }
        return rol;
    }

    private Rol obtenerEntidadPorId(Long id) {
        return rolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un rol con id " + id
                ));
    }

    private RolResponse toResponse(Rol rol) {
        return new RolResponse(rol.getId(), rol.getNombre(), rol.getDescripcion(), rol.isActivo());
    }
}

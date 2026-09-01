package com.plantarsas.gestiondocumental.roles.controller;

import com.plantarsas.gestiondocumental.roles.dto.RolResponse;
import com.plantarsas.gestiondocumental.roles.service.RolService;
import com.plantarsas.gestiondocumental.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class RolController {

    private final RolService rolService;

    @GetMapping
    public ApiResponse<List<RolResponse>> listar() {
        return ApiResponse.exitosa(rolService.listar());
    }

    @GetMapping("/{id}")
    public ApiResponse<RolResponse> obtenerPorId(@PathVariable Long id) {
        return ApiResponse.exitosa(rolService.obtenerPorId(id));
    }
}

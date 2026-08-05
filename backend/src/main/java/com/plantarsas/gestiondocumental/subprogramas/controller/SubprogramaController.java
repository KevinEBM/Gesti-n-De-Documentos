package com.plantarsas.gestiondocumental.subprogramas.controller;

import com.plantarsas.gestiondocumental.shared.dto.ApiResponse;
import com.plantarsas.gestiondocumental.subprogramas.dto.SubprogramaEstadoRequest;
import com.plantarsas.gestiondocumental.subprogramas.dto.SubprogramaRequest;
import com.plantarsas.gestiondocumental.subprogramas.dto.SubprogramaResponse;
import com.plantarsas.gestiondocumental.subprogramas.dto.SubprogramaUpdateRequest;
import com.plantarsas.gestiondocumental.subprogramas.service.SubprogramaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subprogramas")
@RequiredArgsConstructor
public class SubprogramaController {

    private final SubprogramaService subprogramaService;

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<SubprogramaResponse>> crear(@Valid @RequestBody SubprogramaRequest request) {
        SubprogramaResponse creado = subprogramaService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.exitosa(creado));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ApiResponse<List<SubprogramaResponse>> listar() {
        return ApiResponse.exitosa(subprogramaService.listar());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ApiResponse<SubprogramaResponse> obtenerPorId(@PathVariable Long id) {
        return ApiResponse.exitosa(subprogramaService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ApiResponse<SubprogramaResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody SubprogramaUpdateRequest request
    ) {
        return ApiResponse.exitosa(subprogramaService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ApiResponse<SubprogramaResponse> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody SubprogramaEstadoRequest request
    ) {
        return ApiResponse.exitosa(subprogramaService.cambiarEstado(id, request));
    }

    @GetMapping("/area/{areaId}/activos")
    public ApiResponse<List<SubprogramaResponse>> listarActivosPorArea(@PathVariable Long areaId) {
        return ApiResponse.exitosa(subprogramaService.listarActivosPorArea(areaId));
    }
}

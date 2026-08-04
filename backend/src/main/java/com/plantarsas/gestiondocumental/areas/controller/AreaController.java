package com.plantarsas.gestiondocumental.areas.controller;

import com.plantarsas.gestiondocumental.areas.dto.AreaEstadoRequest;
import com.plantarsas.gestiondocumental.areas.dto.AreaRequest;
import com.plantarsas.gestiondocumental.areas.dto.AreaResponse;
import com.plantarsas.gestiondocumental.areas.service.AreaService;
import com.plantarsas.gestiondocumental.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/areas")
@RequiredArgsConstructor
public class AreaController {

    private final AreaService areaService;

    @PostMapping
    public ResponseEntity<ApiResponse<AreaResponse>> crear(@Valid @RequestBody AreaRequest request) {
        AreaResponse creada = areaService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.exitosa(creada));
    }

    @GetMapping
    public ApiResponse<List<AreaResponse>> listar() {
        return ApiResponse.exitosa(areaService.listar());
    }

    @GetMapping("/{id}")
    public ApiResponse<AreaResponse> obtenerPorId(@PathVariable Long id) {
        return ApiResponse.exitosa(areaService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<AreaResponse> actualizar(@PathVariable Long id, @Valid @RequestBody AreaRequest request) {
        return ApiResponse.exitosa(areaService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    public ApiResponse<AreaResponse> cambiarEstado(@PathVariable Long id, @Valid @RequestBody AreaEstadoRequest request) {
        return ApiResponse.exitosa(areaService.cambiarEstado(id, request));
    }
}

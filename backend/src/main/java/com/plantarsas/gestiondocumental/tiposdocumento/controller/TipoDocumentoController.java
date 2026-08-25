package com.plantarsas.gestiondocumental.tiposdocumento.controller;

import com.plantarsas.gestiondocumental.shared.dto.ApiResponse;
import com.plantarsas.gestiondocumental.tiposdocumento.dto.TipoDocumentoEstadoRequest;
import com.plantarsas.gestiondocumental.tiposdocumento.dto.TipoDocumentoRequest;
import com.plantarsas.gestiondocumental.tiposdocumento.dto.TipoDocumentoResponse;
import com.plantarsas.gestiondocumental.tiposdocumento.dto.TipoDocumentoUpdateRequest;
import com.plantarsas.gestiondocumental.tiposdocumento.service.TipoDocumentoService;
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
@RequestMapping("/api/tipos-documento")
@RequiredArgsConstructor
public class TipoDocumentoController {

    private final TipoDocumentoService tipoDocumentoService;

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<TipoDocumentoResponse>> crear(@Valid @RequestBody TipoDocumentoRequest request) {
        TipoDocumentoResponse creado = tipoDocumentoService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.exitosa(creado));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ApiResponse<List<TipoDocumentoResponse>> listar() {
        return ApiResponse.exitosa(tipoDocumentoService.listar());
    }

    @GetMapping("/consulta")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'JEFE_AREA', 'ADMINISTRATIVO')")
    public ApiResponse<List<TipoDocumentoResponse>> listarParaConsulta() {
        return ApiResponse.exitosa(tipoDocumentoService.listar());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ApiResponse<TipoDocumentoResponse> obtenerPorId(@PathVariable Long id) {
        return ApiResponse.exitosa(tipoDocumentoService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ApiResponse<TipoDocumentoResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody TipoDocumentoUpdateRequest request
    ) {
        return ApiResponse.exitosa(tipoDocumentoService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ApiResponse<TipoDocumentoResponse> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody TipoDocumentoEstadoRequest request
    ) {
        return ApiResponse.exitosa(tipoDocumentoService.cambiarEstado(id, request));
    }

    @GetMapping("/activos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'JEFE_AREA', 'ADMINISTRATIVO')")
    public ApiResponse<List<TipoDocumentoResponse>> listarActivos() {
        return ApiResponse.exitosa(tipoDocumentoService.listarActivos());
    }
}

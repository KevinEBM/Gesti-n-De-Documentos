package com.plantarsas.gestiondocumental.usuarios.controller;

import com.plantarsas.gestiondocumental.shared.dto.ApiResponse;
import com.plantarsas.gestiondocumental.usuarios.dto.UsuarioEstadoRequest;
import com.plantarsas.gestiondocumental.usuarios.dto.UsuarioRequest;
import com.plantarsas.gestiondocumental.usuarios.dto.UsuarioResponse;
import com.plantarsas.gestiondocumental.usuarios.dto.UsuarioUpdateRequest;
import com.plantarsas.gestiondocumental.usuarios.service.UsuarioService;
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
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<ApiResponse<UsuarioResponse>> crear(@Valid @RequestBody UsuarioRequest request) {
        UsuarioResponse creado = usuarioService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.exitosa(creado));
    }

    @GetMapping
    public ApiResponse<List<UsuarioResponse>> listar() {
        return ApiResponse.exitosa(usuarioService.listar());
    }

    @GetMapping("/{id}")
    public ApiResponse<UsuarioResponse> obtenerPorId(@PathVariable Long id) {
        return ApiResponse.exitosa(usuarioService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<UsuarioResponse> actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioUpdateRequest request) {
        return ApiResponse.exitosa(usuarioService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    public ApiResponse<UsuarioResponse> cambiarEstado(@PathVariable Long id, @Valid @RequestBody UsuarioEstadoRequest request) {
        return ApiResponse.exitosa(usuarioService.cambiarEstado(id, request));
    }
}

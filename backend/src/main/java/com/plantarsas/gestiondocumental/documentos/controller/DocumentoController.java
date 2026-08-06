package com.plantarsas.gestiondocumental.documentos.controller;

import com.plantarsas.gestiondocumental.documentos.dto.DocumentoPublicacionInicialRequest;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoResponse;
import com.plantarsas.gestiondocumental.documentos.dto.NuevaVersionDocumentoRequest;
import com.plantarsas.gestiondocumental.documentos.service.DocumentoService;
import com.plantarsas.gestiondocumental.exception.BusinessException;
import com.plantarsas.gestiondocumental.security.AuthenticatedUser;
import com.plantarsas.gestiondocumental.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoService documentoService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<DocumentoResponse>> publicarInicial(
            @Valid @RequestPart("metadata") DocumentoPublicacionInicialRequest metadata,
            @RequestPart("archivo") MultipartFile archivo,
            @AuthenticationPrincipal AuthenticatedUser usuarioAutenticado
    ) {
        if (archivo.isEmpty()) {
            throw new BusinessException("El archivo no puede estar vacío", HttpStatus.BAD_REQUEST);
        }

        DocumentoResponse creado;
        try (InputStream contenidoArchivo = archivo.getInputStream()) {
            creado = documentoService.publicarInicial(
                    metadata,
                    usuarioAutenticado,
                    archivo.getOriginalFilename(),
                    contenidoArchivo,
                    archivo.getContentType(),
                    archivo.getSize()
            );
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo del documento", e);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.exitosa(creado));
    }

    @PostMapping(path = "/{id}/versiones", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<DocumentoResponse>> publicarNuevaVersion(
            @PathVariable Long id,
            @Valid @RequestPart("metadata") NuevaVersionDocumentoRequest metadata,
            @RequestPart("archivo") MultipartFile archivo,
            @AuthenticationPrincipal AuthenticatedUser usuarioAutenticado
    ) {
        if (archivo.isEmpty()) {
            throw new BusinessException("El archivo no puede estar vacío", HttpStatus.BAD_REQUEST);
        }

        DocumentoResponse actualizado;
        try (InputStream contenidoArchivo = archivo.getInputStream()) {
            actualizado = documentoService.publicarNuevaVersion(
                    id,
                    metadata,
                    usuarioAutenticado,
                    archivo.getOriginalFilename(),
                    contenidoArchivo,
                    archivo.getContentType(),
                    archivo.getSize()
            );
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo de la nueva versión", e);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.exitosa(actualizado));
    }
}

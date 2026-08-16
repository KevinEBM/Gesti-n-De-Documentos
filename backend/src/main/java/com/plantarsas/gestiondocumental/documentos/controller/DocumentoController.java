package com.plantarsas.gestiondocumental.documentos.controller;

import com.plantarsas.gestiondocumental.documentos.dto.DocumentoPublicacionInicialRequest;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoResponse;
import com.plantarsas.gestiondocumental.documentos.dto.DocumentoResumenResponse;
import com.plantarsas.gestiondocumental.documentos.dto.NuevaVersionDocumentoRequest;
import com.plantarsas.gestiondocumental.documentos.service.DocumentoConsultaService;
import com.plantarsas.gestiondocumental.documentos.service.DocumentoService;
import com.plantarsas.gestiondocumental.exception.BusinessException;
import com.plantarsas.gestiondocumental.security.AuthenticatedUser;
import com.plantarsas.gestiondocumental.shared.dto.ApiResponse;
import com.plantarsas.gestiondocumental.shared.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    private static final int TAMANO_MAXIMO_PAGINA = 100;

    private static final Sort ORDEN_LISTADO =
            Sort.by(Sort.Direction.DESC, "fechaActualizacion")
                    .and(Sort.by(Sort.Direction.DESC, "id"));

    private final DocumentoService documentoService;
    private final DocumentoConsultaService documentoConsultaService;

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

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'JEFE_AREA', 'ADMINISTRATIVO')")
    public ApiResponse<PageResponse<DocumentoResumenResponse>> listar(
            @AuthenticationPrincipal AuthenticatedUser usuarioAutenticado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validarPaginacion(page, size);

        Pageable pageable = PageRequest.of(page, size, ORDEN_LISTADO);
        Page<DocumentoResumenResponse> resultado = documentoConsultaService.listar(usuarioAutenticado, pageable);

        return ApiResponse.exitosa(PageResponse.desde(resultado));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'JEFE_AREA', 'ADMINISTRATIVO')")
    public ApiResponse<DocumentoResponse> obtenerPorId(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser usuarioAutenticado
    ) {
        return ApiResponse.exitosa(documentoConsultaService.obtenerPorId(id, usuarioAutenticado));
    }

    private void validarPaginacion(int page, int size) {
        if (page < 0) {
            throw new BusinessException("El número de página no puede ser negativo");
        }
        if (size < 1 || size > TAMANO_MAXIMO_PAGINA) {
            throw new BusinessException(
                    "El tamaño de página debe estar entre 1 y " + TAMANO_MAXIMO_PAGINA
            );
        }
    }
}

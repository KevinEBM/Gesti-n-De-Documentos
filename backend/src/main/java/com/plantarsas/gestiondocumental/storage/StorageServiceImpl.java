package com.plantarsas.gestiondocumental.storage;

import com.plantarsas.gestiondocumental.exception.BusinessException;
import com.plantarsas.gestiondocumental.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class StorageServiceImpl implements StorageService {

    private static final int LONGITUD_MAXIMA_EXTENSION = 10;
    private static final String MIME_TYPE_POR_DEFECTO = "application/octet-stream";
    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx"
    );
    private static final String MENSAJE_EXTENSION_NO_PERMITIDA =
            "El tipo de archivo no está permitido. Solo se aceptan PDF, DOC, DOCX, XLS y XLSX.";

    private final Path rootLocation;
    private final long maxFileSizeBytes;

    public StorageServiceImpl(
            @Value("${storage.location}") String location,
            @Value("${storage.max-file-size-bytes:10485760}") long maxFileSizeBytes) {

        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("La ubicación de almacenamiento es obligatoria");
        }
        if (maxFileSizeBytes <= 0) {
            throw new IllegalArgumentException("El tamaño máximo debe ser mayor que cero");
        }

        this.rootLocation = Paths.get(location).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @PostConstruct
    void inicializar() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo inicializar el directorio de almacenamiento",
                    e
            );
        }
    }

    @Override
    public StoredFile guardar(
            String nombreOriginal,
            InputStream contenido,
            String mimeType,
            long tamanoBytes) throws IOException {

        if (nombreOriginal == null || nombreOriginal.isBlank()) {
            throw new BusinessException(
                    "El nombre original del archivo es obligatorio",
                    HttpStatus.BAD_REQUEST
            );
        }
        if (contenido == null || tamanoBytes <= 0) {
            throw new BusinessException(
                    "El archivo no puede estar vacío",
                    HttpStatus.BAD_REQUEST
            );
        }
        if (tamanoBytes > maxFileSizeBytes) {
            throw new BusinessException(
                    "El archivo supera el tamaño máximo permitido",
                    HttpStatus.PAYLOAD_TOO_LARGE
            );
        }

        String extension = extensionDe(nombreOriginal);
        if (!extensionPermitida(extension)) {
            throw new BusinessException(MENSAJE_EXTENSION_NO_PERMITIDA, HttpStatus.BAD_REQUEST);
        }

        String nombreAlmacenado = UUID.randomUUID() + extension;
        Path destino = rootLocation.resolve(nombreAlmacenado).normalize();

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "El algoritmo SHA-256 no está disponible",
                    e
            );
        }

        try {
            try (DigestInputStream digestStream =
                         new DigestInputStream(contenido, digest)) {
                Files.copy(digestStream, destino);
            }

            long tamanoReal = Files.size(destino);

            if (tamanoReal <= 0) {
                throw new BusinessException(
                        "El archivo no puede estar vacío",
                        HttpStatus.BAD_REQUEST
                );
            }
            if (tamanoReal > maxFileSizeBytes) {
                throw new BusinessException(
                        "El archivo supera el tamaño máximo permitido",
                        HttpStatus.PAYLOAD_TOO_LARGE
                );
            }
            if (tamanoReal != tamanoBytes) {
                throw new BusinessException(
                        "El tamaño del archivo no coincide con el tamaño declarado",
                        HttpStatus.BAD_REQUEST
                );
            }

            String hash = HexFormat.of().formatHex(digest.digest());
            String mimeTypeFinal = resolverMimeType(mimeType);

            return new StoredFile(
                    nombreOriginal,
                    nombreAlmacenado,
                    mimeTypeFinal,
                    tamanoReal,
                    hash
            );
        } catch (IOException | RuntimeException e) {
            eliminarSilenciosamente(destino);
            throw e;
        }
    }

    @Override
    public InputStream cargar(String ruta) throws IOException {
        Path resuelta = resolverRutaValidada(ruta);

        if (!Files.exists(resuelta) || !Files.isRegularFile(resuelta)) {
            throw new ResourceNotFoundException(
                    "No existe el archivo solicitado"
            );
        }

        return Files.newInputStream(resuelta);
    }

    @Override
    public void eliminar(String ruta) throws IOException {
        Path resuelta = resolverRutaValidada(ruta);
        Files.deleteIfExists(resuelta);
    }

    @Override
    public boolean existe(String ruta) {
        Path resuelta = resolverRutaValidada(ruta);
        return Files.exists(resuelta) && Files.isRegularFile(resuelta);
    }

    private Path resolverRutaValidada(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            throw new BusinessException(
                    "La ruta del archivo es obligatoria",
                    HttpStatus.BAD_REQUEST
            );
        }

        Path resuelta = rootLocation.resolve(ruta).normalize();

        if (!resuelta.startsWith(rootLocation)) {
            throw new BusinessException(
                    "Ruta de archivo inválida",
                    HttpStatus.BAD_REQUEST
            );
        }

        return resuelta;
    }

    private String resolverMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return MIME_TYPE_POR_DEFECTO;
        }
        try {
            MediaType.parseMediaType(mimeType);
        } catch (InvalidMediaTypeException e) {
            return MIME_TYPE_POR_DEFECTO;
        }
        return mimeType;
    }

    private String extensionDe(String nombreOriginal) {
        String normalizado = nombreOriginal.replace('\\', '/');
        String nombreBase =
                normalizado.substring(normalizado.lastIndexOf('/') + 1);

        int indicePunto = nombreBase.lastIndexOf('.');

        if (indicePunto <= 0 || indicePunto == nombreBase.length() - 1) {
            return "";
        }

        String extension = nombreBase.substring(indicePunto);

        if (extension.length() > LONGITUD_MAXIMA_EXTENSION
                || !extension.matches("\\.[A-Za-z0-9]+")) {
            return "";
        }

        return extension;
    }

    private boolean extensionPermitida(String extension) {
        if (extension == null || extension.isBlank()) {
            return false;
        }
        return EXTENSIONES_PERMITIDAS.contains(extension.toLowerCase(Locale.ROOT));
    }

    private void eliminarSilenciosamente(Path destino) {
        try {
            Files.deleteIfExists(destino);
        } catch (IOException ignored) {
            // No se reemplaza la excepción original por un error de limpieza.
        }
    }
}

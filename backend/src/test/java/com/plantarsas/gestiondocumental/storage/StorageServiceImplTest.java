package com.plantarsas.gestiondocumental.storage;

import com.plantarsas.gestiondocumental.exception.BusinessException;
import com.plantarsas.gestiondocumental.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageServiceImplTest {

    private static final long MAX_FILE_SIZE_BYTES = 1024L;

    @TempDir
    Path directorioTemporal;

    private StorageServiceImpl storageServiceImpl;

    @BeforeEach
    void inicializar() {
        storageServiceImpl = new StorageServiceImpl(directorioTemporal.toString(), MAX_FILE_SIZE_BYTES);
        storageServiceImpl.inicializar();
    }

    @Test
    void guardar_debeAlmacenarArchivoRealYRetornarloConDatosCorrectos() throws Exception {
        byte[] contenido = "contenido de prueba".getBytes(StandardCharsets.UTF_8);

        StoredFile resultado = storageServiceImpl.guardar(
                "documento.pdf",
                new ByteArrayInputStream(contenido),
                "application/pdf",
                contenido.length
        );

        assertThat(resultado.nombreOriginal()).isEqualTo("documento.pdf");
        assertThat(resultado.mimeType()).isEqualTo("application/pdf");
        assertThat(resultado.tamanoBytes()).isEqualTo(contenido.length);

        Path archivoFisico = directorioTemporal.resolve(resultado.ruta());
        assertThat(Files.exists(archivoFisico)).isTrue();
        assertThat(Files.readAllBytes(archivoFisico)).isEqualTo(contenido);
    }

    @Test
    void guardar_debeGenerarNombreAlmacenadoDistintoAlNombreOriginal() throws Exception {
        byte[] contenido = "otro contenido".getBytes(StandardCharsets.UTF_8);

        StoredFile resultado = storageServiceImpl.guardar(
                "reporte.docx",
                new ByteArrayInputStream(contenido),
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                contenido.length
        );

        assertThat(resultado.ruta()).isNotEqualTo("reporte.docx");
        assertThat(resultado.ruta()).endsWith(".docx");
    }

    @Test
    void guardar_debeCalcularHashSha256DeSesentaYCuatroCaracteresHexadecimales() throws Exception {
        byte[] contenido = "contenido para hash".getBytes(StandardCharsets.UTF_8);

        StoredFile resultado = storageServiceImpl.guardar(
                "hash.pdf",
                new ByteArrayInputStream(contenido),
                "application/pdf",
                contenido.length
        );

        assertThat(resultado.hash()).isNotBlank();
        assertThat(resultado.hash()).hasSize(64);
        assertThat(resultado.hash()).matches("[0-9a-f]{64}");
        assertThat(resultado.hash()).isEqualTo(sha256Hex(contenido));
    }

    @Test
    void guardar_debeRechazarArchivoVacio() {
        assertThatThrownBy(() -> storageServiceImpl.guardar(
                "vacio.bin",
                new ByteArrayInputStream(new byte[0]),
                "application/octet-stream",
                0))
                .isInstanceOf(BusinessException.class)
                .hasMessage("El archivo no puede estar vacío")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void guardar_debeRechazarArchivoQueSuperaElTamanoMaximoDeclarado() {
        long tamanoDeclarado = MAX_FILE_SIZE_BYTES + 1;

        assertThatThrownBy(() -> storageServiceImpl.guardar(
                "grande.bin",
                new ByteArrayInputStream(new byte[10]),
                "application/octet-stream",
                tamanoDeclarado))
                .isInstanceOf(BusinessException.class)
                .hasMessage("El archivo supera el tamaño máximo permitido")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                        .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
    }

    @Test
    void guardar_debeAceptarArchivoDeTamanoMaximoExacto() throws Exception {
        byte[] contenido = new byte[(int) MAX_FILE_SIZE_BYTES];

        StoredFile resultado = storageServiceImpl.guardar(
                "limite.pdf",
                new ByteArrayInputStream(contenido),
                "application/pdf",
                MAX_FILE_SIZE_BYTES
        );

        assertThat(resultado.tamanoBytes()).isEqualTo(MAX_FILE_SIZE_BYTES);
    }

    @Test
    void cargar_debeRetornarElContenidoDeUnArchivoExistente() throws Exception {
        byte[] contenido = "contenido a leer".getBytes(StandardCharsets.UTF_8);
        StoredFile guardado = storageServiceImpl.guardar(
                "lectura.pdf",
                new ByteArrayInputStream(contenido),
                "application/pdf",
                contenido.length
        );

        try (InputStream leido = storageServiceImpl.cargar(guardado.ruta())) {
            assertThat(leido.readAllBytes()).isEqualTo(contenido);
        }
    }

    @Test
    void guardar_debeRechazarYEliminarArchivoParcialCuandoElTamanoRealNoCoincideConElDeclarado() throws Exception {
        byte[] contenido = "abc".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> storageServiceImpl.guardar(
                "inconsistente.pdf",
                new ByteArrayInputStream(contenido),
                "application/pdf",
                contenido.length + 5))
                .isInstanceOf(BusinessException.class)
                .hasMessage("El tamaño del archivo no coincide con el tamaño declarado")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        try (var archivos = Files.list(directorioTemporal)) {
            assertThat(archivos).isEmpty();
        }
    }

    @Test
    void guardar_debeRetornarRutaRelativaSinExponerElDirectorioFisico() throws Exception {
        byte[] contenido = "contenido".getBytes(StandardCharsets.UTF_8);

        StoredFile resultado = storageServiceImpl.guardar(
                "archivo.pdf",
                new ByteArrayInputStream(contenido),
                "application/pdf",
                contenido.length
        );

        assertThat(Path.of(resultado.ruta()).isAbsolute()).isFalse();
        assertThat(resultado.ruta()).doesNotContain(directorioTemporal.toString());
    }

    @Test
    void guardar_debeUsarMimeTypePorDefectoCuandoLlegaVacio() throws Exception {
        byte[] contenido = "contenido".getBytes(StandardCharsets.UTF_8);

        StoredFile resultado = storageServiceImpl.guardar(
                "archivo.pdf",
                new ByteArrayInputStream(contenido),
                "",
                contenido.length
        );

        assertThat(resultado.mimeType()).isEqualTo("application/octet-stream");
    }

    @Test
    void guardar_debeUsarMimeTypePorDefectoCuandoElDeclaradoEsInvalido() throws Exception {
        byte[] contenido = "contenido".getBytes(StandardCharsets.UTF_8);

        StoredFile resultado = storageServiceImpl.guardar(
                "archivo.pdf",
                new ByteArrayInputStream(contenido),
                "esto no es un mime",
                contenido.length
        );

        assertThat(resultado.mimeType()).isEqualTo("application/octet-stream");

        Path archivoFisico = directorioTemporal.resolve(resultado.ruta());
        assertThat(Files.exists(archivoFisico)).isTrue();
    }

    @Test
    void guardar_debeRechazarNombreOriginalVacio() {
        assertThatThrownBy(() -> storageServiceImpl.guardar(
                " ",
                new ByteArrayInputStream("contenido".getBytes(StandardCharsets.UTF_8)),
                "text/plain",
                9))
                .isInstanceOf(BusinessException.class)
                .hasMessage("El nombre original del archivo es obligatorio")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "informe.pdf",
            "informe.PDF",
            "manual.doc",
            "manual.DOC",
            "reporte.docx",
            "reporte.DOCX",
            "datos.xls",
            "datos.XLS",
            "tabla.xlsx",
            "tabla.XLSX"
    })
    void guardar_debeAceptarExtensionesPermitidasSinImportarMayusculas(String nombreOriginal) throws Exception {
        byte[] contenido = "contenido".getBytes(StandardCharsets.UTF_8);

        StoredFile resultado = storageServiceImpl.guardar(
                nombreOriginal,
                new ByteArrayInputStream(contenido),
                "application/octet-stream",
                contenido.length
        );

        assertThat(resultado.nombreOriginal()).isEqualTo(nombreOriginal);
        assertThat(Files.exists(directorioTemporal.resolve(resultado.ruta()))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"archivo.apk", "archivo.APK", "archivo.txt", "archivo.TXT", "notas.exe", "foto.png"})
    void guardar_debeRechazarExtensionesNoPermitidas(String nombreOriginal) throws Exception {
        assertThatThrownBy(() -> storageServiceImpl.guardar(
                nombreOriginal,
                new ByteArrayInputStream("contenido".getBytes(StandardCharsets.UTF_8)),
                "application/octet-stream",
                9))
                .isInstanceOf(BusinessException.class)
                .hasMessage("El tipo de archivo no está permitido. Solo se aceptan PDF, DOC, DOCX, XLS y XLSX.")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        try (var archivos = Files.list(directorioTemporal)) {
            assertThat(archivos).isEmpty();
        }
    }

    @Test
    void guardar_debeRechazarArchivoSinExtension() throws Exception {
        assertThatThrownBy(() -> storageServiceImpl.guardar(
                "sin_extension",
                new ByteArrayInputStream("contenido".getBytes(StandardCharsets.UTF_8)),
                "application/pdf",
                9))
                .isInstanceOf(BusinessException.class)
                .hasMessage("El tipo de archivo no está permitido. Solo se aceptan PDF, DOC, DOCX, XLS y XLSX.");
    }

    @Test
    void guardar_debeAceptarArchivoConExtensionFinalDistintaAunqueElNombreContengaApk() throws Exception {
        byte[] contenido = "contenido".getBytes(StandardCharsets.UTF_8);

        StoredFile resultado = storageServiceImpl.guardar(
                "archivo.apk.pdf",
                new ByteArrayInputStream(contenido),
                "application/pdf",
                contenido.length
        );

        assertThat(resultado.ruta()).endsWith(".pdf");
    }

    @Test
    void guardar_debeRechazarDobleExtensionCuyaExtensionFinalNoEstaPermitida() {
        assertThatThrownBy(() -> storageServiceImpl.guardar(
                "informe.pdf.apk",
                new ByteArrayInputStream("contenido".getBytes(StandardCharsets.UTF_8)),
                "application/pdf",
                9))
                .isInstanceOf(BusinessException.class)
                .hasMessage("El tipo de archivo no está permitido. Solo se aceptan PDF, DOC, DOCX, XLS y XLSX.");
    }

    @Test
    void cargar_debeLanzarResourceNotFoundCuandoElArchivoNoExiste() {
        assertThatThrownBy(() -> storageServiceImpl.cargar("no-existe.pdf"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No existe el archivo solicitado");
    }

    @Test
    void cargar_debeRechazarIntentoDePathTraversal() {
        assertThatThrownBy(() -> storageServiceImpl.cargar("../../etc/passwd"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Ruta de archivo inválida")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void eliminar_debeEliminarArchivoExistente() throws Exception {
        byte[] contenido = "a borrar".getBytes(StandardCharsets.UTF_8);
        StoredFile guardado = storageServiceImpl.guardar(
                "borrar.pdf",
                new ByteArrayInputStream(contenido),
                "application/pdf",
                contenido.length
        );

        storageServiceImpl.eliminar(guardado.ruta());

        assertThat(storageServiceImpl.existe(guardado.ruta())).isFalse();
    }

    @Test
    void eliminar_debeSerIdempotenteCuandoElArchivoNoExiste() {
        assertThatCode(() -> storageServiceImpl.eliminar("no-existe.pdf"))
                .doesNotThrowAnyException();
    }

    @Test
    void eliminar_debeRechazarIntentoDePathTraversal() {
        assertThatThrownBy(() -> storageServiceImpl.eliminar("../../etc/passwd"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Ruta de archivo inválida")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void existe_debeRetornarTrueParaArchivoRegularExistente() throws Exception {
        byte[] contenido = "existente".getBytes(StandardCharsets.UTF_8);
        StoredFile guardado = storageServiceImpl.guardar(
                "existente.pdf",
                new ByteArrayInputStream(contenido),
                "application/pdf",
                contenido.length
        );

        assertThat(storageServiceImpl.existe(guardado.ruta())).isTrue();
    }

    @Test
    void existe_debeRetornarFalseCuandoElArchivoNoExiste() {
        assertThat(storageServiceImpl.existe("no-existe.pdf")).isFalse();
    }

    @Test
    void existe_debeRechazarIntentoDePathTraversal() {
        assertThatThrownBy(() -> storageServiceImpl.existe("../../etc/passwd"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Ruta de archivo inválida")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private static String sha256Hex(byte[] contenido) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(contenido));
    }
}

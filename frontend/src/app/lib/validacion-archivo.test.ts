import { describe, expect, it } from "vitest";

import {
    extensionPermitida,
    LIMITE_ARCHIVO_BYTES,
    MENSAJE_TIPO_NO_PERMITIDO,
    validarArchivoSubida,
} from "./validacion-archivo";

function archivoMock(nombre: string, size = 100): File {
    return { name: nombre, size } as File;
}

describe("validacion-archivo", () => {
    it("permite PDF, DOC, DOCX, XLS, XLSX, JPG, JPEG y PNG", () => {
        expect(extensionPermitida("informe.pdf")).toBe(true);
        expect(extensionPermitida("manual.doc")).toBe(true);
        expect(extensionPermitida("reporte.docx")).toBe(true);
        expect(extensionPermitida("datos.xls")).toBe(true);
        expect(extensionPermitida("tabla.xlsx")).toBe(true);
        expect(extensionPermitida("foto.jpg")).toBe(true);
        expect(extensionPermitida("foto.jpeg")).toBe(true);
        expect(extensionPermitida("imagen.png")).toBe(true);
    });

    it("permite extensiones de imagen en mayúsculas", () => {
        expect(extensionPermitida("imagen.JPG")).toBe(true);
        expect(extensionPermitida("foto.JPEG")).toBe(true);
        expect(extensionPermitida("imagen.PNG")).toBe(true);
        expect(validarArchivoSubida(archivoMock("imagen.JPG"))).toBeNull();
        expect(validarArchivoSubida(archivoMock("foto.jpeg"))).toBeNull();
        expect(validarArchivoSubida(archivoMock("imagen.PNG"))).toBeNull();
    });

    it("rechaza txt, apk, exe, zip y extensiones desconocidas", () => {
        expect(extensionPermitida("notas.txt")).toBe(false);
        expect(extensionPermitida("app.apk")).toBe(false);
        expect(extensionPermitida("archivo.exe")).toBe(false);
        expect(extensionPermitida("archivo.zip")).toBe(false);
        expect(validarArchivoSubida(archivoMock("notas.txt"))).toBe(MENSAJE_TIPO_NO_PERMITIDO);
        expect(validarArchivoSubida(archivoMock("app.apk"))).toBe(MENSAJE_TIPO_NO_PERMITIDO);
        expect(validarArchivoSubida(archivoMock("archivo.exe"))).toBe(MENSAJE_TIPO_NO_PERMITIDO);
        expect(validarArchivoSubida(archivoMock("archivo.zip"))).toBe(MENSAJE_TIPO_NO_PERMITIDO);
    });

    it("rechaza archivo sin extensión", () => {
        expect(extensionPermitida("sin_extension")).toBe(false);
        expect(validarArchivoSubida(archivoMock("sin_extension"))).toBe(MENSAJE_TIPO_NO_PERMITIDO);
    });

    it("usa la extensión final en nombres con doble extensión", () => {
        expect(extensionPermitida("archivo.apk.pdf")).toBe(true);
        expect(extensionPermitida("informe.pdf.apk")).toBe(false);
        expect(extensionPermitida("archivo.exe.jpg")).toBe(true);
        expect(extensionPermitida("archivo.jpg.exe")).toBe(false);
        expect(validarArchivoSubida(archivoMock("archivo.exe.jpg"))).toBeNull();
        expect(validarArchivoSubida(archivoMock("archivo.jpg.exe"))).toBe(MENSAJE_TIPO_NO_PERMITIDO);
    });

    it("rechaza archivos que superan 10 MB", () => {
        const error = validarArchivoSubida(
            archivoMock("doc.pdf", LIMITE_ARCHIVO_BYTES + 1),
        );
        expect(error).toBe("El archivo no puede superar los 10 MB.");
    });

    it("acepta archivo permitido dentro del limite", () => {
        expect(validarArchivoSubida(archivoMock("doc.pdf"))).toBeNull();
        expect(validarArchivoSubida(archivoMock("foto.jpg"))).toBeNull();
    });
});

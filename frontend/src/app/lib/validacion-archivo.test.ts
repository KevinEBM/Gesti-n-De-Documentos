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
    it("permite PDF, DOC, DOCX, XLS y XLSX", () => {
        expect(extensionPermitida("informe.pdf")).toBe(true);
        expect(extensionPermitida("manual.doc")).toBe(true);
        expect(extensionPermitida("reporte.docx")).toBe(true);
        expect(extensionPermitida("datos.xls")).toBe(true);
        expect(extensionPermitida("tabla.xlsx")).toBe(true);
    });

    it("permite extensiones en mayúsculas", () => {
        expect(extensionPermitida("informe.PDF")).toBe(true);
        expect(extensionPermitida("reporte.DOCX")).toBe(true);
        expect(extensionPermitida("tabla.XLSX")).toBe(true);
    });

    it("rechaza txt, apk y extensiones desconocidas", () => {
        expect(extensionPermitida("notas.txt")).toBe(false);
        expect(extensionPermitida("app.apk")).toBe(false);
        expect(extensionPermitida("foto.png")).toBe(false);
        expect(validarArchivoSubida(archivoMock("notas.txt"))).toBe(MENSAJE_TIPO_NO_PERMITIDO);
        expect(validarArchivoSubida(archivoMock("app.apk"))).toBe(MENSAJE_TIPO_NO_PERMITIDO);
    });

    it("rechaza archivo sin extensión", () => {
        expect(extensionPermitida("sin_extension")).toBe(false);
        expect(validarArchivoSubida(archivoMock("sin_extension"))).toBe(MENSAJE_TIPO_NO_PERMITIDO);
    });

    it("usa la extensión final en nombres con doble extensión", () => {
        expect(extensionPermitida("archivo.apk.pdf")).toBe(true);
        expect(extensionPermitida("informe.pdf.apk")).toBe(false);
        expect(validarArchivoSubida(archivoMock("archivo.apk.pdf"))).toBeNull();
        expect(validarArchivoSubida(archivoMock("informe.pdf.apk"))).toBe(MENSAJE_TIPO_NO_PERMITIDO);
    });

    it("rechaza archivos que superan 10 MB", () => {
        const error = validarArchivoSubida(
            archivoMock("doc.pdf", LIMITE_ARCHIVO_BYTES + 1),
        );
        expect(error).toBe("El archivo no puede superar los 10 MB.");
    });

    it("acepta archivo permitido dentro del limite", () => {
        expect(validarArchivoSubida(archivoMock("doc.pdf"))).toBeNull();
    });
});

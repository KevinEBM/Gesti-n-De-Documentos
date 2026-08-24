import { describe, expect, it } from "vitest";

import {
    extensionProhibida,
    LIMITE_ARCHIVO_BYTES,
    validarArchivoSubida,
} from "./validacion-archivo";

function archivoMock(nombre: string, size = 100): File {
    return { name: nombre, size } as File;
}

describe("validacion-archivo", () => {
    it("rechaza extensiones apk sin importar mayusculas", () => {
        expect(extensionProhibida("app.apk")).toBe(".apk");
        expect(extensionProhibida("app.APK")).toBe(".apk");
    });

    it("rechaza extensiones txt sin importar mayusculas", () => {
        expect(extensionProhibida("notas.txt")).toBe(".txt");
        expect(extensionProhibida("notas.TXT")).toBe(".txt");
        expect(extensionProhibida("notas.TxT")).toBe(".txt");
    });

    it("permite extensiones distintas a apk y txt", () => {
        expect(extensionProhibida("doc.pdf")).toBeNull();
        expect(extensionProhibida("archivo.apk.pdf")).toBeNull();
    });

    it("rechaza archivos que superan 10 MB", () => {
        const error = validarArchivoSubida(
            archivoMock("doc.pdf", LIMITE_ARCHIVO_BYTES + 1),
        );
        expect(error).toBe("El archivo no puede superar los 10 MB.");
    });

    it("rechaza txt con mensaje especifico", () => {
        expect(validarArchivoSubida(archivoMock("notas.txt"))).toBe(
            "No se permiten archivos TXT.",
        );
    });

    it("acepta archivo permitido dentro del limite", () => {
        expect(validarArchivoSubida(archivoMock("doc.pdf"))).toBeNull();
    });
});

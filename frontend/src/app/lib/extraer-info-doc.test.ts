import { describe, expect, it } from "vitest";

import { extraerMetadatosDesdeArchivo } from "./extraer-info-doc";

describe("extraerMetadatosDesdeArchivo", () => {
    it("extrae nomenclatura completa con versión entera", () => {
        const resultado = extraerMetadatosDesdeArchivo(
            "PR-L&D-IN-03 Limpieza y desinfección V7.pdf",
        );

        expect(resultado.reconocido).toBe(true);
        expect(resultado.codigo).toBe("PR-L&D-IN-03");
        expect(resultado.clasificacion).toBe("PR");
        expect(resultado.codigoSubproceso).toBe("L&D");
        expect(resultado.abreviaturaTipo).toBe("IN");
        expect(resultado.nombreTipo).toBe("Instructivo");
        expect(resultado.nombre).toBe("Limpieza y desinfección");
        expect(resultado.version).toEqual({ tipo: "entera", valor: 7 });
    });

    it("extrae el código de subproceso en mayúsculas sin resolverlo", () => {
        const resultado = extraerMetadatosDesdeArchivo(
            "PR-l&d-FO-04 Registro limpieza V1.pdf",
        );

        expect(resultado.reconocido).toBe(true);
        expect(resultado.codigoSubproceso).toBe("L&D");
        expect(resultado.nombreTipo).toBe("Formato");
        expect(resultado.version).toEqual({ tipo: "entera", valor: 1 });
    });

    it("conserva el alias histórico LD sin traducirlo", () => {
        const resultado = extraerMetadatosDesdeArchivo(
            "PR-LD-FO-04 Registro limpieza V1.pdf",
        );

        expect(resultado.reconocido).toBe(true);
        expect(resultado.codigoSubproceso).toBe("LD");
    });

    it("no marca error cuando no hay nomenclatura institucional", () => {
        const resultado = extraerMetadatosDesdeArchivo("Documento prueba.pdf");

        expect(resultado.reconocido).toBe(false);
        expect(resultado.codigo).toBeNull();
        expect(resultado.codigoSubproceso).toBeNull();
        expect(resultado.nombreTipo).toBeNull();
        expect(resultado.version).toEqual({ tipo: "ninguna" });
    });

    it("extrae códigos de subproceso desconocidos sin inventar nada", () => {
        const resultado = extraerMetadatosDesdeArchivo("PR-XYZ-IN-01 Documento.pdf");

        expect(resultado.reconocido).toBe(true);
        expect(resultado.codigoSubproceso).toBe("XYZ");
        expect(resultado.nombreTipo).toBe("Instructivo");
    });

    it("detecta versión decimal sin convertirla a entera", () => {
        const resultado = extraerMetadatosDesdeArchivo(
            "PR-GDO-PD-02 Control documental V2.1.pdf",
        );

        expect(resultado.reconocido).toBe(true);
        expect(resultado.version).toEqual({ tipo: "decimal", valor: "2.1" });
    });
});

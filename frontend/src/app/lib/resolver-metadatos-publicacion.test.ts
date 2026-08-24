import { describe, expect, it } from "vitest";

import { aplicarMetadatosDesdeArchivo, resolverMetadatosCatalogoDesdeArchivo } from "./resolver-metadatos-publicacion";
import type { SubprogramaCatalogo } from "./subprogramas-api";
import type { TipoDocumentoCatalogo } from "./tipos-documento-api";

const NOMBRE_VALIDO = "PR-LD-PG-01 Programa limpieza V1.pdf";

const subprogramasMock: SubprogramaCatalogo[] = [
    {
        id: "10",
        nombre: "Limpieza y Desinfección",
        descripcion: "",
        areaId: "1",
        areaCodigo: "LD",
        areaNombre: "Limpieza y Desinfección",
        areaActiva: true,
        activo: true,
    },
];

const tiposMock: TipoDocumentoCatalogo[] = [
    {
        id: "20",
        nombre: "Programa",
        descripcion: "",
        activo: true,
    },
];

describe("resolver metadatos publicación — condición de carrera M4", () => {
    it("con catálogos vacíos genera avisos falsos de no encontrado (condición original)", () => {
        const resultado = aplicarMetadatosDesdeArchivo(NOMBRE_VALIDO, [], []);

        expect(resultado.detectado).toBe(true);
        expect(resultado.actualizaciones.codigo).toBe("PR-LD-PG-01");
        expect(resultado.actualizaciones.titulo).toBe("Programa limpieza");
        expect(resultado.actualizaciones.areaId).toBeUndefined();
        expect(resultado.actualizaciones.subprogramaId).toBeUndefined();
        expect(resultado.actualizaciones.tipoDocumentoId).toBeUndefined();
        expect(resultado.avisos.some((aviso) => aviso.includes("no se encontró en los catálogos activos"))).toBe(
            true,
        );
    });

    it("omitirResolucionCatalogo aplica metadatos inmediatos sin avisos de catálogo", () => {
        const resultado = aplicarMetadatosDesdeArchivo(NOMBRE_VALIDO, [], [], {
            omitirResolucionCatalogo: true,
        });

        expect(resultado.detectado).toBe(true);
        expect(resultado.actualizaciones.codigo).toBe("PR-LD-PG-01");
        expect(resultado.actualizaciones.titulo).toBe("Programa limpieza");
        expect(resultado.actualizaciones.numeroVersionInicial).toBe("1");
        expect(resultado.actualizaciones.areaId).toBeUndefined();
        expect(resultado.avisos).toEqual([]);
    });

    it("resolverMetadatosCatalogoDesdeArchivo completa área, subproceso y tipo cuando hay catálogos", () => {
        const resultado = resolverMetadatosCatalogoDesdeArchivo(
            NOMBRE_VALIDO,
            subprogramasMock,
            tiposMock,
        );

        expect(resultado.actualizaciones.areaId).toBe("1");
        expect(resultado.actualizaciones.subprogramaId).toBe("10");
        expect(resultado.actualizaciones.tipoDocumentoId).toBe("20");
        expect(resultado.actualizaciones.codigo).toBeUndefined();
        expect(resultado.avisos).toEqual([]);
    });
});

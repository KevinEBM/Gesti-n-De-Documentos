import { describe, expect, it } from "vitest";

import { aplicarMetadatosDesdeArchivo, resolverMetadatosCatalogoDesdeArchivo } from "./resolver-metadatos-publicacion";
import type { SubprogramaCatalogo } from "./subprogramas-api";
import type { TipoDocumentoCatalogo } from "./tipos-documento-api";

const NOMBRE_VALIDO = "PR-LD-PG-01 Programa limpieza V1.pdf";

function subprograma(
    valores: Partial<SubprogramaCatalogo> & Pick<SubprogramaCatalogo, "id" | "codigo" | "nombre">,
): SubprogramaCatalogo {
    return {
        descripcion: "",
        areaId: "1",
        areaCodigo: "GAMB",
        areaNombre: "Gestión Ambiental",
        areaActiva: true,
        activo: true,
        ...valores,
    };
}

const subprogramasMock: SubprogramaCatalogo[] = [
    subprograma({ id: "10", codigo: "L&D", nombre: "Limpieza y Desinfección" }),
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

describe("resolución del subproceso por código del catálogo", () => {
    it("resuelve el código canónico y hereda el área del subprograma", () => {
        const resultado = resolverMetadatosCatalogoDesdeArchivo(
            "PR-L&D-PG-01 Programa limpieza V1.pdf",
            subprogramasMock,
            tiposMock,
        );

        expect(resultado.actualizaciones.subprogramaId).toBe("10");
        expect(resultado.actualizaciones.areaId).toBe("1");
        expect(resultado.avisos).toEqual([]);
    });

    it("resuelve el código sin distinguir mayúsculas ni espacios", () => {
        const catalogo = [
            subprograma({ id: "10", codigo: "  l&d  ", nombre: "Limpieza y Desinfección" }),
        ];

        const resultado = resolverMetadatosCatalogoDesdeArchivo(
            "PR-l&d-PG-01 Programa limpieza V1.pdf",
            catalogo,
            tiposMock,
        );

        expect(resultado.actualizaciones.subprogramaId).toBe("10");
    });

    it("traduce el alias histórico LD al código canónico L&D", () => {
        const resultado = resolverMetadatosCatalogoDesdeArchivo(
            "PR-LD-PG-01 Programa limpieza V1.pdf",
            subprogramasMock,
            tiposMock,
        );

        expect(resultado.actualizaciones.subprogramaId).toBe("10");
        expect(resultado.actualizaciones.areaId).toBe("1");
    });

    it("traduce el alias histórico CD al código canónico C&D", () => {
        const catalogo = [
            subprograma({
                id: "30",
                codigo: "C&D",
                nombre: "Capacitación y Desarrollo",
                areaId: "2",
                areaCodigo: "GHUM",
                areaNombre: "Gestión Humana",
            }),
        ];

        const resultado = resolverMetadatosCatalogoDesdeArchivo(
            "PR-CD-PG-01 Plan de formación V1.pdf",
            catalogo,
            tiposMock,
        );

        expect(resultado.actualizaciones.subprogramaId).toBe("30");
        expect(resultado.actualizaciones.areaId).toBe("2");
    });

    it("prefiere el código canónico del catálogo antes que el alias", () => {
        const catalogo = [
            subprograma({ id: "40", codigo: "LD", nombre: "Logística Directa" }),
            subprograma({ id: "10", codigo: "L&D", nombre: "Limpieza y Desinfección" }),
        ];

        const resultado = resolverMetadatosCatalogoDesdeArchivo(
            "PR-LD-PG-01 Documento V1.pdf",
            catalogo,
            tiposMock,
        );

        expect(resultado.actualizaciones.subprogramaId).toBe("40");
    });

    it("no inventa coincidencias parciales para códigos inexistentes", () => {
        const resultado = resolverMetadatosCatalogoDesdeArchivo(
            "PR-L-PG-01 Documento V1.pdf",
            subprogramasMock,
            tiposMock,
        );

        expect(resultado.actualizaciones.subprogramaId).toBeUndefined();
        expect(resultado.actualizaciones.areaId).toBeUndefined();
        expect(
            resultado.avisos.some((aviso) => aviso.includes("no se encontró en los catálogos activos")),
        ).toBe(true);
    });

    it("no resuelve subprogramas inactivos", () => {
        const catalogo = [
            subprograma({
                id: "10",
                codigo: "L&D",
                nombre: "Limpieza y Desinfección",
                activo: false,
            }),
        ];

        const resultado = resolverMetadatosCatalogoDesdeArchivo(
            "PR-L&D-PG-01 Documento V1.pdf",
            catalogo,
            tiposMock,
        );

        expect(resultado.actualizaciones.subprogramaId).toBeUndefined();
        expect(
            resultado.avisos.some((aviso) => aviso.includes("no se encontró en los catálogos activos")),
        ).toBe(true);
    });

    it("resuelve un subproceso nuevo del catálogo sin tocar mapas del parser", () => {
        const catalogo = [
            subprograma({
                id: "99",
                codigo: "NVO",
                nombre: "Subproceso Recién Parametrizado",
                areaId: "7",
                areaCodigo: "GTICS",
                areaNombre: "Gestión TICs",
            }),
        ];

        const resultado = resolverMetadatosCatalogoDesdeArchivo(
            "PR-NVO-PG-01 Documento nuevo V1.pdf",
            catalogo,
            tiposMock,
        );

        expect(resultado.actualizaciones.subprogramaId).toBe("99");
        expect(resultado.actualizaciones.areaId).toBe("7");
        expect(resultado.actualizaciones.tipoDocumentoId).toBe("20");
    });
});

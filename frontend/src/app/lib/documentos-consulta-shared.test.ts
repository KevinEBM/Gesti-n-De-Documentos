import { describe, expect, it } from "vitest";

import {
    areaConsultaNoAdminBloqueada,
    construirFiltrosApi,
    construirFiltrosBaseConsulta,
    etiquetaCatalogoConsulta,
    filtrarSubprogramasConsulta,
    formatFechaCalendarioBogota,
    formatFechaDocumento,
    resolverAreaAsignadaNoAdmin,
    resolverAreaIdEfectivaConsulta,
    resolverAreaObligatoriaNoAdmin,
    resolverAreaPrincipalDesdeCatalogo,
    subprocesoConsultaDeshabilitado,
    textoRetencionObsoleto,
    TODOS,
} from "./documentos-consulta-shared";

const areasApi = [
    { id: "1", nombre: "Producción", activo: true },
    { id: "2", nombre: "Calidad", activo: false },
];

const subprogramas = [
    { id: "10", areaId: "1", nombre: "GDO", activo: true },
    { id: "11", areaId: "2", nombre: "Auditoría", activo: false },
    { id: "12", areaId: "1", nombre: "Mantenimiento", activo: true },
];

describe("resolverAreaAsignadaNoAdmin", () => {
    it("A: sesión Calidad + API Producción -> Producción", () => {
        expect(
            resolverAreaAsignadaNoAdmin({
                areasUsuario: [{ id: "1", nombre: "Producción", activo: true }],
                areasApiCargadas: true,
                sesionAreaId: "2",
                sesionAreaNombre: "Calidad",
            }),
        ).toEqual({ areaId: "1", nombre: "Producción" });
    });

    it("B: sesión Calidad + API exitosa [] -> NO Calidad", () => {
        expect(
            resolverAreaAsignadaNoAdmin({
                areasUsuario: [],
                areasApiCargadas: true,
                sesionAreaId: "2",
                sesionAreaNombre: "Calidad",
            }),
        ).toBeNull();
    });

    it("C: API con área inactiva asignada -> esa área sigue siendo efectiva", () => {
        expect(
            resolverAreaAsignadaNoAdmin({
                areasUsuario: [{ id: "2", nombre: "Calidad", activo: false }],
                areasApiCargadas: true,
            }),
        ).toEqual({ areaId: "2", nombre: "Calidad" });
    });

    it("D: respuesta ambigua con varias áreas -> no elige la primera", () => {
        expect(
            resolverAreaAsignadaNoAdmin({
                areasUsuario: areasApi,
                areasApiCargadas: true,
                sesionAreaId: "2",
                sesionAreaNombre: "Calidad",
            }),
        ).toBeNull();
    });

    it("antes de cargar API usa sesión como fallback temporal", () => {
        expect(
            resolverAreaAsignadaNoAdmin({
                areasUsuario: [],
                areasApiCargadas: false,
                sesionAreaId: "2",
                sesionAreaNombre: "Calidad",
            }),
        ).toEqual({ areaId: "2", nombre: "Calidad" });
    });
});

describe("resolverAreaIdEfectivaConsulta", () => {
    it("ADMIN con Área TODOS devuelve TODOS", () => {
        expect(
            resolverAreaIdEfectivaConsulta({
                esAdmin: true,
                filtroArea: TODOS,
                areasUsuario: areasApi,
                areasApiCargadas: true,
            }),
        ).toBe(TODOS);
    });

    it("JEFE con API cargada usa asignación y no sesión stale", () => {
        expect(
            resolverAreaIdEfectivaConsulta({
                esAdmin: false,
                filtroArea: TODOS,
                areasUsuario: [{ id: "1", nombre: "Producción", activo: true }],
                areasApiCargadas: true,
                sesionAreaId: "2",
                sesionAreaNombre: "Calidad",
            }),
        ).toBe("1");
    });

    it("JEFE con API [] devuelve TODOS aunque sesión tenga área", () => {
        expect(
            resolverAreaIdEfectivaConsulta({
                esAdmin: false,
                filtroArea: TODOS,
                areasUsuario: [],
                areasApiCargadas: true,
                sesionAreaId: "2",
                sesionAreaNombre: "Calidad",
            }),
        ).toBe(TODOS);
    });

    it("JEFE antes de API usa sesión temporalmente", () => {
        expect(
            resolverAreaIdEfectivaConsulta({
                esAdmin: false,
                filtroArea: TODOS,
                areasUsuario: [],
                areasApiCargadas: false,
                sesionAreaId: "2",
                sesionAreaNombre: "Calidad",
            }),
        ).toBe("2");
    });
});

describe("areaConsultaNoAdminBloqueada", () => {
    it("JEFE siempre bloquea el selector de Área", () => {
        expect(areaConsultaNoAdminBloqueada(false)).toBe(true);
    });

    it("ADMIN no bloquea el selector", () => {
        expect(areaConsultaNoAdminBloqueada(true)).toBe(false);
    });
});

describe("filtrarSubprogramasConsulta", () => {
    it("ADMIN + Área TODOS incluye subprocesos de varias áreas", () => {
        const resultado = filtrarSubprogramasConsulta(subprogramas, {
            esAdmin: true,
            areaIdEfectiva: TODOS,
        });
        expect(resultado.map((item) => item.id)).toEqual(["10", "11", "12"]);
    });

    it("JEFE con área efectiva muestra solo subprocesos de su área", () => {
        const resultado = filtrarSubprogramasConsulta(subprogramas, {
            esAdmin: false,
            areaIdEfectiva: "1",
        });
        expect(resultado.map((item) => item.id)).toEqual(["10", "12"]);
    });
});

describe("subprocesoConsultaDeshabilitado", () => {
    it("JEFE sin área resuelta queda disabled", () => {
        expect(subprocesoConsultaDeshabilitado(false, TODOS, false, null)).toBe(true);
    });

    it("JEFE con área resuelta queda habilitado", () => {
        expect(subprocesoConsultaDeshabilitado(false, "1", false, null)).toBe(false);
    });

    it("Tipo/ADMIN: ADMIN nunca disabled por área", () => {
        expect(subprocesoConsultaDeshabilitado(true, TODOS, false, null)).toBe(false);
    });
});

describe("resolverAreaPrincipalDesdeCatalogo", () => {
    it("A: API Producción con sesión stale no usa sesión", () => {
        expect(
            resolverAreaPrincipalDesdeCatalogo(
                [{ id: "1", nombre: "Producción", activo: true }],
                true,
            ),
        ).toEqual({
            areaId: "1",
            areaPrincipalNombre: "Producción",
        });
    });

    it("B: API [] devuelve null para limpiar sesión stale", () => {
        expect(resolverAreaPrincipalDesdeCatalogo([], true)).toBeNull();
    });
});

describe("etiquetaCatalogoConsulta", () => {
    it("conserva etiquetas de catálogos inactivos", () => {
        expect(etiquetaCatalogoConsulta("Calidad", false, "femenino")).toBe("Calidad (Inactiva)");
        expect(etiquetaCatalogoConsulta("GDO", false, "masculino")).toBe("GDO (Inactivo)");
    });
});

describe("construirFiltrosBaseConsulta", () => {
    it("JEFE con API cargada preselecciona área asignada", () => {
        expect(
            construirFiltrosBaseConsulta(
                false,
                [{ id: "1", nombre: "Producción", activo: true }],
                true,
            ),
        ).toEqual(expect.objectContaining({ area: "1" }));
    });

    it("JEFE con API [] no preselecciona área stale de sesión", () => {
        expect(
            construirFiltrosBaseConsulta(false, [], true, "2", "Calidad"),
        ).toEqual(expect.objectContaining({ area: TODOS, soloGlobales: false }));
    });
});

describe("formatFechaDocumento", () => {
    it("2026-08-25T13:21:57Z se muestra como 08:21 en Bogotá", () => {
        const formateado = formatFechaDocumento("2026-08-25T13:21:57Z");
        expect(formateado).toContain("25");
        expect(formateado).toMatch(/8:21|08:21/);
    });

    it("instante nocturno UTC pertenece al día anterior en Bogotá", () => {
        const formateado = formatFechaDocumento("2026-08-26T03:00:00Z");
        expect(formateado).toContain("25");
    });
});

describe("construirFiltrosApi — solo globales", () => {
    const filtrosBase = {
        codigo: "",
        titulo: "",
        area: "1",
        subprograma: TODOS,
        tipo: TODOS,
        estado: TODOS,
        soloGlobales: false,
        fechaDesde: "",
        fechaHasta: "",
    };

    it("casilla desmarcada no agrega alcanceConsulta", () => {
        expect(construirFiltrosApi(filtrosBase, 0, { incluirAreaEnConsulta: false })).not.toHaveProperty(
            "alcanceConsulta",
        );
    });

    it("casilla marcada agrega alcanceConsulta=GLOBALES", () => {
        expect(
            construirFiltrosApi(
                { ...filtrosBase, soloGlobales: true },
                0,
                { incluirAreaEnConsulta: false },
            ),
        ).toMatchObject({ alcanceConsulta: "GLOBALES" });
    });

    it("Solo globales + Tipo compone ambos filtros", () => {
        expect(
            construirFiltrosApi(
                { ...filtrosBase, soloGlobales: true, tipo: "5" },
                0,
                { incluirAreaEnConsulta: false },
            ),
        ).toMatchObject({ alcanceConsulta: "GLOBALES", tipoDocumentoId: 5 });
    });
});

describe("formatFechaCalendarioBogota", () => {
    it("formatea una fecha UTC como calendario en America/Bogota", () => {
        expect(formatFechaCalendarioBogota("2026-08-26T15:00:00Z")).toBe("26/08/2026");
    });

    it("devuelve null si no hay fecha", () => {
        expect(formatFechaCalendarioBogota(null)).toBeNull();
        expect(formatFechaCalendarioBogota(undefined)).toBeNull();
    });
});

describe("textoRetencionObsoleto", () => {
    it("incluye la fecha calendario del plazo", () => {
        expect(textoRetencionObsoleto("2028-08-26T15:00:00Z")).toBe(
            "Eliminación disponible a partir del 26/08/2028",
        );
    });
});

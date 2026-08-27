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
    type FiltrosDocumentos,
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

    it("JEFE con Área TODOS no fuerza su área asignada", () => {
        expect(
            resolverAreaIdEfectivaConsulta({
                esAdmin: false,
                filtroArea: TODOS,
                areasUsuario: [{ id: "1", nombre: "Producción", activo: true }],
                areasApiCargadas: true,
                sesionAreaId: "2",
                sesionAreaNombre: "Calidad",
            }),
        ).toBe(TODOS);
    });

    it("JEFE con Área seleccionada usa el filtro, aunque no sea la suya", () => {
        expect(
            resolverAreaIdEfectivaConsulta({
                esAdmin: false,
                filtroArea: "8",
                areasUsuario: [{ id: "1", nombre: "Producción", activo: true }],
                areasApiCargadas: true,
            }),
        ).toBe("8");
    });

    it("JEFE con API [] y filtro TODOS permanece en TODOS", () => {
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
});

describe("areaConsultaNoAdminBloqueada", () => {
    it("JEFE no bloquea el selector de Área responsable", () => {
        expect(areaConsultaNoAdminBloqueada(false)).toBe(false);
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
    it("queda disabled solo mientras cargan catálogos", () => {
        expect(subprocesoConsultaDeshabilitado(false, TODOS, true, null)).toBe(true);
    });

    it("queda disabled si fallaron los catálogos", () => {
        expect(subprocesoConsultaDeshabilitado(false, TODOS, false, "error")).toBe(true);
    });

    it("JEFE sin área de filtro permanece habilitado", () => {
        expect(subprocesoConsultaDeshabilitado(false, TODOS, false, null)).toBe(false);
    });

    it("ADMIN nunca disabled por área", () => {
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
    it("JEFE no preselecciona área asignada", () => {
        expect(
            construirFiltrosBaseConsulta(
                false,
                [{ id: "1", nombre: "Producción", activo: true }],
                true,
            ),
        ).toEqual(expect.objectContaining({ area: TODOS, alcance: TODOS }));
    });

    it("JEFE con API [] inicia en Área Todos y Alcance Todos", () => {
        expect(
            construirFiltrosBaseConsulta(false, [], true, "2", "Calidad"),
        ).toEqual(expect.objectContaining({ area: TODOS, alcance: TODOS }));
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

describe("construirFiltrosApi — alcance", () => {
    const filtrosBase: FiltrosDocumentos = {
        codigo: "",
        titulo: "",
        area: "1",
        subprograma: TODOS,
        tipo: TODOS,
        estado: TODOS,
        alcance: TODOS,
        fechaDesde: "",
        fechaHasta: "",
    };

    it("Todos no agrega alcanceConsulta", () => {
        expect(construirFiltrosApi(filtrosBase, 0)).not.toHaveProperty("alcanceConsulta");
    });

    it("Global agrega alcanceConsulta=GLOBALES", () => {
        expect(construirFiltrosApi({ ...filtrosBase, alcance: "GLOBALES" }, 0)).toMatchObject({
            alcanceConsulta: "GLOBALES",
        });
    });

    it("Áreas específicas agrega alcanceConsulta=AREAS_ESPECIFICAS", () => {
        expect(
            construirFiltrosApi({ ...filtrosBase, alcance: "AREAS_ESPECIFICAS" }, 0),
        ).toMatchObject({ alcanceConsulta: "AREAS_ESPECIFICAS" });
    });

    it("Global + Tipo compone ambos filtros", () => {
        expect(
            construirFiltrosApi({ ...filtrosBase, alcance: "GLOBALES", tipo: "5" }, 0),
        ).toMatchObject({ alcanceConsulta: "GLOBALES", tipoDocumentoId: 5 });
    });

    it("no-admin también envía areaId cuando filtra por Área responsable", () => {
        expect(construirFiltrosApi(filtrosBase, 0, { incluirAreaEnConsulta: true })).toMatchObject({
            areaId: 1,
        });
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

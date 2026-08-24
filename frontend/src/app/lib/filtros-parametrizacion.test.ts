import { describe, expect, it } from "vitest";

import type { AreaCatalogo } from "./areas-api";
import {
    filtrarAreasCatalogo,
    filtrarSubprogramasCatalogo,
    filtrarTiposDocumentoCatalogo,
} from "./filtros-parametrizacion";
import type { SubprogramaCatalogo } from "./subprogramas-api";
import type { TipoDocumentoCatalogo } from "./tipos-documento-api";

const areasMock: AreaCatalogo[] = [
    {
        id: "1",
        codigo: "GHUM",
        nombre: "Gestión Humana",
        descripcion: "",
        activo: true,
    },
    {
        id: "2",
        codigo: "GCAL",
        nombre: "Gestión de la Calidad",
        descripcion: "",
        activo: false,
    },
];

const subprogramasMock: SubprogramaCatalogo[] = [
    {
        id: "10",
        nombre: "Limpieza y Desinfección",
        descripcion: "",
        areaId: "3",
        areaCodigo: "L&D",
        areaNombre: "Limpieza y Desinfección",
        areaActiva: true,
        activo: true,
    },
    {
        id: "11",
        nombre: "Gestión Ambiental",
        descripcion: "",
        areaId: "4",
        areaCodigo: "GAMB",
        areaNombre: "Gestión Ambiental",
        areaActiva: true,
        activo: false,
    },
];

describe("filtrarAreasCatalogo", () => {
    it("encuentra por nombre sin tilde", () => {
        const resultado = filtrarAreasCatalogo(areasMock, "gestion humana", "todos");
        expect(resultado.map((area) => area.id)).toEqual(["1"]);
    });

    it("encuentra por código", () => {
        const resultado = filtrarAreasCatalogo(areasMock, "GHUM", "todos");
        expect(resultado.map((area) => area.id)).toEqual(["1"]);
    });

    it("combina búsqueda con estado inactivos", () => {
        const resultado = filtrarAreasCatalogo(areasMock, "gestion", "inactivos");
        expect(resultado.map((area) => area.id)).toEqual(["2"]);
    });
});

describe("filtrarSubprogramasCatalogo", () => {
    it("encuentra por nombre parcial", () => {
        const resultado = filtrarSubprogramasCatalogo(subprogramasMock, "limpieza", "todos");
        expect(resultado.map((item) => item.id)).toEqual(["10"]);
    });

    it("encuentra por código de área", () => {
        const resultado = filtrarSubprogramasCatalogo(subprogramasMock, "L&D", "todos");
        expect(resultado.map((item) => item.id)).toEqual(["10"]);
    });

    it("encuentra por nombre de área responsable", () => {
        const resultado = filtrarSubprogramasCatalogo(
            subprogramasMock,
            "Gestión Ambiental",
            "todos",
        );
        expect(resultado.map((item) => item.id)).toEqual(["11"]);
    });

    it("filtra solo inactivos", () => {
        const resultado = filtrarSubprogramasCatalogo(subprogramasMock, "", "inactivos");
        expect(resultado.map((item) => item.id)).toEqual(["11"]);
    });
});

const tiposMock: TipoDocumentoCatalogo[] = [
    {
        id: "1",
        nombre: "Protocolo",
        descripcion: "",
        activo: true,
    },
    {
        id: "2",
        nombre: "Formato",
        descripcion: "",
        activo: false,
    },
];

describe("filtrarTiposDocumentoCatalogo", () => {
    it("encuentra por nombre parcial", () => {
        const resultado = filtrarTiposDocumentoCatalogo(tiposMock, "protocolo", "todos");
        expect(resultado.map((item) => item.id)).toEqual(["1"]);
    });

    it("encuentra por abreviatura institucional PT", () => {
        const resultado = filtrarTiposDocumentoCatalogo(tiposMock, "PT", "todos");
        expect(resultado.map((item) => item.id)).toEqual(["1"]);
    });

    it("encuentra por abreviatura institucional FO", () => {
        const resultado = filtrarTiposDocumentoCatalogo(tiposMock, "FO", "todos");
        expect(resultado.map((item) => item.id)).toEqual(["2"]);
    });

    it("combina búsqueda con estado inactivos", () => {
        const resultado = filtrarTiposDocumentoCatalogo(tiposMock, "pro", "inactivos");
        expect(resultado).toEqual([]);
    });

    it("combina búsqueda parcial con estado inactivos", () => {
        const resultado = filtrarTiposDocumentoCatalogo(tiposMock, "forma", "inactivos");
        expect(resultado.map((item) => item.id)).toEqual(["2"]);
    });
});

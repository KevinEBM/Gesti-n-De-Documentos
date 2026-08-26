import { describe, expect, it } from "vitest";

import {
    mapSubprogramaResponseDto,
    type SubprogramaResponseDto,
} from "./subprogramas-api";

const respuestaDto: SubprogramaResponseDto = {
    id: 10,
    codigo: "L&D",
    nombre: "Limpieza y Desinfección",
    descripcion: null,
    area: {
        id: 3,
        codigo: "GAMB",
        nombre: "Gestión Ambiental",
        descripcion: null,
        activo: true,
        fechaCreacion: "2026-01-01T08:00:00Z",
        fechaActualizacion: "2026-01-01T08:00:00Z",
    },
    activo: true,
    fechaCreacion: "2026-01-01T08:00:00Z",
    fechaActualizacion: "2026-01-01T08:00:00Z",
};

describe("mapSubprogramaResponseDto", () => {
    it("conserva el código junto con el resto del catálogo", () => {
        const catalogo = mapSubprogramaResponseDto(respuestaDto);

        expect(catalogo).toEqual({
            id: "10",
            codigo: "L&D",
            nombre: "Limpieza y Desinfección",
            descripcion: "",
            areaId: "3",
            areaCodigo: "GAMB",
            areaNombre: "Gestión Ambiental",
            areaActiva: true,
            activo: true,
        });
    });

    it("no transforma el código recibido del backend", () => {
        const catalogo = mapSubprogramaResponseDto({ ...respuestaDto, codigo: "C&D" });

        expect(catalogo.codigo).toBe("C&D");
    });
});

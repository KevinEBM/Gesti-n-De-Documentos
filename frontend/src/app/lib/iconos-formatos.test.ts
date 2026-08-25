import { describe, expect, it } from "vitest";
import {
    IconFile,
    IconFileChart,
    IconFileInfo,
    IconPhoto,
    IconTemplate,
    IconWorld,
} from "@tabler/icons-react";

import { obtenerIconoFormato } from "./iconos-formatos";

describe("obtenerIconoFormato", () => {
    it("Plantilla -> IconTemplate", () => {
        expect(obtenerIconoFormato("Plantilla").icono).toBe(IconTemplate);
    });

    it("Otros Documentos -> IconFile", () => {
        expect(obtenerIconoFormato("Otros Documentos").icono).toBe(IconFile);
    });

    it("Documentos Externos -> IconWorld", () => {
        expect(obtenerIconoFormato("Documentos Externos").icono).toBe(IconWorld);
    });

    it("Imagenes -> IconPhoto", () => {
        expect(obtenerIconoFormato("Imagenes").icono).toBe(IconPhoto);
    });

    it("Imágenes -> IconPhoto", () => {
        expect(obtenerIconoFormato("Imágenes").icono).toBe(IconPhoto);
    });

    it("Diagrama conserva IconFileChart", () => {
        expect(obtenerIconoFormato("Diagrama").icono).toBe(IconFileChart);
    });

    it("tipo desconocido usa fallback IconFileInfo", () => {
        expect(obtenerIconoFormato("Tipo Inventado").icono).toBe(IconFileInfo);
    });
});

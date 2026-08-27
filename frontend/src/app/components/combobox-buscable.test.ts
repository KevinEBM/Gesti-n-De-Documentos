import { describe, expect, it } from "vitest";

import { filtrarOpcionCombobox } from "./combobox-buscable";

describe("filtrarOpcionCombobox", () => {
    it("encuentra un área por fragmento del nombre sin tilde", () => {
        expect(filtrarOpcionCombobox("Gestión de Calidad", "cal")).toBe(1);
        expect(filtrarOpcionCombobox("Gestión Humana", "cal")).toBe(0);
    });

    it("encuentra un subproceso por código o nombre", () => {
        expect(filtrarOpcionCombobox("SST Seguridad y Salud en el Trabajo", "sst")).toBe(1);
        expect(filtrarOpcionCombobox("SST Seguridad y Salud en el Trabajo", "salud")).toBe(1);
        expect(filtrarOpcionCombobox("L&D Limpieza y Desinfección", "sst")).toBe(0);
    });

    it("encuentra un tipo por código o nombre y distingue ODE por nombre", () => {
        expect(filtrarOpcionCombobox("PD Procedimiento", "pd")).toBe(1);
        expect(filtrarOpcionCombobox("ODE Plantilla", "ode")).toBe(1);
        expect(filtrarOpcionCombobox("ODE Documentos Externos", "ode")).toBe(1);
        expect(filtrarOpcionCombobox("ODE Imágenes", "imagenes")).toBe(1);
        expect(filtrarOpcionCombobox("ODE Plantilla", "imagenes")).toBe(0);
    });

    it("con búsqueda vacía conserva todas las opciones", () => {
        expect(filtrarOpcionCombobox("Cualquier valor", "  ")).toBe(1);
    });
});

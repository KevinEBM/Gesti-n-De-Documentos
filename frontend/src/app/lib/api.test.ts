import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { apiFetch } from "./api";
import { suscribirSesionInvalidada } from "./auth-sesion-invalida";
import { getSession, saveSession, type AuthSession } from "./auth-storage";

const storage = new Map<string, string>();

const sesionBase: AuthSession = {
    token: "token-prueba",
    tipo: "Bearer",
    usuario: {
        id: "1",
        nombre: "Usuario Prueba",
        correo: "prueba@empresa.com",
        rol: "jefe_area",
        activo: true,
        areaId: "2",
        areaPrincipalNombre: "Calidad",
    },
};

function mockSessionStorage() {
    storage.clear();
    const sessionStorageMock = {
        getItem: (key: string) => storage.get(key) ?? null,
        setItem: (key: string, value: string) => {
            storage.set(key, value);
        },
        removeItem: (key: string) => {
            storage.delete(key);
        },
        clear: () => {
            storage.clear();
        },
    };
    vi.stubGlobal("sessionStorage", sessionStorageMock);
    vi.stubGlobal("window", { sessionStorage: sessionStorageMock });
}

function respuestaError(status: number, mensaje: string) {
    return {
        ok: false,
        status,
        json: async () => ({
            exito: false,
            mensaje,
            datos: null,
            errores: null,
            fechaHora: "",
        }),
    };
}

describe("apiFetch — 401/403 y sesión", () => {
    let avisos: number[];
    let cancelar: () => void;

    beforeEach(() => {
        mockSessionStorage();
        saveSession(sesionBase);
        avisos = [];
        cancelar = suscribirSesionInvalidada(() => {
            avisos.push(1);
        });
    });

    afterEach(() => {
        cancelar();
        vi.unstubAllGlobals();
    });

    it("401 autenticado limpia sessionStorage, notifica y lanza ApiError", async () => {
        vi.stubGlobal(
            "fetch",
            vi.fn(async () => respuestaError(401, "Autenticación requerida")),
        );

        await expect(apiFetch("/api/auth/me")).rejects.toMatchObject({
            name: "ApiError",
            status: 401,
            message: "Autenticación requerida",
        });

        expect(getSession()).toBeNull();
        expect(avisos).toEqual([1]);
    });

    it("401 en POST /api/auth/login no invalida sesión y mantiene ApiError", async () => {
        vi.stubGlobal(
            "fetch",
            vi.fn(async () => respuestaError(401, "Credenciales incorrectas")),
        );

        await expect(
            apiFetch("/api/auth/login", {
                method: "POST",
                body: JSON.stringify({ correo: "a@b.com", contrasena: "x" }),
            }),
        ).rejects.toMatchObject({
            name: "ApiError",
            status: 401,
            message: "Credenciales incorrectas",
        });

        expect(getSession()?.token).toBe("token-prueba");
        expect(avisos).toEqual([]);
    });

    it("403 no invalida sesión", async () => {
        vi.stubGlobal(
            "fetch",
            vi.fn(async () => respuestaError(403, "No autorizado")),
        );

        await expect(apiFetch("/api/documentos/1")).rejects.toMatchObject({
            name: "ApiError",
            status: 403,
        });

        expect(getSession()?.token).toBe("token-prueba");
        expect(avisos).toEqual([]);
    });
});

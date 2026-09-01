import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import {
    esPeticionLogin,
    invalidarSesion,
    invalidarSesionSiNoAutorizada,
    suscribirSesionInvalidada,
} from "./auth-sesion-invalida";
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

describe("auth-sesion-invalida", () => {
    beforeEach(() => {
        mockSessionStorage();
        saveSession(sesionBase);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it("reconoce POST /api/auth/login y no otras rutas", () => {
        expect(esPeticionLogin("/api/auth/login", "POST")).toBe(true);
        expect(esPeticionLogin("/api/auth/login", "post")).toBe(true);
        expect(esPeticionLogin("/api/auth/me", "GET")).toBe(false);
        expect(esPeticionLogin("/api/auth/login", "GET")).toBe(false);
        expect(esPeticionLogin("/api/documentos/1/descarga")).toBe(false);
    });

    it("401 autenticado limpia sesión y notifica", () => {
        const avisos: number[] = [];
        const cancelar = suscribirSesionInvalidada(() => {
            avisos.push(1);
        });

        invalidarSesionSiNoAutorizada(401, "/api/auth/me", "GET");

        expect(getSession()).toBeNull();
        expect(avisos).toEqual([1]);
        cancelar();
    });

    it("401 de login no invalida sesión ni notifica", () => {
        const avisos: number[] = [];
        const cancelar = suscribirSesionInvalidada(() => {
            avisos.push(1);
        });

        invalidarSesionSiNoAutorizada(401, "/api/auth/login", "POST");

        expect(getSession()?.token).toBe("token-prueba");
        expect(avisos).toEqual([]);
        cancelar();
    });

    it("403 no invalida sesión", () => {
        invalidarSesionSiNoAutorizada(403, "/api/documentos/1", "GET");
        expect(getSession()?.token).toBe("token-prueba");
    });

    it("invalidarSesion limpia y deja de notificar tras cancelar la suscripción", () => {
        const avisos: number[] = [];
        const cancelar = suscribirSesionInvalidada(() => {
            avisos.push(1);
        });
        cancelar();
        invalidarSesion();
        expect(getSession()).toBeNull();
        expect(avisos).toEqual([]);
    });
});

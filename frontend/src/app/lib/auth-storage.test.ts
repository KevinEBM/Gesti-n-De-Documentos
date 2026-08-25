import { beforeEach, describe, expect, it, vi } from "vitest";

import {
    getSession,
    patchUsuarioAreaEnSesion,
    saveSession,
    type AuthSession,
} from "./auth-storage";
import { resolverAreaPrincipalDesdeCatalogo } from "./documentos-consulta-shared";

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

describe("patchUsuarioAreaEnSesion", () => {
    beforeEach(() => {
        mockSessionStorage();
        saveSession(sesionBase);
    });

    it("A: actualiza sesión y sessionStorage a Producción", () => {
        const actualizado = patchUsuarioAreaEnSesion("1", "Producción");
        expect(actualizado?.areaId).toBe("1");
        expect(actualizado?.areaPrincipalNombre).toBe("Producción");
        expect(getSession()?.usuario.areaPrincipalNombre).toBe("Producción");
    });

    it("B: limpia área stale cuando API devuelve []", () => {
        const actualizado = patchUsuarioAreaEnSesion(undefined, null);
        expect(actualizado?.areaId).toBeUndefined();
        expect(actualizado?.areaPrincipalNombre).toBeNull();
        expect(getSession()?.usuario.areaPrincipalNombre).toBeNull();
        expect(getSession()?.usuario.areaId).toBeUndefined();
    });

    it("conserva token y demás datos del usuario", () => {
        patchUsuarioAreaEnSesion("1", "Producción");
        const session = getSession();
        expect(session?.token).toBe("token-prueba");
        expect(session?.usuario.correo).toBe("prueba@empresa.com");
        expect(session?.usuario.rol).toBe("jefe_area");
    });
});

describe("sincronización de área desde catálogo API", () => {
    beforeEach(() => {
        mockSessionStorage();
    });

    it("A: sesión Calidad + API Producción -> Producción", () => {
        saveSession(sesionBase);
        const resuelta = resolverAreaPrincipalDesdeCatalogo(
            [{ id: "1", nombre: "Producción", activo: true }],
            true,
        );
        expect(resuelta).toEqual({
            areaId: "1",
            areaPrincipalNombre: "Producción",
        });

        const actualizado = patchUsuarioAreaEnSesion(
            resuelta!.areaId,
            resuelta!.areaPrincipalNombre,
        );
        expect(actualizado?.areaPrincipalNombre).toBe("Producción");
        expect(getSession()?.usuario.areaPrincipalNombre).toBe("Producción");
    });

    it("ADMIN no recibe área del catálogo al sincronizar sesión", () => {
        const adminSession: AuthSession = {
            ...sesionBase,
            usuario: {
                ...sesionBase.usuario,
                rol: "administrador",
                areaId: undefined,
                areaPrincipalNombre: null,
            },
        };
        saveSession(adminSession);
        expect(getSession()?.usuario.rol).toBe("administrador");
        expect(getSession()?.usuario.areaPrincipalNombre).toBeNull();
    });
});

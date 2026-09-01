import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { suscribirSesionInvalidada } from "./auth-sesion-invalida";
import { getSession, saveSession, type AuthSession } from "./auth-storage";
import { descargarVersionVigente } from "./documentos-api";

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

describe("descargarArchivoDocumento — 401", () => {
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

    it("401 en descarga invalida la sesión y lanza ApiError", async () => {
        vi.stubGlobal(
            "fetch",
            vi.fn(async () => ({
                ok: false,
                status: 401,
                json: async () => ({
                    exito: false,
                    mensaje: "Autenticación requerida",
                    datos: null,
                    errores: null,
                    fechaHora: "",
                }),
            })),
        );

        await expect(descargarVersionVigente("12")).rejects.toMatchObject({
            name: "ApiError",
            status: 401,
        });

        expect(getSession()).toBeNull();
        expect(avisos).toEqual([1]);
    });
});

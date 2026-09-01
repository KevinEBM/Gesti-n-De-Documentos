// @vitest-environment jsdom
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { apiFetch } from "@/lib/api";
import type { PerfilUsuarioResponseDto } from "@/lib/api";
import { saveSession, type AuthSession } from "@/lib/auth-storage";
import { IntranetProvider } from "@/lib/store";

vi.mock("@tanstack/react-router", () => ({
    createFileRoute: () => (opciones: Record<string, unknown>) => opciones,
    Link: ({ children }: { children?: ReactNode }) => <a>{children}</a>,
}));

vi.mock("@/components/AppShell", () => ({
    AppShell: ({ children, titulo }: { children?: ReactNode; titulo?: string }) => (
        <div>
            <h1>{titulo}</h1>
            {children}
        </div>
    ),
}));

vi.mock("@/lib/auth-api", () => ({ obtenerPerfilActual: vi.fn() }));

const { obtenerPerfilActual } = await import("@/lib/auth-api");
const { Route } = await import("./app.inicio");

const PaginaInicio = (Route as unknown as { component: () => ReactNode }).component;

const AREA_ANTERIOR = "Gestión Ambiental";
const AREA_NUEVA = "Gestión de la Calidad";

function perfilDe(
    rolBackend: PerfilUsuarioResponseDto["rol"],
    area?: { id: number; nombre: string },
): PerfilUsuarioResponseDto {
    return {
        id: 9,
        correo: "juan@plantar.com",
        nombres: "Juan",
        apellidos: "Pérez",
        rol: rolBackend,
        areaPrincipalId: area?.id ?? null,
        areaPrincipalNombre: area?.nombre ?? null,
    };
}

function sesionDe(rol: AuthSession["usuario"]["rol"], area?: { id: string; nombre: string }): AuthSession {
    return {
        token: "token-prueba",
        tipo: "Bearer",
        usuario: {
            id: "9",
            nombre: "Juan Pérez",
            correo: "juan@plantar.com",
            rol,
            activo: true,
            areaId: area?.id,
            areaPrincipalNombre: area?.nombre ?? null,
        },
    };
}

function areaMostrada(): string {
    const termino = screen.getByText("Área").parentElement?.querySelector("dd");
    return termino?.textContent?.trim() ?? "";
}

beforeEach(() => {
    window.sessionStorage.clear();
});

afterEach(() => {
    cleanup();
    vi.clearAllMocks();
    vi.unstubAllGlobals();
});

function renderizarInicio() {
    return render(
        <IntranetProvider>
            <PaginaInicio />
        </IntranetProvider>,
    );
}

describe("Inicio — área del usuario", () => {
    it("muestra el área actual del usuario", async () => {
        saveSession(sesionDe("jefe_area", { id: "3", nombre: AREA_ANTERIOR }));
        vi.mocked(obtenerPerfilActual).mockResolvedValue(
            perfilDe("JEFE_AREA", { id: 3, nombre: AREA_ANTERIOR }),
        );

        renderizarInicio();

        await waitFor(() => expect(obtenerPerfilActual).toHaveBeenCalled());
        expect(areaMostrada()).toBe(AREA_ANTERIOR);
    });

    it("reemplaza el área antigua de la sesión cuando el backend devuelve otra", async () => {
        saveSession(sesionDe("jefe_area", { id: "3", nombre: AREA_ANTERIOR }));
        vi.mocked(obtenerPerfilActual).mockResolvedValue(
            perfilDe("JEFE_AREA", { id: 5, nombre: AREA_NUEVA }),
        );

        renderizarInicio();

        await waitFor(() => expect(areaMostrada()).toBe(AREA_NUEVA));
        expect(areaMostrada()).not.toBe(AREA_ANTERIOR);
    });

    it("persiste el área nueva en la sesión almacenada", async () => {
        saveSession(sesionDe("administrativo", { id: "3", nombre: AREA_ANTERIOR }));
        vi.mocked(obtenerPerfilActual).mockResolvedValue(
            perfilDe("ADMINISTRATIVO", { id: 5, nombre: AREA_NUEVA }),
        );

        renderizarInicio();

        await waitFor(() => expect(areaMostrada()).toBe(AREA_NUEVA));
        const guardada = JSON.parse(
            window.sessionStorage.getItem("intranet.auth.session") ?? "{}",
        ) as AuthSession;
        expect(guardada.usuario.areaPrincipalNombre).toBe(AREA_NUEVA);
        expect(guardada.usuario.areaId).toBe("5");
        expect(guardada.token).toBe("token-prueba");
    });

    it("mantiene No aplica para ADMINISTRADOR", async () => {
        saveSession(sesionDe("administrador"));
        vi.mocked(obtenerPerfilActual).mockResolvedValue(perfilDe("ADMINISTRADOR"));

        renderizarInicio();

        await waitFor(() => expect(obtenerPerfilActual).toHaveBeenCalled());
        expect(areaMostrada()).toBe("No aplica");
    });

    it("no altera nombre ni rol al actualizar el área", async () => {
        saveSession(sesionDe("jefe_area", { id: "3", nombre: AREA_ANTERIOR }));
        vi.mocked(obtenerPerfilActual).mockResolvedValue(
            perfilDe("JEFE_AREA", { id: 5, nombre: AREA_NUEVA }),
        );

        renderizarInicio();

        await waitFor(() => expect(areaMostrada()).toBe(AREA_NUEVA));
        expect(screen.getByText("Juan Pérez")).toBeDefined();
        const guardada = JSON.parse(
            window.sessionStorage.getItem("intranet.auth.session") ?? "{}",
        ) as AuthSession;
        expect(guardada.usuario.nombre).toBe("Juan Pérez");
        expect(guardada.usuario.rol).toBe("jefe_area");
        expect(guardada.token).toBe("token-prueba");
    });

    it("conserva la sesión y el último área conocida si falla la consulta", async () => {
        saveSession(sesionDe("jefe_area", { id: "3", nombre: AREA_ANTERIOR }));
        vi.mocked(obtenerPerfilActual).mockRejectedValue(new Error("backend caído"));

        renderizarInicio();

        await waitFor(() => expect(obtenerPerfilActual).toHaveBeenCalled());
        expect(areaMostrada()).toBe(AREA_ANTERIOR);
        expect(window.sessionStorage.getItem("intranet.auth.session")).not.toBeNull();
    });

    it("avisa de que el área no pudo confirmarse cuando falla la consulta", async () => {
        saveSession(sesionDe("jefe_area", { id: "3", nombre: AREA_ANTERIOR }));
        vi.mocked(obtenerPerfilActual).mockRejectedValue(new Error("backend caído"));

        renderizarInicio();

        await waitFor(() =>
            expect(screen.getByText(/No se pudo confirmar tu área/)).toBeDefined(),
        );
    });

    it("reintentar recupera el área nueva y retira el aviso", async () => {
        saveSession(sesionDe("jefe_area", { id: "3", nombre: AREA_ANTERIOR }));
        vi.mocked(obtenerPerfilActual).mockRejectedValueOnce(new Error("backend caído"));

        renderizarInicio();

        const reintentar = await screen.findByRole("button", { name: "Reintentar" });
        vi.mocked(obtenerPerfilActual).mockResolvedValue(
            perfilDe("JEFE_AREA", { id: 5, nombre: AREA_NUEVA }),
        );
        await userEvent.click(reintentar);

        await waitFor(() => expect(areaMostrada()).toBe(AREA_NUEVA));
        expect(screen.queryByText(/No se pudo confirmar tu área/)).toBeNull();
    });

    it("cierra la sesión si el perfil responde 401", async () => {
        saveSession(sesionDe("jefe_area", { id: "3", nombre: AREA_ANTERIOR }));
        vi.mocked(obtenerPerfilActual).mockImplementation(() => apiFetch("/api/auth/me"));
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

        renderizarInicio();

        await waitFor(() =>
            expect(window.sessionStorage.getItem("intranet.auth.session")).toBeNull(),
        );
        expect(screen.queryByText("Área")).toBeNull();
    });
});

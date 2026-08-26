// @vitest-environment jsdom
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import type { AreaCatalogo } from "@/lib/areas-api";
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

vi.mock("@/lib/areas-api", () => ({ listarAreas: vi.fn() }));

const { listarAreas } = await import("@/lib/areas-api");
const { Route } = await import("./app.inicio");

const PaginaInicio = (Route as unknown as { component: () => ReactNode }).component;

const AREA_ANTERIOR = "Gestión Ambiental";
const AREA_NUEVA = "Gestión de la Calidad";

function areaCatalogo(id: string, nombre: string): AreaCatalogo {
    return { id, codigo: "COD", nombre, descripcion: "", activo: true };
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
        vi.mocked(listarAreas).mockResolvedValue([areaCatalogo("3", AREA_ANTERIOR)]);

        renderizarInicio();

        await waitFor(() => expect(listarAreas).toHaveBeenCalled());
        expect(areaMostrada()).toBe(AREA_ANTERIOR);
    });

    it("reemplaza el área antigua de la sesión cuando el backend devuelve otra", async () => {
        saveSession(sesionDe("jefe_area", { id: "3", nombre: AREA_ANTERIOR }));
        vi.mocked(listarAreas).mockResolvedValue([areaCatalogo("5", AREA_NUEVA)]);

        renderizarInicio();

        await waitFor(() => expect(areaMostrada()).toBe(AREA_NUEVA));
        expect(areaMostrada()).not.toBe(AREA_ANTERIOR);
    });

    it("persiste el área nueva en la sesión almacenada", async () => {
        saveSession(sesionDe("administrativo", { id: "3", nombre: AREA_ANTERIOR }));
        vi.mocked(listarAreas).mockResolvedValue([areaCatalogo("5", AREA_NUEVA)]);

        renderizarInicio();

        await waitFor(() => expect(areaMostrada()).toBe(AREA_NUEVA));
        const guardada = JSON.parse(
            window.sessionStorage.getItem("intranet.auth.session") ?? "{}",
        ) as AuthSession;
        expect(guardada.usuario.areaPrincipalNombre).toBe(AREA_NUEVA);
        expect(guardada.usuario.areaId).toBe("5");
    });

    it("mantiene No aplica para ADMINISTRADOR sin consultar áreas", async () => {
        saveSession(sesionDe("administrador"));

        renderizarInicio();

        expect(areaMostrada()).toBe("No aplica");
        expect(listarAreas).not.toHaveBeenCalled();
    });

    it("no altera nombre ni rol al actualizar el área", async () => {
        saveSession(sesionDe("jefe_area", { id: "3", nombre: AREA_ANTERIOR }));
        vi.mocked(listarAreas).mockResolvedValue([areaCatalogo("5", AREA_NUEVA)]);

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
        vi.mocked(listarAreas).mockRejectedValue(new Error("backend caído"));

        renderizarInicio();

        await waitFor(() => expect(listarAreas).toHaveBeenCalled());
        expect(areaMostrada()).toBe(AREA_ANTERIOR);
        expect(window.sessionStorage.getItem("intranet.auth.session")).not.toBeNull();
    });

    it("avisa de que el área no pudo confirmarse cuando falla la consulta", async () => {
        saveSession(sesionDe("jefe_area", { id: "3", nombre: AREA_ANTERIOR }));
        vi.mocked(listarAreas).mockRejectedValue(new Error("backend caído"));

        renderizarInicio();

        await waitFor(() =>
            expect(screen.getByText(/No se pudo confirmar tu área/)).toBeDefined(),
        );
    });

    it("reintentar recupera el área nueva y retira el aviso", async () => {
        saveSession(sesionDe("jefe_area", { id: "3", nombre: AREA_ANTERIOR }));
        vi.mocked(listarAreas).mockRejectedValueOnce(new Error("backend caído"));

        renderizarInicio();

        const reintentar = await screen.findByRole("button", { name: "Reintentar" });
        vi.mocked(listarAreas).mockResolvedValue([areaCatalogo("5", AREA_NUEVA)]);
        await userEvent.click(reintentar);

        await waitFor(() => expect(areaMostrada()).toBe(AREA_NUEVA));
        expect(screen.queryByText(/No se pudo confirmar tu área/)).toBeNull();
    });
});

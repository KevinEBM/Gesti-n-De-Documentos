// @vitest-environment jsdom
import { cleanup, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ReactNode } from "react";
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from "vitest";

import type { AreaCatalogo } from "@/lib/areas-api";
import type { SubprogramaCatalogo } from "@/lib/subprogramas-api";
import type { TipoDocumentoCatalogo } from "@/lib/tipos-documento-api";

vi.mock("@tanstack/react-router", () => ({
    createFileRoute: () => (opciones: Record<string, unknown>) => opciones,
    useNavigate: () => vi.fn(),
    Link: ({ children }: { children?: ReactNode }) => <a>{children}</a>,
}));

vi.mock("@/components/AppShell", () => ({
    AppShell: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
}));

vi.mock("@/lib/store", () => ({
    useIntranet: () => ({ permisos: { publicarDocumentos: true } }),
}));

vi.mock("sonner", () => ({
    toast: { success: vi.fn(), info: vi.fn(), error: vi.fn() },
}));

vi.mock("@/lib/areas-api", () => ({ listarAreas: vi.fn() }));
vi.mock("@/lib/subprogramas-api", () => ({ listarSubprogramas: vi.fn() }));
vi.mock("@/lib/tipos-documento-api", () => ({ listarTiposDocumento: vi.fn() }));
vi.mock("@/lib/documentos-api", () => ({ publicarDocumentoInicial: vi.fn() }));

const { listarAreas } = await import("@/lib/areas-api");
const { listarSubprogramas } = await import("@/lib/subprogramas-api");
const { listarTiposDocumento } = await import("@/lib/tipos-documento-api");
const { Route } = await import("./app.publicar");

// createFileRoute está mockeado y devuelve las opciones tal cual, pero los tipos
// reales del router siguen vigentes y no exponen `component`.
const PaginaPublicar = (Route as unknown as { component: () => ReactNode }).component;

const areas: AreaCatalogo[] = [
    { id: "3", codigo: "AMB", nombre: "Gestión Ambiental", descripcion: "", activo: true },
    { id: "5", codigo: "CAL", nombre: "Gestión de la Calidad", descripcion: "", activo: true },
];

const subprogramas: SubprogramaCatalogo[] = [
    {
        id: "10",
        codigo: "L&D",
        nombre: "Limpieza y Desinfección",
        descripcion: "",
        areaId: "3",
        areaCodigo: "AMB",
        areaNombre: "Gestión Ambiental",
        areaActiva: true,
        activo: true,
    },
    {
        id: "11",
        codigo: "CRS",
        nombre: "Control de Residuos Sólidos",
        descripcion: "",
        areaId: "3",
        areaCodigo: "AMB",
        areaNombre: "Gestión Ambiental",
        areaActiva: true,
        activo: true,
    },
    {
        id: "20",
        codigo: "AUD",
        nombre: "Auditoría Interna",
        descripcion: "",
        areaId: "5",
        areaCodigo: "CAL",
        areaNombre: "Gestión de la Calidad",
        areaActiva: true,
        activo: true,
    },
];

const tipos: TipoDocumentoCatalogo[] = [
    { id: "7", codigo: "PG", nombre: "Programa", descripcion: "", activo: true },
    { id: "8", codigo: "FO", nombre: "Formato", descripcion: "", activo: true },
    { id: "9", codigo: "ODE", nombre: "Plantilla", descripcion: "", activo: true },
    { id: "10", codigo: "ODE", nombre: "Documentos Externos", descripcion: "", activo: true },
    { id: "11", codigo: "ODE", nombre: "Imágenes", descripcion: "", activo: true },
];

const NOMBRE_ARCHIVO = "PR-L&D-PG-01 PROGRAMA LIMPIEZA Y DESINFECCIÓN V5.docx";

/** Radix Select y Popper dependen de APIs de layout que jsdom no implementa. */
beforeAll(() => {
    globalThis.ResizeObserver = class {
        observe() {}
        unobserve() {}
        disconnect() {}
    };
    Element.prototype.scrollIntoView = () => {};
    Element.prototype.hasPointerCapture = () => false;
    Element.prototype.setPointerCapture = () => {};
    Element.prototype.releasePointerCapture = () => {};
});

beforeEach(() => {
    vi.mocked(listarAreas).mockResolvedValue(areas);
    vi.mocked(listarSubprogramas).mockResolvedValue(subprogramas);
    vi.mocked(listarTiposDocumento).mockResolvedValue(tipos);
});

afterEach(() => {
    cleanup();
    vi.clearAllMocks();
});

type Usuario = ReturnType<typeof userEvent.setup>;

function selectDe(etiqueta: string): HTMLButtonElement {
    const label = screen.getByText(new RegExp(`^${etiqueta}`), { selector: "label" });
    const contenedor = label.parentElement;
    if (!contenedor) throw new Error(`Campo sin contenedor: ${etiqueta}`);
    return within(contenedor).getByRole("combobox") as HTMLButtonElement;
}

function textoDe(etiqueta: string): string {
    return selectDe(etiqueta).textContent ?? "";
}

function valorDe(etiqueta: RegExp): string {
    return screen.getByLabelText<HTMLInputElement>(etiqueta).value;
}

async function renderizarFormulario() {
    render(<PaginaPublicar />);
    await waitFor(() => expect(listarSubprogramas).toHaveBeenCalled());
    await waitFor(() => expect(selectDe("Área responsable").disabled).toBe(false));
}

async function cargarArchivo(usuario: Usuario) {
    const entrada = screen.getByLabelText<HTMLInputElement>(
        "Seleccionar archivo del documento",
    );
    const archivo = new File(["contenido"], NOMBRE_ARCHIVO, {
        type: "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    });
    await usuario.upload(entrada, archivo);
}

async function elegirOpcion(usuario: Usuario, campo: string, opcion: string) {
    await usuario.click(selectDe(campo));
    const listado = await screen.findByRole("listbox");
    await usuario.click(within(listado).getByRole("option", { name: new RegExp(opcion, "i") }));
}

describe("Publicar documento — autocompletado de área y subproceso", () => {
    it("conserva el subproceso resuelto cuando el área se autocompleta desde el archivo", async () => {
        const usuario = userEvent.setup();
        await renderizarFormulario();

        await cargarArchivo(usuario);

        await waitFor(() => expect(textoDe("Área responsable")).toContain("Gestión Ambiental"));
        expect(textoDe("Subproceso")).toContain("Limpieza y Desinfección");
        expect(textoDe("Tipo de documento")).toContain("Programa");
        expect(valorDe(/^Código/)).toBe("PR-L&D-PG-01");
        expect(valorDe(/^Versión inicial/)).toBe("5");
    });

    it("limpia el subproceso cuando el usuario cambia manualmente de área", async () => {
        const usuario = userEvent.setup();
        await renderizarFormulario();
        await cargarArchivo(usuario);
        await waitFor(() => expect(textoDe("Subproceso")).toContain("Limpieza y Desinfección"));

        await elegirOpcion(usuario, "Área responsable", "Gestión de la Calidad");

        await waitFor(() =>
            expect(textoDe("Área responsable")).toContain("Gestión de la Calidad"),
        );
        expect(textoDe("Subproceso")).not.toContain("Limpieza y Desinfección");
        expect(textoDe("Subproceso")).toContain("Seleccionar");
    });

    it("mantiene el subproceso cuando se reemite el mismo área ya seleccionada", async () => {
        const usuario = userEvent.setup();
        await renderizarFormulario();
        await cargarArchivo(usuario);
        await waitFor(() => expect(textoDe("Subproceso")).toContain("Limpieza y Desinfección"));

        await elegirOpcion(usuario, "Área responsable", "Gestión Ambiental");

        expect(textoDe("Área responsable")).toContain("Gestión Ambiental");
        expect(textoDe("Subproceso")).toContain("Limpieza y Desinfección");
    });

    it("permite seleccionar manualmente un subproceso del área elegida", async () => {
        const usuario = userEvent.setup();
        await renderizarFormulario();

        await elegirOpcion(usuario, "Área responsable", "Gestión Ambiental");
        await elegirOpcion(usuario, "Subproceso", "Control de Residuos Sólidos");

        expect(textoDe("Subproceso")).toContain("Control de Residuos Sólidos");
    });

    it("encuentra un área por fragmento del nombre sin distinguir mayúsculas", async () => {
        const usuario = userEvent.setup();
        await renderizarFormulario();

        await usuario.click(selectDe("Área responsable"));
        await usuario.type(screen.getByPlaceholderText("Buscar área..."), "cal");
        const listado = await screen.findByRole("listbox");

        expect(within(listado).getByRole("option", { name: /Gestión de la Calidad/ })).toBeTruthy();
        expect(within(listado).queryByRole("option", { name: /Gestión Ambiental/ })).toBeNull();
    });

    it("encuentra un subproceso por código", async () => {
        const usuario = userEvent.setup();
        await renderizarFormulario();

        await elegirOpcion(usuario, "Área responsable", "Gestión Ambiental");
        await usuario.click(selectDe("Subproceso"));
        await usuario.type(screen.getByPlaceholderText("Buscar subproceso..."), "sst");
        expect(screen.getByText("No se encontraron subprocesos.")).toBeTruthy();

        await usuario.clear(screen.getByPlaceholderText("Buscar subproceso..."));
        await usuario.type(screen.getByPlaceholderText("Buscar subproceso..."), "l&d");
        const listado = await screen.findByRole("listbox");
        expect(within(listado).getByRole("option", { name: /Limpieza y Desinfección/ })).toBeTruthy();
    });

    it("restringe subprocesos al área elegida", async () => {
        const usuario = userEvent.setup();
        await renderizarFormulario();

        await elegirOpcion(usuario, "Área responsable", "Gestión de la Calidad");
        await usuario.click(selectDe("Subproceso"));
        const listado = await screen.findByRole("listbox");

        expect(within(listado).getByRole("option", { name: /Auditoría Interna/ })).toBeTruthy();
        expect(within(listado).queryByRole("option", { name: /Limpieza y Desinfección/ })).toBeNull();
    });

    it("muestra los tres tipos ODE y selecciona por id", async () => {
        const usuario = userEvent.setup();
        await renderizarFormulario();

        await usuario.click(selectDe("Tipo de documento"));
        await usuario.type(screen.getByPlaceholderText("Buscar tipo..."), "ode");
        const listado = await screen.findByRole("listbox");

        expect(within(listado).getByRole("option", { name: /Plantilla/ })).toBeTruthy();
        expect(within(listado).getByRole("option", { name: /Documentos Externos/ })).toBeTruthy();
        expect(within(listado).getByRole("option", { name: /Imágenes/ })).toBeTruthy();

        await usuario.click(within(listado).getByRole("option", { name: /Documentos Externos/ }));
        expect(textoDe("Tipo de documento")).toContain("Documentos Externos");
        expect(textoDe("Tipo de documento")).toContain("ODE");
    });
});

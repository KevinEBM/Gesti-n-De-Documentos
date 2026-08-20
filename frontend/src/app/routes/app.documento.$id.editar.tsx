import { Link, createFileRoute, useNavigate, useParams } from "@tanstack/react-router";
import { ArrowLeft, ChevronDown, Plus, Save, X } from "lucide-react";
import {
    useCallback,
    useEffect,
    useMemo,
    useState,
    type FormEvent,
    type ReactNode,
} from "react";
import { toast } from "sonner";

import { AppShell } from "@/components/AppShell";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import {
    Command,
    CommandEmpty,
    CommandGroup,
    CommandInput,
    CommandItem,
    CommandList,
} from "@/components/ui/command";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { ApiError } from "@/lib/api";
import { listarAreas, type AreaCatalogo } from "@/lib/areas-api";
import {
    SELECT_CONTENT_CLASS,
    SELECT_ITEM_CLASS,
    SELECT_TRIGGER_CLASS,
    etiquetasAlcance,
} from "@/lib/documentos-consulta-shared";
import {
    actualizarDocumento,
    obtenerDocumento,
    type DocumentoActualizacionRequestDto,
    type DocumentoAlcance,
    type DocumentoDetalle,
} from "@/lib/documentos-api";
import { obtenerIconoArea } from "@/lib/iconos-areas";
import { obtenerIconoFormato } from "@/lib/iconos-formatos";
import { obtenerIconoSubProceso } from "@/lib/iconos-subprocesos";
import { listarSubprogramas, type SubprogramaCatalogo } from "@/lib/subprogramas-api";
import { useIntranet } from "@/lib/store";
import { listarTiposDocumento, type TipoDocumentoCatalogo } from "@/lib/tipos-documento-api";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/app/documento/$id/editar")({
    head: () => ({
        meta: [{ title: "Editar publicación — Intranet documental" }],
    }),
    component: EditarDocumentoPage,
});

const POPOVER_CONTENT_CLASS =
    "w-[var(--radix-popover-trigger-width)] p-0 !bg-white !text-slate-900 border border-slate-200 shadow-2xl z-[99999]";

const COMMAND_CLASS = "!bg-white !text-slate-900";

const COMMAND_ITEM_CLASS =
    "!text-slate-900 data-[selected=true]:!bg-slate-100 data-[selected=true]:!text-slate-900";

const ayudaAlcance: Record<DocumentoAlcance, string> = {
    AREA_RESPONSABLE: "Visible para usuarios asociados al área responsable.",
    AREAS_ESPECIFICAS: "Visible para el área responsable y las áreas adicionales seleccionadas.",
    GLOBAL: "Visible para todos los usuarios autorizados del sistema.",
};

interface FormularioEdicion {
    codigo: string;
    titulo: string;
    descripcion: string;
    areaId: string;
    subprogramaId: string;
    tipoDocumentoId: string;
    alcance: DocumentoAlcance | "";
    areasAdicionalesIds: string[];
}

function incluirAreaCatalogo(areas: AreaCatalogo[], id: string, nombre: string): AreaCatalogo[] {
    if (!id || areas.some((area) => area.id === id)) return areas;
    return [...areas, { id, nombre, descripcion: "", activo: false }];
}

function incluirSubprogramaCatalogo(
    subprogramas: SubprogramaCatalogo[],
    id: string,
    nombre: string,
    areaId: string,
): SubprogramaCatalogo[] {
    if (!id || subprogramas.some((item) => item.id === id)) return subprogramas;
    return [...subprogramas, { id, nombre, descripcion: "", areaId, activo: false }];
}

function incluirTipoCatalogo(tipos: TipoDocumentoCatalogo[], id: string, nombre: string): TipoDocumentoCatalogo[] {
    if (!id || tipos.some((tipo) => tipo.id === id)) return tipos;
    return [...tipos, { id, nombre, descripcion: "", activo: false }];
}

function validarFormulario(
    form: FormularioEdicion,
    areasActivas: AreaCatalogo[],
    subprogramas: SubprogramaCatalogo[],
    tipos: TipoDocumentoCatalogo[],
): Record<string, string> {
    const errores: Record<string, string> = {};

    const codigo = form.codigo.trim();
    if (!codigo) errores.codigo = "El código documental es obligatorio.";
    else if (codigo.length > 50) errores.codigo = "El código no puede superar los 50 caracteres.";

    const titulo = form.titulo.trim();
    if (!titulo) errores.titulo = "El título es obligatorio.";
    else if (titulo.length > 200) errores.titulo = "El título no puede superar los 200 caracteres.";

    if (form.descripcion.trim().length > 500) {
        errores.descripcion = "La descripción no puede superar los 500 caracteres.";
    }

    if (!form.areaId) errores.areaId = "Selecciona el área responsable.";
    else if (!areasActivas.some((area) => area.id === form.areaId)) {
        errores.areaId = "Selecciona un área válida.";
    }

    if (!form.areaId) {
        errores.subprogramaId = "Selecciona primero un área responsable.";
    } else if (!form.subprogramaId) {
        errores.subprogramaId = "Selecciona un subprograma.";
    } else {
        const subprograma = subprogramas.find((item) => item.id === form.subprogramaId);
        if (!subprograma || subprograma.areaId !== form.areaId) {
            errores.subprogramaId = "Selecciona un subprograma del área elegida.";
        }
    }

    if (!form.tipoDocumentoId) errores.tipoDocumentoId = "Selecciona un tipo de documento.";
    else if (!tipos.some((tipo) => tipo.id === form.tipoDocumentoId)) {
        errores.tipoDocumentoId = "Selecciona un tipo válido.";
    }

    if (!form.alcance) {
        errores.alcance = "Selecciona el alcance del documento.";
    } else if (form.alcance === "AREAS_ESPECIFICAS") {
        if (form.areasAdicionalesIds.length === 0) {
            errores.areasAdicionalesIds = "Selecciona al menos un área adicional.";
        } else if (form.areasAdicionalesIds.includes(form.areaId)) {
            errores.areasAdicionalesIds = "El área responsable no puede repetirse como adicional.";
        }
    }

    return errores;
}

function EditarDocumentoPage() {
    const { id } = useParams({ from: "/app/documento/$id/editar" });
    const navigate = useNavigate();
    const { permisos } = useIntranet();

    const [documento, setDocumento] = useState<DocumentoDetalle | null>(null);
    const [form, setForm] = useState<FormularioEdicion | null>(null);
    const [areasCatalogo, setAreasCatalogo] = useState<AreaCatalogo[]>([]);
    const [subprogramasCatalogo, setSubprogramasCatalogo] = useState<SubprogramaCatalogo[]>([]);
    const [tiposCatalogo, setTiposCatalogo] = useState<TipoDocumentoCatalogo[]>([]);
    const [cargando, setCargando] = useState(true);
    const [noDisponible, setNoDisponible] = useState(false);
    const [errorCarga, setErrorCarga] = useState<string | null>(null);
    const [errores, setErrores] = useState<Record<string, string>>({});
    const [guardando, setGuardando] = useState(false);
    const [buscadorAreaAdicionalVisible, setBuscadorAreaAdicionalVisible] = useState(false);

    const cargarDatos = useCallback(async () => {
        setCargando(true);
        setNoDisponible(false);
        setErrorCarga(null);
        try {
            const [detalle, areas, subprogramas, tipos] = await Promise.all([
                obtenerDocumento(id),
                listarAreas(),
                listarSubprogramas(),
                listarTiposDocumento(),
            ]);

            setDocumento(detalle);
            setAreasCatalogo(areas);
            setSubprogramasCatalogo(subprogramas);
            setTiposCatalogo(tipos);
            setForm({
                codigo: detalle.codigo,
                titulo: detalle.titulo,
                descripcion: detalle.descripcion ?? "",
                areaId: detalle.areaId,
                subprogramaId: detalle.subprogramaId,
                tipoDocumentoId: detalle.tipoDocumentoId,
                alcance: detalle.alcance,
                areasAdicionalesIds: detalle.areasAdicionales.map((area) => area.id),
            });
        } catch (err) {
            if (err instanceof ApiError && (err.status === 404 || err.status === 403)) {
                setNoDisponible(true);
                return;
            }
            setErrorCarga(
                err instanceof ApiError ? err.message : "No fue posible cargar el documento.",
            );
        } finally {
            setCargando(false);
        }
    }, [id]);

    useEffect(() => {
        if (!permisos.actualizarDocumentos) return;
        cargarDatos();
    }, [cargarDatos, permisos.actualizarDocumentos]);

    const areasActivas = useMemo(
        () =>
            documento
                ? incluirAreaCatalogo(
                      areasCatalogo.filter((area) => area.activo),
                      documento.areaId,
                      documento.areaNombre,
                  )
                : areasCatalogo.filter((area) => area.activo),
        [areasCatalogo, documento],
    );

    const subprogramasArea = useMemo(() => {
        if (!form?.areaId) return [];
        let opciones = subprogramasCatalogo.filter(
            (item) => item.activo && item.areaId === form.areaId,
        );
        if (documento && form.areaId === documento.areaId) {
            opciones = incluirSubprogramaCatalogo(
                opciones,
                documento.subprogramaId,
                documento.subprogramaNombre,
                documento.areaId,
            );
        }
        return opciones.filter((item) => item.areaId === form.areaId);
    }, [subprogramasCatalogo, form?.areaId, documento]);

    const tiposDisponibles = useMemo(
        () =>
            documento
                ? incluirTipoCatalogo(
                      tiposCatalogo.filter((tipo) => tipo.activo),
                      documento.tipoDocumentoId,
                      documento.tipoDocumentoNombre,
                  )
                : tiposCatalogo.filter((tipo) => tipo.activo),
        [tiposCatalogo, documento],
    );

    const areasSeleccionablesAdicionales = useMemo(() => {
        if (!form) return [];
        return areasActivas.filter(
            (area) =>
                area.id !== form.areaId && !form.areasAdicionalesIds.includes(area.id),
        );
    }, [areasActivas, form]);

    const areasAdicionalesSeleccionadas = useMemo(() => {
        if (!form) return [];
        return form.areasAdicionalesIds
            .map((areaId) => areasActivas.find((area) => area.id === areaId))
            .filter((area): area is AreaCatalogo => !!area);
    }, [form, areasActivas]);

    const cambiarArea = (areaId: string) => {
        setForm((prev) =>
            prev
                ? {
                      ...prev,
                      areaId,
                      subprogramaId: "",
                      areasAdicionalesIds: prev.areasAdicionalesIds.filter((idArea) => idArea !== areaId),
                  }
                : prev,
        );
    };

    const cambiarAlcance = (alcance: DocumentoAlcance) => {
        setForm((prev) =>
            prev
                ? {
                      ...prev,
                      alcance,
                      areasAdicionalesIds:
                          alcance === "AREAS_ESPECIFICAS" ? prev.areasAdicionalesIds : [],
                  }
                : prev,
        );
        if (alcance === "AREAS_ESPECIFICAS") {
            setBuscadorAreaAdicionalVisible(form?.areasAdicionalesIds.length === 0);
        } else {
            setBuscadorAreaAdicionalVisible(false);
        }
    };

    const agregarAreaAdicional = (areaId: string) => {
        setForm((prev) => {
            if (!prev || prev.areaId === areaId || prev.areasAdicionalesIds.includes(areaId)) {
                return prev;
            }
            return { ...prev, areasAdicionalesIds: [...prev.areasAdicionalesIds, areaId] };
        });
        setBuscadorAreaAdicionalVisible(false);
    };

    const quitarAreaAdicional = (areaId: string) => {
        setForm((prev) => {
            if (!prev) return prev;
            const areasAdicionalesIds = prev.areasAdicionalesIds.filter((idArea) => idArea !== areaId);
            if (areasAdicionalesIds.length === 0) setBuscadorAreaAdicionalVisible(true);
            return { ...prev, areasAdicionalesIds };
        });
    };

    const construirPayload = (): DocumentoActualizacionRequestDto => {
        if (!form) throw new Error("Formulario no inicializado");
        const alcance = form.alcance as DocumentoAlcance;
        return {
            codigo: form.codigo.trim(),
            titulo: form.titulo.trim(),
            descripcion: form.descripcion.trim() || null,
            areaId: Number(form.areaId),
            subprogramaId: Number(form.subprogramaId),
            tipoDocumentoId: Number(form.tipoDocumentoId),
            alcance,
            areasAdicionalesIds:
                alcance === "AREAS_ESPECIFICAS"
                    ? form.areasAdicionalesIds.map(Number)
                    : [],
        };
    };

    const enviar = async (event: FormEvent) => {
        event.preventDefault();
        if (!form) return;

        const erroresValidacion = validarFormulario(
            form,
            areasActivas,
            subprogramasCatalogo,
            tiposDisponibles,
        );
        setErrores(erroresValidacion);
        if (Object.keys(erroresValidacion).length > 0) return;

        setGuardando(true);
        try {
            await actualizarDocumento(id, construirPayload());
            toast.success("Documento actualizado correctamente.");
            navigate({ to: "/app/documento/$id", params: { id } });
        } catch (err) {
            if (err instanceof ApiError) {
                if (err.status === 409) {
                    setErrores((prev) => ({
                        ...prev,
                        codigo: err.message || "Ya existe un documento con este código.",
                    }));
                }
                toast.error(err.message);
            } else {
                toast.error("No fue posible actualizar el documento.");
            }
        } finally {
            setGuardando(false);
        }
    };

    if (!permisos.actualizarDocumentos) {
        return (
            <AppShell titulo="Editar publicación">
                <Card>
                    <CardContent className="py-14 text-center text-sm text-muted-foreground">
                        No cuentas con permisos para editar documentos.
                    </CardContent>
                </Card>
            </AppShell>
        );
    }

    if (cargando) {
        return (
            <AppShell titulo="Editar publicación">
                <Card>
                    <CardContent className="py-14 text-center text-sm text-muted-foreground">
                        Cargando documento...
                    </CardContent>
                </Card>
            </AppShell>
        );
    }

    if (noDisponible) {
        return (
            <AppShell titulo="Documento no disponible">
                <Card>
                    <CardContent className="py-14 text-center">
                        <p className="text-sm font-medium">Documento no disponible</p>
                        <p className="mt-2 text-sm text-muted-foreground">
                            El documento no existe o no está disponible para tu usuario.
                        </p>
                    </CardContent>
                </Card>
            </AppShell>
        );
    }

    if (errorCarga || !documento || !form) {
        return (
            <AppShell titulo="Editar publicación">
                <Card>
                    <CardContent className="space-y-3 py-14 text-center">
                        <p className="text-sm font-medium">{errorCarga ?? "Error inesperado."}</p>
                        <Button variant="outline" size="sm" onClick={cargarDatos}>
                            Reintentar
                        </Button>
                    </CardContent>
                </Card>
            </AppShell>
        );
    }

    if (documento.estado === "OBSOLETO") {
        return (
            <AppShell titulo="Editar publicación">
                <Button asChild variant="ghost" size="sm" className="-ml-2 mb-4 gap-1.5">
                    <Link to="/app/documento/$id" params={{ id }}>
                        <ArrowLeft className="size-4" /> Volver al detalle
                    </Link>
                </Button>
                <Card>
                    <CardContent className="py-8 text-center">
                        <p className="text-sm font-medium text-amber-800">
                            Este documento está obsoleto y no puede editarse.
                        </p>
                        <p className="mt-2 text-sm text-muted-foreground">
                            Actívalo nuevamente desde Gestión de documentos para poder modificarlo.
                        </p>
                        <Button asChild variant="outline" className="mt-4">
                            <Link to="/app/gestion-documentos">Ir a Gestión de documentos</Link>
                        </Button>
                    </CardContent>
                </Card>
            </AppShell>
        );
    }

    return (
        <AppShell
            titulo="Editar publicación"
            descripcion={`Código: ${documento.codigo} · Versión vigente: ${documento.numeroVersionActual}`}
        >
            <Button asChild variant="ghost" size="sm" className="-ml-2 mb-4 gap-1.5">
                <Link to="/app/documento/$id" params={{ id }}>
                    <ArrowLeft className="size-4" /> Volver al detalle
                </Link>
            </Button>

            <form onSubmit={enviar} className="space-y-4">
                <Card>
                    <CardHeader>
                        <CardTitle>Metadatos del documento</CardTitle>
                        <CardDescription>
                            Modifica código, título, descripción, clasificación y alcance. El archivo
                            vigente no cambia.
                        </CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-5">
                        <Campo label="Código documental" obligatorio error={errores.codigo}>
                            <Input
                                value={form.codigo}
                                onChange={(e) =>
                                    setForm((prev) =>
                                        prev ? { ...prev, codigo: e.target.value } : prev,
                                    )
                                }
                                disabled={guardando}
                                className="font-mono text-slate-900"
                            />
                        </Campo>

                        <Campo label="Título" obligatorio error={errores.titulo}>
                            <Input
                                value={form.titulo}
                                onChange={(e) =>
                                    setForm((prev) =>
                                        prev ? { ...prev, titulo: e.target.value } : prev,
                                    )
                                }
                                disabled={guardando}
                            />
                        </Campo>

                        <Campo label="Descripción" error={errores.descripcion}>
                            <Textarea
                                rows={3}
                                value={form.descripcion}
                                onChange={(e) =>
                                    setForm((prev) =>
                                        prev ? { ...prev, descripcion: e.target.value } : prev,
                                    )
                                }
                                disabled={guardando}
                            />
                        </Campo>

                        <div className="grid gap-4 md:grid-cols-2">
                            <Campo label="Área responsable" obligatorio error={errores.areaId}>
                                <Select
                                    value={form.areaId}
                                    onValueChange={cambiarArea}
                                    disabled={guardando}
                                >
                                    <SelectTrigger className={SELECT_TRIGGER_CLASS}>
                                        <SelectValue placeholder="Seleccionar área…" />
                                    </SelectTrigger>
                                    <SelectContent className={SELECT_CONTENT_CLASS}>
                                        {areasActivas.map((area) => {
                                            const { icono: Icono, color } = obtenerIconoArea(area.nombre);
                                            return (
                                                <SelectItem
                                                    key={area.id}
                                                    value={area.id}
                                                    className={SELECT_ITEM_CLASS}
                                                >
                                                    <div className="flex items-center gap-2">
                                                        <Icono className={`size-4 ${color}`} />
                                                        <span>{area.nombre}</span>
                                                    </div>
                                                </SelectItem>
                                            );
                                        })}
                                    </SelectContent>
                                </Select>
                            </Campo>

                            <Campo label="Subprograma" obligatorio error={errores.subprogramaId}>
                                <Select
                                    value={form.subprogramaId}
                                    onValueChange={(subprogramaId) =>
                                        setForm((prev) => (prev ? { ...prev, subprogramaId } : prev))
                                    }
                                    disabled={guardando || !form.areaId}
                                >
                                    <SelectTrigger className={SELECT_TRIGGER_CLASS}>
                                        <SelectValue
                                            placeholder={
                                                form.areaId
                                                    ? "Seleccionar subprograma…"
                                                    : "Seleccione primero un área"
                                            }
                                        />
                                    </SelectTrigger>
                                    <SelectContent className={SELECT_CONTENT_CLASS}>
                                        {subprogramasArea.map((item) => {
                                            const { icono: Icono, color } = obtenerIconoSubProceso(
                                                item.nombre,
                                            );
                                            return (
                                                <SelectItem
                                                    key={item.id}
                                                    value={item.id}
                                                    className={SELECT_ITEM_CLASS}
                                                >
                                                    <div className="flex items-center gap-2">
                                                        <Icono className={`size-4 ${color}`} />
                                                        <span>{item.nombre}</span>
                                                    </div>
                                                </SelectItem>
                                            );
                                        })}
                                    </SelectContent>
                                </Select>
                            </Campo>

                            <Campo label="Tipo de documento" obligatorio error={errores.tipoDocumentoId}>
                                <Select
                                    value={form.tipoDocumentoId}
                                    onValueChange={(tipoDocumentoId) =>
                                        setForm((prev) => (prev ? { ...prev, tipoDocumentoId } : prev))
                                    }
                                    disabled={guardando}
                                >
                                    <SelectTrigger className={SELECT_TRIGGER_CLASS}>
                                        <SelectValue placeholder="Seleccionar tipo…" />
                                    </SelectTrigger>
                                    <SelectContent className={SELECT_CONTENT_CLASS}>
                                        {tiposDisponibles.map((tipo) => {
                                            const { icono: Icono, color } = obtenerIconoFormato(tipo.nombre);
                                            return (
                                                <SelectItem
                                                    key={tipo.id}
                                                    value={tipo.id}
                                                    className={SELECT_ITEM_CLASS}
                                                >
                                                    <div className="flex items-center gap-2">
                                                        <Icono className={`size-4 ${color}`} />
                                                        <span>{tipo.nombre}</span>
                                                    </div>
                                                </SelectItem>
                                            );
                                        })}
                                    </SelectContent>
                                </Select>
                            </Campo>

                            <Campo label="Alcance" obligatorio error={errores.alcance}>
                                <Select
                                    value={form.alcance}
                                    onValueChange={(value) => cambiarAlcance(value as DocumentoAlcance)}
                                    disabled={guardando}
                                >
                                    <SelectTrigger className={SELECT_TRIGGER_CLASS}>
                                        <SelectValue placeholder="Seleccionar alcance…" />
                                    </SelectTrigger>
                                    <SelectContent className={SELECT_CONTENT_CLASS}>
                                        {(Object.keys(etiquetasAlcance) as DocumentoAlcance[]).map(
                                            (alcance) => (
                                                <SelectItem
                                                    key={alcance}
                                                    value={alcance}
                                                    className={SELECT_ITEM_CLASS}
                                                >
                                                    {etiquetasAlcance[alcance]}
                                                </SelectItem>
                                            ),
                                        )}
                                    </SelectContent>
                                </Select>
                                {form.alcance ? (
                                    <p className="text-xs text-muted-foreground">
                                        {ayudaAlcance[form.alcance]}
                                    </p>
                                ) : null}
                            </Campo>
                        </div>

                        {form.alcance === "AREAS_ESPECIFICAS" ? (
                            <div className="space-y-3 rounded-md border p-4">
                                <Label>
                                    Áreas adicionales <span className="text-destructive">*</span>
                                </Label>
                                {buscadorAreaAdicionalVisible && areasSeleccionablesAdicionales.length > 0 ? (
                                    <BuscadorAreaAdicional
                                        areas={areasSeleccionablesAdicionales}
                                        onSeleccionar={agregarAreaAdicional}
                                        disabled={guardando}
                                    />
                                ) : null}
                                {areasAdicionalesSeleccionadas.length > 0 ? (
                                    <div className="flex flex-wrap gap-2">
                                        {areasAdicionalesSeleccionadas.map((area) => {
                                            const { icono: Icono, color } = obtenerIconoArea(area.nombre);
                                            return (
                                                <div
                                                    key={area.id}
                                                    className="flex items-center gap-2 rounded-md border border-emerald-500/30 bg-emerald-500/5 px-3 py-1.5"
                                                >
                                                    <Icono className={`size-4 ${color}`} />
                                                    <span className="text-sm font-medium">{area.nombre}</span>
                                                    <button
                                                        type="button"
                                                        onClick={() => quitarAreaAdicional(area.id)}
                                                        disabled={guardando}
                                                        className="rounded-sm p-0.5 text-muted-foreground hover:bg-muted"
                                                        aria-label={`Quitar ${area.nombre}`}
                                                    >
                                                        <X className="size-3.5" />
                                                    </button>
                                                </div>
                                            );
                                        })}
                                    </div>
                                ) : null}
                                {areasSeleccionablesAdicionales.length > 0 ? (
                                    <Button
                                        type="button"
                                        variant="outline"
                                        size="sm"
                                        className="gap-1.5"
                                        disabled={guardando}
                                        onClick={() => setBuscadorAreaAdicionalVisible(true)}
                                    >
                                        <Plus className="size-4" /> Agregar otra área
                                    </Button>
                                ) : null}
                                {errores.areasAdicionalesIds ? (
                                    <p className="text-sm text-destructive">{errores.areasAdicionalesIds}</p>
                                ) : null}
                            </div>
                        ) : null}

                        <div className="flex justify-end gap-3 border-t pt-4">
                            <Button asChild variant="outline" type="button" disabled={guardando}>
                                <Link to="/app/documento/$id" params={{ id }}>
                                    Cancelar
                                </Link>
                            </Button>
                            <Button type="submit" disabled={guardando} className="gap-2">
                                <Save className="size-4" />
                                {guardando ? "Guardando..." : "Guardar cambios"}
                            </Button>
                        </div>
                    </CardContent>
                </Card>
            </form>
        </AppShell>
    );
}

function Campo({
    label,
    obligatorio = false,
    error,
    children,
}: {
    label: string;
    obligatorio?: boolean;
    error?: string;
    children: ReactNode;
}) {
    return (
        <div className="space-y-1.5">
            <Label>
                {label}
                {obligatorio ? <span className="text-destructive"> *</span> : null}
            </Label>
            {children}
            {error ? <p className="text-sm text-destructive">{error}</p> : null}
        </div>
    );
}

function BuscadorAreaAdicional({
    areas,
    onSeleccionar,
    disabled = false,
}: {
    areas: AreaCatalogo[];
    onSeleccionar: (areaId: string) => void;
    disabled?: boolean;
}) {
    const [abierto, setAbierto] = useState(false);

    return (
        <Popover open={abierto} onOpenChange={setAbierto}>
            <PopoverTrigger asChild>
                <Button
                    type="button"
                    variant="outline"
                    className="w-full justify-between font-normal !bg-white !text-slate-900"
                    disabled={disabled || areas.length === 0}
                >
                    Buscar y seleccionar área…
                    <ChevronDown className="ml-2 size-4 shrink-0 opacity-50" />
                </Button>
            </PopoverTrigger>
            <PopoverContent align="start" className={POPOVER_CONTENT_CLASS}>
                <Command className={COMMAND_CLASS}>
                    <CommandInput placeholder="Buscar área…" className="!text-slate-900" />
                    <CommandList>
                        <CommandEmpty>No se encontraron áreas.</CommandEmpty>
                        <CommandGroup>
                            {areas.map((area) => {
                                const { icono: Icono, color } = obtenerIconoArea(area.nombre);
                                return (
                                    <CommandItem
                                        key={area.id}
                                        value={area.nombre}
                                        className={COMMAND_ITEM_CLASS}
                                        onSelect={() => {
                                            onSeleccionar(area.id);
                                            setAbierto(false);
                                        }}
                                    >
                                        <Icono className={`size-4 ${color}`} />
                                        <span>{area.nombre}</span>
                                    </CommandItem>
                                );
                            })}
                        </CommandGroup>
                    </CommandList>
                </Command>
            </PopoverContent>
        </Popover>
    );
}

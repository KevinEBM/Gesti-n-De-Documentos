import { createFileRoute } from "@tanstack/react-router";
import type { ComponentType } from "react";
import { Plus } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { toast } from "sonner";

import { AppShell } from "@/components/AppShell";
import { ActivoBadge } from "@/components/EstadoBadge";
import { Button } from "@/components/ui/button";
import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { SelectorAreaResponsable } from "@/components/selector-area-responsable";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table";
import {
    Tabs,
    TabsContent,
    TabsList,
    TabsTrigger,
} from "@/components/ui/tabs";
import { Textarea } from "@/components/ui/textarea";
import {
    actualizarArea,
    cambiarEstadoArea,
    crearArea,
    listarAreas,
    type AreaCatalogo,
} from "@/lib/areas-api";
import { ApiError } from "@/lib/api";
import {
    listarSubprogramas,
    crearSubprograma,
    actualizarSubprograma,
    cambiarEstadoSubprograma,
    type SubprogramaCatalogo,
} from "@/lib/subprogramas-api";
import {
    listarTiposDocumento,
    crearTipoDocumento,
    actualizarTipoDocumento,
    cambiarEstadoTipoDocumento,
    type TipoDocumentoCatalogo,
} from "@/lib/tipos-documento-api";
import { useIntranet } from "@/lib/store";

import { obtenerIconoArea } from "@/lib/iconos-areas";
import { obtenerIconoFormato } from "@/lib/iconos-formatos";
import { obtenerIconoSubProceso } from "@/lib/iconos-subprocesos";

export const Route = createFileRoute("/app/parametrizacion")({
    head: () => ({
        meta: [
            { title: "Parametrización — Intranet documental" },
            {
                name: "description",
                content:
                    "Administra áreas, subprocesos y tipos de documento de la intranet corporativa.",
            },
            {
                property: "og:title",
                content: "Parametrización — Intranet documental",
            },
            {
                property: "og:description",
                content:
                    "Áreas, subprocesos y tipos de documento configurables.",
            },
        ],
    }),
    component: Parametrizacion,
});

function etiquetaConteoTab(total: number | null): string {
    return total === null ? "…" : String(total);
}

function Parametrizacion() {
    const { permisos } = useIntranet();
    const [totalAreas, setTotalAreas] = useState<number | null>(null);
    const [totalSubprogramas, setTotalSubprogramas] = useState<number | null>(null);
    const [totalTiposDocumento, setTotalTiposDocumento] = useState<number | null>(null);

    if (!permisos.gestionarParametros) {
        return (
            <AppShell titulo="Parametrización">
                <Card>
                    <CardContent className="py-14 text-center text-sm text-muted-foreground">
                        No cuentas con permisos para acceder a la parametrización.
                    </CardContent>
                </Card>
            </AppShell>
        );
    }

    return (
        <AppShell
            titulo="Parametrización"
            descripcion="Áreas, subprocesos y tipos de documento"
        >
            <Tabs defaultValue="areas" className="space-y-4">
                <TabsList>
                    <TabsTrigger
                        value="areas"
                        className="flex items-center gap-2 border border-input bg-background text-black shadow-sm hover:bg-[#289248] hover:text-white hover:border-[#289248] hover:shadow data-[state=active]:bg-[#289248] data-[state=active]:text-white"
                    >
                        Áreas ({etiquetaConteoTab(totalAreas)})
                    </TabsTrigger>

                    <TabsTrigger
                        value="subprogramas"
                        className="flex items-center gap-2 border border-input bg-background text-black shadow-sm hover:bg-[#289248] hover:text-white hover:border-[#289248] hover:shadow data-[state=active]:bg-[#289248] data-[state=active]:text-white"
                    >
                        Subprocesos ({etiquetaConteoTab(totalSubprogramas)})
                    </TabsTrigger>

                    <TabsTrigger
                        value="tipos"
                        className="flex items-center gap-2 border border-input bg-background text-black shadow-sm hover:bg-[#289248] hover:text-white hover:border-[#289248] hover:shadow data-[state=active]:bg-[#289248] data-[state=active]:text-white"
                    >
                        Tipos de documento ({etiquetaConteoTab(totalTiposDocumento)})
                    </TabsTrigger>
                </TabsList>

                <TabsContent
                    value="areas"
                    forceMount
                    className="data-[state=inactive]:hidden"
                >
                    <SeccionAreas onTotalChange={setTotalAreas} />
                </TabsContent>

                <TabsContent
                    value="subprogramas"
                    forceMount
                    className="data-[state=inactive]:hidden"
                >
                    <SeccionSubprogramas onTotalChange={setTotalSubprogramas} />
                </TabsContent>

                <TabsContent
                    value="tipos"
                    forceMount
                    className="data-[state=inactive]:hidden"
                >
                    <SeccionTiposDocumento
                        onTotalChange={setTotalTiposDocumento}
                    />
                </TabsContent>
            </Tabs>
        </AppShell>
    );
}

function SeccionAreas({ onTotalChange }: { onTotalChange?: (total: number) => void }) {
    const [areasCatalogo, setAreasCatalogo] = useState<AreaCatalogo[]>([]);
    const [cargando, setCargando] = useState(true);
    const [errorCarga, setErrorCarga] = useState<string | null>(null);
    const [guardando, setGuardando] = useState(false);
    const [alternandoId, setAlternandoId] = useState<string | null>(null);
    const [abierto, setAbierto] = useState(false);
    const [form, setForm] = useState<{
        id?: string;
        codigo: string;
        nombre: string;
        descripcion: string;
    }>({
        codigo: "",
        nombre: "",
        descripcion: "",
    });
    const [errorForm, setErrorForm] = useState("");
    const [erroresCampo, setErroresCampo] = useState<
        Partial<Record<"codigo" | "nombre" | "descripcion", string>>
    >({});

    const notificarTotal = (lista: AreaCatalogo[]) => {
        onTotalChange?.(lista.length);
    };

    const cargar = async () => {
        setCargando(true);
        setErrorCarga(null);
        try {
            const resultado = await listarAreas();
            setAreasCatalogo(resultado);
            notificarTotal(resultado);
        } catch (err) {
            const mensaje =
                err instanceof ApiError
                    ? err.message
                    : "No fue posible cargar las áreas.";
            setErrorCarga(mensaje);
        } finally {
            setCargando(false);
        }
    };

    useEffect(() => {
        void cargar();
    }, []);

    const abrirNuevo = () => {
        setForm({ codigo: "", nombre: "", descripcion: "" });
        setErrorForm("");
        setErroresCampo({});
        setAbierto(true);
    };

    const abrirEditar = (area: AreaCatalogo) => {
        setForm({
            id: area.id,
            codigo: area.codigo,
            nombre: area.nombre,
            descripcion: area.descripcion,
        });
        setErrorForm("");
        setErroresCampo({});
        setAbierto(true);
    };

    const guardar = async () => {
        setErrorForm("");
        setErroresCampo({});

        const codigo = form.codigo.trim();
        const nombre = form.nombre.trim();
        const descripcion = form.descripcion.trim();

        if (!codigo) {
            setErrorForm("El código es obligatorio.");
            return;
        }
        if (codigo.length > 20) {
            setErrorForm("El código no puede superar los 20 caracteres.");
            return;
        }
        if (!nombre) {
            setErrorForm("El nombre es obligatorio.");
            return;
        }
        if (nombre.length > 100) {
            setErrorForm("El nombre no puede superar los 100 caracteres.");
            return;
        }
        if (descripcion.length > 255) {
            setErrorForm("La descripción no puede superar los 255 caracteres.");
            return;
        }

        const body = { codigo, nombre, descripcion };
        setGuardando(true);
        try {
            if (form.id) {
                const actualizada = await actualizarArea(form.id, body);
                setAreasCatalogo((prev) => {
                    const next = prev.map((a) =>
                        a.id === actualizada.id ? actualizada : a,
                    );
                    notificarTotal(next);
                    return next;
                });
                toast.success("Registro actualizado");
            } else {
                const creada = await crearArea(body);
                setAreasCatalogo((prev) => {
                    const next = [...prev, creada];
                    notificarTotal(next);
                    return next;
                });
                toast.success("Registro creado");
            }
            setAbierto(false);
        } catch (err) {
            if (err instanceof ApiError) {
                if (err.errores) {
                    setErroresCampo({
                        codigo: err.errores.codigo,
                        nombre: err.errores.nombre,
                        descripcion: err.errores.descripcion,
                    });
                }
                setErrorForm(err.message);
            } else {
                setErrorForm("No fue posible guardar el área.");
            }
        } finally {
            setGuardando(false);
        }
    };

    const alternarEstado = async (area: AreaCatalogo) => {
        setAlternandoId(area.id);
        try {
            const actualizada = await cambiarEstadoArea(area.id, !area.activo);
            setAreasCatalogo((prev) =>
                prev.map((a) => (a.id === actualizada.id ? actualizada : a)),
            );
            toast.success(
                actualizada.activo ? "Registro activado" : "Registro desactivado",
            );
        } catch (err) {
            const mensaje =
                err instanceof ApiError
                    ? err.message
                    : "No fue posible cambiar el estado del área.";
            toast.error(mensaje);
        } finally {
            setAlternandoId(null);
        }
    };

    return (
        <Card>
            <CardHeader className="flex-row items-start justify-between space-y-0">
                <div>
                    <CardTitle className="text-base">Áreas</CardTitle>
                    <CardDescription>
                        Unidades organizacionales responsables de los documentos.
                    </CardDescription>
                </div>

                <Button
                    size="sm"
                    className="gap-1.5"
                    onClick={abrirNuevo}
                    disabled={cargando || !!errorCarga}
                >
                    <Plus className="size-4" />
                    Nuevo
                </Button>
            </CardHeader>

            {cargando ? (
                <CardContent className="py-14 text-center text-sm text-muted-foreground">
                    Cargando áreas...
                </CardContent>
            ) : errorCarga ? (
                <CardContent className="space-y-4 py-14 text-center">
                    <p className="text-sm text-muted-foreground">{errorCarga}</p>
                    <Button variant="outline" onClick={() => void cargar()}>
                        Reintentar
                    </Button>
                </CardContent>
            ) : (
                <CardContent className="px-0 pb-0">
                    <Table>
                        <TableHeader>
                            <TableRow className="bg-secondary/60">
                                <TableHead>Nombre</TableHead>
                                <TableHead>Código</TableHead>
                                <TableHead>Descripción</TableHead>
                                <TableHead>Estado</TableHead>
                                <TableHead className="text-right">Acciones</TableHead>
                            </TableRow>
                        </TableHeader>

                        <TableBody>
                            {areasCatalogo.map((r) => {
                                const { icono: Icono, color } = obtenerIconoArea(r.nombre);

                                return (
                                    <TableRow key={r.id}>
                                        <TableCell className="font-medium">
                                            <div className="flex items-center gap-2">
                                                <Icono className={`size-4 ${color}`} />
                                                <span>{r.nombre}</span>
                                            </div>
                                        </TableCell>

                                        <TableCell className="text-sm text-muted-foreground">
                                            {r.codigo}
                                        </TableCell>

                                        <TableCell className="text-sm text-muted-foreground">
                                            {r.descripcion}
                                        </TableCell>

                                        <TableCell>
                                            <ActivoBadge activo={r.activo} />
                                        </TableCell>

                                        <TableCell>
                                            <div className="flex justify-end gap-2">
                                                <Button
                                                    size="sm"
                                                    variant="outline"
                                                    onClick={() => abrirEditar(r)}
                                                    disabled={alternandoId === r.id}
                                                >
                                                    Editar
                                                </Button>

                                                <Button
                                                    size="sm"
                                                    variant="ghost"
                                                    disabled={alternandoId === r.id}
                                                    onClick={() => void alternarEstado(r)}
                                                >
                                                    {r.activo ? "Desactivar" : "Activar"}
                                                </Button>
                                            </div>
                                        </TableCell>
                                    </TableRow>
                                );
                            })}
                        </TableBody>
                    </Table>
                </CardContent>
            )}

            <Dialog open={abierto} onOpenChange={setAbierto}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>
                            {form.id ? "Editar registro" : "Nuevo registro"}
                        </DialogTitle>
                        <DialogDescription>Áreas</DialogDescription>
                    </DialogHeader>

                    <div className="space-y-4">
                        <div className="space-y-1.5">
                            <Label>Código</Label>
                            <Input
                                value={form.codigo}
                                maxLength={20}
                                onChange={(e) =>
                                    setForm({ ...form, codigo: e.target.value })
                                }
                            />
                            {erroresCampo.codigo && (
                                <p className="text-xs text-destructive">
                                    {erroresCampo.codigo}
                                </p>
                            )}
                        </div>

                        <div className="space-y-1.5">
                            <Label>Nombre</Label>
                            <Input
                                value={form.nombre}
                                maxLength={100}
                                onChange={(e) =>
                                    setForm({ ...form, nombre: e.target.value })
                                }
                            />
                            {erroresCampo.nombre && (
                                <p className="text-xs text-destructive">
                                    {erroresCampo.nombre}
                                </p>
                            )}
                        </div>

                        <div className="space-y-1.5">
                            <Label>Descripción</Label>
                            <Textarea
                                rows={3}
                                maxLength={255}
                                value={form.descripcion}
                                onChange={(e) =>
                                    setForm({ ...form, descripcion: e.target.value })
                                }
                            />
                            {erroresCampo.descripcion && (
                                <p className="text-xs text-destructive">
                                    {erroresCampo.descripcion}
                                </p>
                            )}
                        </div>

                        {errorForm && (
                            <p className="text-xs text-destructive">{errorForm}</p>
                        )}
                    </div>

                    <DialogFooter>
                        <Button
                            variant="outline"
                            onClick={() => setAbierto(false)}
                            disabled={guardando}
                        >
                            Cancelar
                        </Button>

                        <Button onClick={() => void guardar()} disabled={guardando}>
                            Guardar
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </Card>
    );
}

function SeccionSubprogramas({
    onTotalChange,
}: {
    onTotalChange?: (total: number) => void;
}) {
    const [subprogramas, setSubprogramas] = useState<SubprogramaCatalogo[]>([]);
    const [cargando, setCargando] = useState(true);
    const [errorCarga, setErrorCarga] = useState<string | null>(null);
    const [areasReales, setAreasReales] = useState<AreaCatalogo[]>([]);
    const [cargandoAreas, setCargandoAreas] = useState(true);
    const [errorAreas, setErrorAreas] = useState<string | null>(null);
    const [abierto, setAbierto] = useState(false);
    const [guardando, setGuardando] = useState(false);
    const [alternandoId, setAlternandoId] = useState<string | null>(null);
    const [form, setForm] = useState<{
        id?: string;
        nombre: string;
        descripcion: string;
        areaId: string;
    }>({
        nombre: "",
        descripcion: "",
        areaId: "",
    });
    const [errorForm, setErrorForm] = useState("");
    const [erroresCampo, setErroresCampo] = useState<
        Partial<Record<"nombre" | "descripcion" | "areaId", string>>
    >({});

    const areasActivas = areasReales.filter((area) => area.activo);

    const areasParaSelector = useMemo(() => {
        if (!form.areaId || areasActivas.some((area) => area.id === form.areaId)) {
            return areasActivas;
        }
        const areaActual = areasReales.find((area) => area.id === form.areaId);
        return areaActual ? [...areasActivas, areaActual] : areasActivas;
    }, [areasActivas, areasReales, form.areaId]);

    const notificarTotal = (lista: SubprogramaCatalogo[]) => {
        onTotalChange?.(lista.length);
    };

    const cargar = async () => {
        setCargando(true);
        setErrorCarga(null);
        try {
            const resultado = await listarSubprogramas();
            setSubprogramas(resultado);
            notificarTotal(resultado);
        } catch (err) {
            const mensaje =
                err instanceof ApiError
                    ? err.message
                    : "No fue posible cargar los subprocesos.";
            setErrorCarga(mensaje);
        } finally {
            setCargando(false);
        }
    };

    const cargarAreas = async () => {
        setCargandoAreas(true);
        setErrorAreas(null);
        try {
            const resultado = await listarAreas();
            setAreasReales(resultado);
        } catch (err) {
            const mensaje =
                err instanceof ApiError
                    ? err.message
                    : "No fue posible cargar las áreas.";
            setErrorAreas(mensaje);
        } finally {
            setCargandoAreas(false);
        }
    };

    useEffect(() => {
        void cargar();
        void cargarAreas();
    }, []);

    const abrirNuevo = () => {
        setForm({ nombre: "", descripcion: "", areaId: "" });
        setErrorForm("");
        setErroresCampo({});
        setAbierto(true);
    };

    const abrirEditar = (sp: SubprogramaCatalogo) => {
        setForm({
            id: sp.id,
            nombre: sp.nombre,
            descripcion: sp.descripcion,
            areaId: sp.areaId,
        });
        setErrorForm("");
        setErroresCampo({});
        setAbierto(true);
    };

    const guardar = async () => {
        setErrorForm("");
        setErroresCampo({});

        const nombre = form.nombre.trim();
        const descripcion = form.descripcion.trim();

        if (!nombre) {
            setErrorForm("El nombre es obligatorio.");
            return;
        }
        if (nombre.length > 100) {
            setErrorForm("El nombre no puede superar los 100 caracteres.");
            return;
        }
        if (descripcion.length > 255) {
            setErrorForm("La descripción no puede superar los 255 caracteres.");
            return;
        }

        if (!form.areaId) {
            setErrorForm("Debe seleccionar un área responsable.");
            setErroresCampo({ areaId: "Seleccione un área." });
            return;
        }

        const areaSeleccionada = areasReales.find((area) => area.id === form.areaId);
        if (!areaSeleccionada) {
            setErrorForm("Debe seleccionar un área válida.");
            setErroresCampo({ areaId: "Seleccione un área válida." });
            return;
        }

        const areaOriginalId = form.id
            ? subprogramas.find((sp) => sp.id === form.id)?.areaId
            : undefined;
        const cambiaArea = !!form.id && areaOriginalId !== form.areaId;

        if ((!form.id || cambiaArea) && !areaSeleccionada.activo) {
            setErrorForm("Debe seleccionar un área activa válida.");
            setErroresCampo({ areaId: "Seleccione un área activa válida." });
            return;
        }

        setGuardando(true);
        try {
            if (form.id) {
                const actualizado = await actualizarSubprograma(form.id, {
                    nombre,
                    descripcion,
                    areaId: Number(form.areaId),
                });
                setSubprogramas((prev) =>
                    prev.map((sp) =>
                        sp.id === actualizado.id ? actualizado : sp,
                    ),
                );
                toast.success("Registro actualizado");
            } else {
                const creado = await crearSubprograma({
                    nombre,
                    descripcion,
                    areaId: Number(form.areaId),
                });
                setSubprogramas((prev) => {
                    const next = [...prev, creado];
                    notificarTotal(next);
                    return next;
                });
                toast.success("Registro creado");
            }
            setAbierto(false);
        } catch (err) {
            if (err instanceof ApiError) {
                if (err.errores) {
                    setErroresCampo({
                        nombre: err.errores.nombre,
                        descripcion: err.errores.descripcion,
                        areaId: err.errores.areaId,
                    });
                }
                setErrorForm(err.message);
            } else {
                setErrorForm(
                    form.id
                        ? "No fue posible actualizar el subproceso."
                        : "No fue posible crear el subproceso.",
                );
            }
        } finally {
            setGuardando(false);
        }
    };

    const alternarEstado = async (sp: SubprogramaCatalogo) => {
        setAlternandoId(sp.id);
        try {
            const actualizado = await cambiarEstadoSubprograma(sp.id, !sp.activo);
            setSubprogramas((prev) =>
                prev.map((actual) =>
                    actual.id === actualizado.id ? actualizado : actual,
                ),
            );
            toast.success(
                actualizado.activo
                    ? "Registro activado"
                    : "Registro desactivado",
            );
        } catch (err) {
            const mensaje =
                err instanceof ApiError
                    ? err.message
                    : "No fue posible cambiar el estado del subproceso.";
            toast.error(mensaje);
        } finally {
            setAlternandoId(null);
        }
    };

    const etiquetaArea = (sp: SubprogramaCatalogo) => {
        const base = `${sp.areaCodigo} - ${sp.areaNombre}`;
        return sp.areaActiva ? base : `${base} (inactiva)`;
    };

    const nuevoDeshabilitado =
        cargando ||
        cargandoAreas ||
        !!errorCarga ||
        !!errorAreas ||
        areasActivas.length === 0;

    return (
        <Card>
            <CardHeader className="flex-row items-start justify-between space-y-0">
                <div>
                    <CardTitle className="text-base">Subprocesos</CardTitle>
                    <CardDescription>
                        Agrupaciones temáticas para clasificar la documentación
                        asociadas a un área.
                    </CardDescription>

                    {errorAreas && (
                        <div className="mt-2 space-y-2">
                            <p className="text-xs text-destructive">{errorAreas}</p>
                            <Button
                                variant="outline"
                                size="sm"
                                onClick={() => void cargarAreas()}
                            >
                                Reintentar Áreas
                            </Button>
                        </div>
                    )}

                    {!cargandoAreas &&
                        !errorAreas &&
                        areasActivas.length === 0 && (
                            <p className="mt-2 text-xs text-muted-foreground">
                                Debe existir al menos un área activa para crear un
                                subproceso.
                            </p>
                        )}
                </div>

                <Button
                    size="sm"
                    className="gap-1.5"
                    onClick={abrirNuevo}
                    disabled={nuevoDeshabilitado}
                >
                    <Plus className="size-4" />
                    Nuevo
                </Button>
            </CardHeader>

            {cargando ? (
                <CardContent className="py-14 text-center text-sm text-muted-foreground">
                    Cargando subprocesos...
                </CardContent>
            ) : errorCarga ? (
                <CardContent className="space-y-4 py-14 text-center">
                    <p className="text-sm text-muted-foreground">{errorCarga}</p>
                    <Button variant="outline" onClick={() => void cargar()}>
                        Reintentar
                    </Button>
                </CardContent>
            ) : subprogramas.length === 0 ? (
                <CardContent className="py-14 text-center text-sm text-muted-foreground">
                    No hay subprocesos registrados.
                </CardContent>
            ) : (
                <CardContent className="px-0 pb-0">
                    <Table>
                        <TableHeader>
                            <TableRow className="bg-secondary/60">
                                <TableHead>Nombre</TableHead>
                                <TableHead>Área responsable</TableHead>
                                <TableHead>Descripción</TableHead>
                                <TableHead>Estado</TableHead>
                                <TableHead className="text-right">Acciones</TableHead>
                            </TableRow>
                        </TableHeader>

                        <TableBody>
                            {subprogramas.map((sp) => {
                                const { icono: Icono, color } =
                                    obtenerIconoSubProceso(sp.nombre);

                                return (
                                    <TableRow key={sp.id}>
                                        <TableCell className="font-medium">
                                            <div className="flex items-center gap-2">
                                                <Icono
                                                    className={`size-4 ${color}`}
                                                />
                                                <span>{sp.nombre}</span>
                                            </div>
                                        </TableCell>

                                        <TableCell className="text-sm text-muted-foreground">
                                            {etiquetaArea(sp)}
                                        </TableCell>

                                        <TableCell className="text-sm text-muted-foreground">
                                            {sp.descripcion}
                                        </TableCell>

                                        <TableCell>
                                            <ActivoBadge activo={sp.activo} />
                                        </TableCell>

                                        <TableCell>
                                            <div className="flex justify-end gap-2">
                                                <Button
                                                    size="sm"
                                                    variant="outline"
                                                    onClick={() => abrirEditar(sp)}
                                                    disabled={alternandoId === sp.id}
                                                >
                                                    Editar
                                                </Button>

                                                <Button
                                                    size="sm"
                                                    variant="ghost"
                                                    disabled={alternandoId === sp.id}
                                                    onClick={() =>
                                                        void alternarEstado(sp)
                                                    }
                                                >
                                                    {sp.activo
                                                        ? "Desactivar"
                                                        : "Activar"}
                                                </Button>
                                            </div>
                                        </TableCell>
                                    </TableRow>
                                );
                            })}
                        </TableBody>
                    </Table>
                </CardContent>
            )}

            <Dialog open={abierto} onOpenChange={setAbierto}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>
                            {form.id ? "Editar subproceso" : "Nuevo subproceso"}
                        </DialogTitle>
                        <DialogDescription>
                            {form.id
                                ? "Actualice el área responsable, el nombre y la descripción del subproceso."
                                : "Seleccione el área responsable y complete la información."}
                        </DialogDescription>
                    </DialogHeader>

                    <div className="space-y-4">
                        <div className="space-y-1.5">
                            <Label>Área responsable *</Label>
                            <SelectorAreaResponsable
                                areas={areasParaSelector}
                                value={form.areaId}
                                onValueChange={(areaId) =>
                                    setForm({ ...form, areaId })
                                }
                                placeholder="Seleccione un área"
                                disabled={cargandoAreas || !!errorAreas}
                            />
                            {erroresCampo.areaId && (
                                <p className="text-xs text-destructive">
                                    {erroresCampo.areaId}
                                </p>
                            )}
                        </div>

                        <div className="space-y-1.5">
                            <Label>Nombre</Label>
                            <Input
                                value={form.nombre}
                                maxLength={100}
                                onChange={(e) =>
                                    setForm({ ...form, nombre: e.target.value })
                                }
                            />
                            {erroresCampo.nombre && (
                                <p className="text-xs text-destructive">
                                    {erroresCampo.nombre}
                                </p>
                            )}
                        </div>

                        <div className="space-y-1.5">
                            <Label>Descripción</Label>
                            <Textarea
                                rows={3}
                                maxLength={255}
                                value={form.descripcion}
                                onChange={(e) =>
                                    setForm({
                                        ...form,
                                        descripcion: e.target.value,
                                    })
                                }
                            />
                            {erroresCampo.descripcion && (
                                <p className="text-xs text-destructive">
                                    {erroresCampo.descripcion}
                                </p>
                            )}
                        </div>

                        {errorForm && (
                            <p className="text-xs text-destructive">{errorForm}</p>
                        )}
                    </div>

                    <DialogFooter>
                        <Button
                            variant="outline"
                            onClick={() => setAbierto(false)}
                            disabled={guardando}
                        >
                            Cancelar
                        </Button>

                        <Button
                            onClick={() => void guardar()}
                            disabled={guardando}
                        >
                            Guardar
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </Card>
    );
}

function SeccionTiposDocumento({
    onTotalChange,
}: {
    onTotalChange?: (total: number) => void;
}) {
    const [tiposDocumento, setTiposDocumento] = useState<TipoDocumentoCatalogo[]>(
        [],
    );
    const [cargando, setCargando] = useState(true);
    const [errorCarga, setErrorCarga] = useState<string | null>(null);
    const [abierto, setAbierto] = useState(false);
    const [guardando, setGuardando] = useState(false);
    const [alternandoId, setAlternandoId] = useState<string | null>(null);
    const [form, setForm] = useState<{
        id?: string;
        nombre: string;
        descripcion: string;
    }>({
        nombre: "",
        descripcion: "",
    });
    const [errorForm, setErrorForm] = useState("");
    const [erroresCampo, setErroresCampo] = useState<
        Partial<Record<"nombre" | "descripcion", string>>
    >({});

    const notificarTotal = (lista: TipoDocumentoCatalogo[]) => {
        onTotalChange?.(lista.length);
    };

    const cargar = async () => {
        setCargando(true);
        setErrorCarga(null);
        try {
            const resultado = await listarTiposDocumento();
            setTiposDocumento(resultado);
            notificarTotal(resultado);
        } catch (err) {
            const mensaje =
                err instanceof ApiError
                    ? err.message
                    : "No fue posible cargar los tipos de documento.";
            setErrorCarga(mensaje);
        } finally {
            setCargando(false);
        }
    };

    useEffect(() => {
        void cargar();
    }, []);

    const abrirNuevo = () => {
        setForm({
            nombre: "",
            descripcion: "",
        });
        setErrorForm("");
        setErroresCampo({});
        setAbierto(true);
    };

    const abrirEditar = (tipo: TipoDocumentoCatalogo) => {
        setForm({
            id: tipo.id,
            nombre: tipo.nombre,
            descripcion: tipo.descripcion,
        });
        setErrorForm("");
        setErroresCampo({});
        setAbierto(true);
    };

    const guardar = async () => {
        setErrorForm("");
        setErroresCampo({});

        const nombre = form.nombre.trim();
        const descripcion = form.descripcion.trim();

        if (!nombre) {
            setErrorForm("El nombre es obligatorio.");
            return;
        }
        if (nombre.length > 100) {
            setErrorForm("El nombre no puede superar los 100 caracteres.");
            return;
        }
        if (descripcion.length > 255) {
            setErrorForm("La descripción no puede superar los 255 caracteres.");
            return;
        }

        setGuardando(true);
        try {
            if (form.id) {
                const actualizado = await actualizarTipoDocumento(form.id, {
                    nombre,
                    descripcion,
                });
                setTiposDocumento((prev) =>
                    prev.map((tipo) =>
                        tipo.id === actualizado.id ? actualizado : tipo,
                    ),
                );
                toast.success("Registro actualizado");
            } else {
                const creado = await crearTipoDocumento({
                    nombre,
                    descripcion,
                });
                setTiposDocumento((prev) => {
                    const next = [...prev, creado];
                    notificarTotal(next);
                    return next;
                });
                toast.success("Registro creado");
            }
            setAbierto(false);
        } catch (err) {
            if (err instanceof ApiError) {
                if (err.errores) {
                    setErroresCampo({
                        nombre: err.errores.nombre,
                        descripcion: err.errores.descripcion,
                    });
                }
                setErrorForm(err.message);
            } else {
                setErrorForm(
                    form.id
                        ? "No fue posible actualizar el tipo de documento."
                        : "No fue posible crear el tipo de documento.",
                );
            }
        } finally {
            setGuardando(false);
        }
    };

    const alternarEstado = async (tipo: TipoDocumentoCatalogo) => {
        setAlternandoId(tipo.id);
        try {
            const actualizado = await cambiarEstadoTipoDocumento(
                tipo.id,
                !tipo.activo,
            );
            setTiposDocumento((prev) =>
                prev.map((actual) =>
                    actual.id === actualizado.id ? actualizado : actual,
                ),
            );
            toast.success(
                actualizado.activo
                    ? "Registro activado"
                    : "Registro desactivado",
            );
        } catch (err) {
            const mensaje =
                err instanceof ApiError
                    ? err.message
                    : "No fue posible cambiar el estado del tipo de documento.";
            toast.error(mensaje);
        } finally {
            setAlternandoId(null);
        }
    };

    return (
        <Card>
            <CardHeader className="flex-row items-start justify-between space-y-0">
                <div>
                    <CardTitle className="text-base">Tipos de documento</CardTitle>
                    <CardDescription>
                        Plantilla, protocolo, programa, manual, política,
                        procedimiento y otros.
                    </CardDescription>
                </div>

                <Button
                    size="sm"
                    className="gap-1.5"
                    onClick={abrirNuevo}
                    disabled={cargando || !!errorCarga}
                >
                    <Plus className="size-4" />
                    Nuevo
                </Button>
            </CardHeader>

            {cargando ? (
                <CardContent className="py-14 text-center text-sm text-muted-foreground">
                    Cargando tipos de documento...
                </CardContent>
            ) : errorCarga ? (
                <CardContent className="space-y-4 py-14 text-center">
                    <p className="text-sm text-muted-foreground">{errorCarga}</p>
                    <Button variant="outline" onClick={() => void cargar()}>
                        Reintentar
                    </Button>
                </CardContent>
            ) : tiposDocumento.length === 0 ? (
                <CardContent className="py-14 text-center text-sm text-muted-foreground">
                    No hay tipos de documento registrados.
                </CardContent>
            ) : (
                <CardContent className="px-0 pb-0">
                    <Table>
                        <TableHeader>
                            <TableRow className="bg-secondary/60">
                                <TableHead>Nombre</TableHead>
                                <TableHead>Descripción</TableHead>
                                <TableHead>Estado</TableHead>
                                <TableHead className="text-right">Acciones</TableHead>
                            </TableRow>
                        </TableHeader>

                        <TableBody>
                            {tiposDocumento.map((tipo) => {
                                const { icono: Icono, color } =
                                    obtenerIconoFormato(tipo.nombre);

                                return (
                                    <TableRow key={tipo.id}>
                                        <TableCell className="font-medium">
                                            <div className="flex items-center gap-2">
                                                <Icono
                                                    className={`size-4 ${color}`}
                                                />
                                                <span>{tipo.nombre}</span>
                                            </div>
                                        </TableCell>

                                        <TableCell className="text-sm text-muted-foreground">
                                            {tipo.descripcion}
                                        </TableCell>

                                        <TableCell>
                                            <ActivoBadge activo={tipo.activo} />
                                        </TableCell>

                                        <TableCell>
                                            <div className="flex justify-end gap-2">
                                                <Button
                                                    size="sm"
                                                    variant="outline"
                                                    onClick={() => abrirEditar(tipo)}
                                                    disabled={alternandoId === tipo.id}
                                                >
                                                    Editar
                                                </Button>

                                                <Button
                                                    size="sm"
                                                    variant="ghost"
                                                    disabled={alternandoId === tipo.id}
                                                    onClick={() =>
                                                        void alternarEstado(tipo)
                                                    }
                                                >
                                                    {tipo.activo
                                                        ? "Desactivar"
                                                        : "Activar"}
                                                </Button>
                                            </div>
                                        </TableCell>
                                    </TableRow>
                                );
                            })}
                        </TableBody>
                    </Table>
                </CardContent>
            )}

            <Dialog open={abierto} onOpenChange={setAbierto}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>
                            {form.id
                                ? "Editar tipo de documento"
                                : "Nuevo tipo de documento"}
                        </DialogTitle>
                        <DialogDescription>
                            {form.id
                                ? "Actualice la información del tipo de documento."
                                : "Complete la información del tipo de documento."}
                        </DialogDescription>
                    </DialogHeader>

                    <div className="space-y-4">
                        <div className="space-y-1.5">
                            <Label>Nombre *</Label>
                            <Input
                                value={form.nombre}
                                maxLength={100}
                                onChange={(e) =>
                                    setForm({ ...form, nombre: e.target.value })
                                }
                            />
                            {erroresCampo.nombre && (
                                <p className="text-xs text-destructive">
                                    {erroresCampo.nombre}
                                </p>
                            )}
                        </div>

                        <div className="space-y-1.5">
                            <Label>Descripción</Label>
                            <Textarea
                                rows={3}
                                maxLength={255}
                                value={form.descripcion}
                                onChange={(e) =>
                                    setForm({
                                        ...form,
                                        descripcion: e.target.value,
                                    })
                                }
                            />
                            {erroresCampo.descripcion && (
                                <p className="text-xs text-destructive">
                                    {erroresCampo.descripcion}
                                </p>
                            )}
                        </div>

                        {errorForm && (
                            <p className="text-xs text-destructive">{errorForm}</p>
                        )}
                    </div>

                    <DialogFooter>
                        <Button
                            variant="outline"
                            onClick={() => setAbierto(false)}
                            disabled={guardando}
                        >
                            Cancelar
                        </Button>

                        <Button
                            onClick={() => void guardar()}
                            disabled={guardando}
                        >
                            Guardar
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </Card>
    );
}

import { createFileRoute } from "@tanstack/react-router";
import { Search, UserPlus, Pencil, Power, Mail, Users } from "lucide-react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { toast } from "sonner";

import { AppShell } from "@/components/AppShell";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
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
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ApiError } from "@/lib/api";
import { listarAreas, type AreaCatalogo } from "@/lib/areas-api";
import { mapRolBackend } from "@/lib/auth-storage";
import { etiquetaRol, type Rol } from "@/lib/data";
import { obtenerIconoArea } from "@/lib/iconos-areas";
import { obtenerIconoRol } from "@/lib/iconos-roles";
import { useIntranet } from "@/lib/store";
import {
    actualizarUsuario,
    cambiarEstadoUsuario,
    crearUsuario,
    listarRoles,
    listarUsuarios,
    type EstadoUsuario,
    type RolCatalogo,
    type UsuarioCatalogo,
} from "@/lib/usuarios-api";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/app/usuarios")({
    head: () => ({
        meta: [
            { title: "Gestión de usuarios — Intranet documental" },
            {
                name: "description",
                content: "Crea, edita, activa o desactiva las cuentas de los trabajadores autorizados de la empresa.",
            },
            { property: "og:title", content: "Gestión de usuarios — Intranet documental" },
            { property: "og:description", content: "Administra cuentas, áreas, roles y estados de los usuarios." },
        ],
    }),
    component: Usuarios,
});

const TODOS = "todos";

const FILTRO_ROL_SELECT_CONTENT_CLASS =
    "!bg-white !text-slate-900 border border-slate-200 shadow-2xl z-[99999]";

const FILTRO_ROL_SELECT_TRIGGER_CLASS = "!bg-white !text-slate-900";

const FILTRO_ROL_SELECT_ITEM_CLASS =
    "!text-slate-900 focus:!bg-slate-100 focus:!text-slate-900 data-[highlighted]:!bg-slate-100 data-[highlighted]:!text-slate-900";

const OPCIONES_FILTRO_ROL: { value: string; etiqueta: string }[] = [
    { value: TODOS, etiqueta: "Todos los roles" },
    ...(Object.entries(etiquetaRol) as [Rol, string][]).map(([value, etiqueta]) => ({
        value,
        etiqueta,
    })),
];

const formCrearVacio = {
    nombres: "",
    apellidos: "",
    correo: "",
    password: "",
    rolId: "",
    areaId: "",
};

const formEditarVacio = {
    id: "",
    nombres: "",
    apellidos: "",
    correo: "",
    rolId: "",
    areaId: "",
};

function areaVisible(usuario: UsuarioCatalogo) {
    if (usuario.areaPrincipalId) {
        const principal = usuario.areas.find((area) => area.id === usuario.areaPrincipalId);
        if (principal) return principal;
    }
    return usuario.areas[0] ?? null;
}

function EstadoUsuarioBadge({ estado }: { estado: EstadoUsuario }) {
    const config = {
        ACTIVO: {
            label: "Activo",
            className:
                "border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400",
            dot: "bg-emerald-500",
        },
        INACTIVO: {
            label: "Inactivo",
            className: "border-rose-500/30 bg-rose-500/10 text-rose-600 dark:text-rose-400",
            dot: "bg-rose-500",
        },
        BLOQUEADO: {
            label: "Bloqueado",
            className: "border-amber-500/30 bg-amber-500/10 text-amber-600 dark:text-amber-400",
            dot: "bg-amber-500",
        },
    }[estado];

    return (
        <Badge variant="outline" className={cn("gap-1.5 font-medium", config.className)}>
            <span className={cn("size-1.5 rounded-full", config.dot)} />
            {config.label}
        </Badge>
    );
}

function validarFormCrear(
    form: typeof formCrearVacio,
    roles: RolCatalogo[],
    areas: AreaCatalogo[],
) {
    const errores: Record<string, string> = {};
    const nombres = form.nombres.trim();
    const apellidos = form.apellidos.trim();
    const correo = form.correo.trim();

    if (!nombres) errores.nombres = "Los nombres son obligatorios.";
    else if (nombres.length > 100) errores.nombres = "Los nombres no pueden superar los 100 caracteres.";

    if (!apellidos) errores.apellidos = "Los apellidos son obligatorios.";
    else if (apellidos.length > 100) {
        errores.apellidos = "Los apellidos no pueden superar los 100 caracteres.";
    }

    if (!correo) errores.correo = "El correo es obligatorio.";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo)) {
        errores.correo = "Ingresa un correo electrónico válido.";
    } else if (correo.length > 150) {
        errores.correo = "El correo no puede superar los 150 caracteres.";
    }

    if (!form.password) errores.password = "La contraseña inicial es obligatoria.";
    else if (form.password.length < 8 || form.password.length > 100) {
        errores.password = "La contraseña debe tener entre 8 y 100 caracteres.";
    }

    if (!form.rolId) errores.rolId = "Selecciona un rol.";
    else if (!roles.some((rol) => rol.id === form.rolId)) {
        errores.rolId = "Selecciona un rol válido.";
    }

    if (!form.areaId) errores.areaId = "Selecciona el área.";
    else if (!areas.some((area) => area.id === form.areaId)) {
        errores.areaId = "Selecciona un área válida.";
    }

    return errores;
}

function validarFormEditar(
    form: typeof formEditarVacio,
    roles: RolCatalogo[],
    areas: AreaCatalogo[],
) {
    const errores: Record<string, string> = {};
    const nombres = form.nombres.trim();
    const apellidos = form.apellidos.trim();
    const correo = form.correo.trim();

    if (!nombres) errores.nombres = "Los nombres son obligatorios.";
    else if (nombres.length > 100) errores.nombres = "Los nombres no pueden superar los 100 caracteres.";

    if (!apellidos) errores.apellidos = "Los apellidos son obligatorios.";
    else if (apellidos.length > 100) {
        errores.apellidos = "Los apellidos no pueden superar los 100 caracteres.";
    }

    if (!correo) errores.correo = "El correo es obligatorio.";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo)) {
        errores.correo = "Ingresa un correo electrónico válido.";
    } else if (correo.length > 150) {
        errores.correo = "El correo no puede superar los 150 caracteres.";
    }

    if (!form.rolId) errores.rolId = "Selecciona un rol.";
    else if (!roles.some((rol) => rol.id === form.rolId)) {
        errores.rolId = "Selecciona un rol válido.";
    }

    if (!form.areaId) errores.areaId = "Selecciona un área activa.";
    else if (!areas.some((area) => area.id === form.areaId)) {
        errores.areaId = "Selecciona un área activa válida.";
    }

    return errores;
}

function Usuarios() {
    const { permisos } = useIntranet();

    if (!permisos.gestionarUsuarios) {
        return (
            <AppShell titulo="Gestión de usuarios">
                <Card>
                    <CardContent className="py-14 text-center text-sm text-muted-foreground">
                        No cuentas con permisos para acceder a la gestión de usuarios.
                    </CardContent>
                </Card>
            </AppShell>
        );
    }

    return <GestionUsuarios />;
}

function GestionUsuarios() {
    const [usuarios, setUsuarios] = useState<UsuarioCatalogo[]>([]);
    const [cargando, setCargando] = useState(true);
    const [errorCarga, setErrorCarga] = useState<string | null>(null);
    const [busqueda, setBusqueda] = useState("");
    const [rol, setRol] = useState(TODOS);
    const [abierto, setAbierto] = useState(false);
    const [modoDialogo, setModoDialogo] = useState<"crear" | "editar">("crear");
    const [formCrear, setFormCrear] = useState(formCrearVacio);
    const [formEditar, setFormEditar] = useState(formEditarVacio);
    const [errores, setErrores] = useState<Record<string, string>>({});
    const [guardando, setGuardando] = useState(false);
    const [alternandoId, setAlternandoId] = useState<string | null>(null);
    const [rolesCatalogo, setRolesCatalogo] = useState<RolCatalogo[]>([]);
    const [areasCatalogo, setAreasCatalogo] = useState<AreaCatalogo[]>([]);
    const [cargandoCatalogos, setCargandoCatalogos] = useState(false);
    const [errorCatalogos, setErrorCatalogos] = useState<string | null>(null);
    const [areaInactivaNombre, setAreaInactivaNombre] = useState<string | null>(null);
    const catalogosCargados = useRef(false);

    const rolesActivos = useMemo(
        () => rolesCatalogo.filter((item) => item.activo),
        [rolesCatalogo],
    );
    const areasActivas = useMemo(
        () => areasCatalogo.filter((area) => area.activo),
        [areasCatalogo],
    );

    const cargarUsuarios = useCallback(async () => {
        setCargando(true);
        setErrorCarga(null);
        try {
            const resultado = await listarUsuarios();
            setUsuarios(resultado);
        } catch (err) {
            const mensaje =
                err instanceof ApiError
                    ? err.message
                    : "No fue posible cargar los usuarios.";
            setErrorCarga(mensaje);
        } finally {
            setCargando(false);
        }
    }, []);

    const cargarCatalogos = useCallback(async (forzar = false) => {
        if (catalogosCargados.current && !forzar && !errorCatalogos) return;

        setCargandoCatalogos(true);
        setErrorCatalogos(null);
        try {
            const [roles, areas] = await Promise.all([listarRoles(), listarAreas()]);
            setRolesCatalogo(roles);
            setAreasCatalogo(areas);
            catalogosCargados.current = true;
        } catch (err) {
            catalogosCargados.current = false;
            const mensaje =
                err instanceof ApiError
                    ? err.message
                    : "No fue posible cargar roles y áreas.";
            setErrorCatalogos(mensaje);
        } finally {
            setCargandoCatalogos(false);
        }
    }, [errorCatalogos]);

    useEffect(() => {
        let activo = true;

        const cargar = async () => {
            setCargando(true);
            setErrorCarga(null);
            try {
                const resultado = await listarUsuarios();
                if (activo) setUsuarios(resultado);
            } catch (err) {
                if (!activo) return;
                const mensaje =
                    err instanceof ApiError
                        ? err.message
                        : "No fue posible cargar los usuarios.";
                setErrorCarga(mensaje);
            } finally {
                if (activo) setCargando(false);
            }
        };

        cargar();
        return () => {
            activo = false;
        };
    }, []);

    const lista = useMemo(
        () =>
            usuarios.filter((u) => {
                const q = busqueda.trim().toLowerCase();
                if (
                    q &&
                    !`${u.nombreCompleto} ${u.correo}`.toLowerCase().includes(q)
                ) {
                    return false;
                }
                if (rol !== TODOS && mapRolBackend(u.rolCodigo) !== rol) return false;
                return true;
            }),
        [usuarios, busqueda, rol],
    );

    const abrirNuevo = () => {
        setModoDialogo("crear");
        setFormCrear(formCrearVacio);
        setErrores({});
        setAbierto(true);
        void cargarCatalogos();
    };

    const abrirEditar = (u: UsuarioCatalogo) => {
        const area = areaVisible(u);
        const areaActiva = area?.activo ?? false;
        setModoDialogo("editar");
        setAreaInactivaNombre(area && !areaActiva ? area.nombre : null);
        setFormEditar({
            id: u.id,
            nombres: u.nombres,
            apellidos: u.apellidos,
            correo: u.correo,
            rolId: u.rolId,
            areaId: areaActiva && area ? area.id : "",
        });
        setErrores({});
        setAbierto(true);
        void cargarCatalogos();
    };

    const cerrarDialogo = (open: boolean) => {
        setAbierto(open);
        if (!open) {
            setFormCrear(formCrearVacio);
            setFormEditar(formEditarVacio);
            setAreaInactivaNombre(null);
            setErrores({});
            setGuardando(false);
        }
    };

    const guardarCrear = async () => {
        if (cargandoCatalogos || errorCatalogos) return;

        const e = validarFormCrear(formCrear, rolesActivos, areasActivas);
        setErrores(e);
        if (Object.keys(e).length) return;

        const rolSeleccionado = rolesActivos.find((item) => item.id === formCrear.rolId);
        if (!rolSeleccionado) {
            setErrores({ rolId: "Selecciona un rol válido." });
            return;
        }

        const areaId = Number(formCrear.areaId);
        const body = {
            nombres: formCrear.nombres.trim(),
            apellidos: formCrear.apellidos.trim(),
            correo: formCrear.correo.trim(),
            password: formCrear.password,
            rolId: Number(formCrear.rolId),
            areaIds: [areaId],
            areaPrincipalId: rolSeleccionado.codigo === "ADMINISTRADOR" ? null : areaId,
        };

        setGuardando(true);
        try {
            const creado = await crearUsuario(body);
            setUsuarios((prev) => [...prev, creado]);
            setFormCrear(formCrearVacio);
            setErrores({});
            setAbierto(false);
            toast.success("Usuario creado correctamente");
        } catch (err) {
            if (err instanceof ApiError) {
                if (err.status === 409) {
                    setErrores({ correo: err.message });
                } else if (err.errores) {
                    const mapeados: Record<string, string> = {};
                    for (const [campo, mensaje] of Object.entries(err.errores)) {
                        mapeados[campo] = mensaje;
                    }
                    setErrores(mapeados);
                }
                toast.error(err.message);
            } else {
                toast.error("No fue posible crear el usuario.");
            }
        } finally {
            setGuardando(false);
        }
    };

    const guardarEditar = async () => {
        if (cargandoCatalogos || errorCatalogos) return;

        const e = validarFormEditar(formEditar, rolesActivos, areasActivas);
        setErrores(e);
        if (Object.keys(e).length) return;

        const rolSeleccionado = rolesActivos.find((item) => item.id === formEditar.rolId);
        if (!rolSeleccionado) {
            setErrores({ rolId: "Selecciona un rol válido." });
            return;
        }

        const areaId = Number(formEditar.areaId);
        const body = {
            nombres: formEditar.nombres.trim(),
            apellidos: formEditar.apellidos.trim(),
            correo: formEditar.correo.trim(),
            rolId: Number(formEditar.rolId),
            areaIds: [areaId],
            areaPrincipalId: rolSeleccionado.codigo === "ADMINISTRADOR" ? null : areaId,
        };

        setGuardando(true);
        try {
            const actualizado = await actualizarUsuario(formEditar.id, body);
            setUsuarios((prev) =>
                prev.map((usuario) =>
                    usuario.id === actualizado.id ? actualizado : usuario,
                ),
            );
            setFormEditar(formEditarVacio);
            setAreaInactivaNombre(null);
            setErrores({});
            setAbierto(false);
            toast.success("Usuario actualizado correctamente");
        } catch (err) {
            if (err instanceof ApiError) {
                if (err.status === 409) {
                    setErrores({ correo: err.message });
                } else if (err.errores) {
                    const mapeados: Record<string, string> = {};
                    for (const [campo, mensaje] of Object.entries(err.errores)) {
                        mapeados[campo] = mensaje;
                    }
                    setErrores(mapeados);
                }
                toast.error(err.message);
            } else {
                toast.error("No fue posible actualizar el usuario.");
            }
        } finally {
            setGuardando(false);
        }
    };

    const guardar = () => {
        if (modoDialogo === "crear") {
            void guardarCrear();
            return;
        }
        void guardarEditar();
    };

    const cambiarEstado = async (usuario: UsuarioCatalogo) => {
        if (usuario.estado === "BLOQUEADO") return;

        const nuevoEstado: EstadoUsuario =
            usuario.estado === "ACTIVO" ? "INACTIVO" : "ACTIVO";

        setAlternandoId(usuario.id);
        try {
            const actualizado = await cambiarEstadoUsuario(usuario.id, nuevoEstado);
            setUsuarios((prev) =>
                prev.map((item) => (item.id === actualizado.id ? actualizado : item)),
            );
            toast.success(
                nuevoEstado === "ACTIVO"
                    ? "Usuario activado correctamente"
                    : "Usuario desactivado correctamente",
            );
        } catch (err) {
            const mensaje =
                err instanceof ApiError
                    ? err.message
                    : "No fue posible cambiar el estado del usuario.";
            toast.error(mensaje);
        } finally {
            setAlternandoId(null);
        }
    };

    const obtenerIniciales = (nombre: string) => {
        return nombre
            .split(" ")
            .map((n) => n[0])
            .slice(0, 2)
            .join("")
            .toUpperCase();
    };

    const mensajeVacio = () => {
        if (usuarios.length === 0) return "No hay usuarios registrados.";
        return "No se encontraron usuarios con los filtros seleccionados.";
    };

    const formularioDeshabilitado =
        guardando || cargandoCatalogos || !!errorCatalogos;

    return (
        <AppShell
            titulo="Gestión de usuarios"
            descripcion={`${usuarios.length} cuentas registradas en el sistema`}
            acciones={
                <Button
                    size="sm"
                    className="gap-1.5 shadow-sm"
                    onClick={abrirNuevo}
                    disabled={cargando || !!errorCarga}
                >
                    <UserPlus className="size-4" /> Crear usuario
                </Button>
            }
        >
            <Card className="border-border/60 shadow-xs">
                <CardContent className="grid gap-3 py-4 md:grid-cols-[1fr_240px]">
                    <div className="relative">
                        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                        <Input
                            className="pl-9 bg-background/50"
                            placeholder="Buscar por nombre o correo electrónico…"
                            value={busqueda}
                            onChange={(e) => setBusqueda(e.target.value)}
                            disabled={cargando || !!errorCarga}
                        />
                    </div>
                    <Select value={rol} onValueChange={setRol} disabled={cargando || !!errorCarga}>
                        <SelectTrigger
                            className={cn("bg-background/50", FILTRO_ROL_SELECT_TRIGGER_CLASS)}
                        >
                            <SelectValue placeholder="Filtrar por rol" />
                        </SelectTrigger>
                        <SelectContent className={FILTRO_ROL_SELECT_CONTENT_CLASS}>
                            {OPCIONES_FILTRO_ROL.map(({ value, etiqueta }) => {
                                const esTodos = value === TODOS;
                                const { icono: Icono, color } = esTodos
                                    ? { icono: Users, color: "text-muted-foreground" }
                                    : obtenerIconoRol(etiqueta);

                                return (
                                    <SelectItem
                                        key={value}
                                        value={value}
                                        className={FILTRO_ROL_SELECT_ITEM_CLASS}
                                    >
                                        <div className="flex items-center gap-2">
                                            <Icono className={`size-4 shrink-0 ${color}`} />
                                            <span>{etiqueta}</span>
                                        </div>
                                    </SelectItem>
                                );
                            })}
                        </SelectContent>
                    </Select>
                </CardContent>
            </Card>

            <Card className="overflow-hidden py-0 border-border/60 shadow-xs">
                {cargando ? (
                    <CardContent className="py-14 text-center text-sm text-muted-foreground">
                        Cargando usuarios...
                    </CardContent>
                ) : errorCarga ? (
                    <CardContent className="flex flex-col items-center gap-3 py-14 text-center">
                        <p className="text-sm text-muted-foreground">{errorCarga}</p>
                        <Button variant="outline" size="sm" onClick={cargarUsuarios}>
                            Reintentar
                        </Button>
                    </CardContent>
                ) : (
                    <Table>
                        <TableHeader>
                            <TableRow className="bg-muted/50 hover:bg-muted/50">
                                <TableHead className="py-3.5">Nombre</TableHead>
                                <TableHead className="py-3.5">Correo</TableHead>
                                <TableHead className="py-3.5">Área</TableHead>
                                <TableHead className="py-3.5">Rol</TableHead>
                                <TableHead className="py-3.5">Estado</TableHead>
                                <TableHead className="py-3.5 text-right">Acciones</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {lista.length === 0 ? (
                                <TableRow>
                                    <TableCell colSpan={6} className="h-24 text-center text-muted-foreground">
                                        {mensajeVacio()}
                                    </TableCell>
                                </TableRow>
                            ) : (
                                lista.map((u) => {
                                    const area = areaVisible(u);
                                    const { icono: IconoRol, color: colorRol } = obtenerIconoRol(
                                        u.rolEtiqueta,
                                    );
                                    const esActivo = u.estado === "ACTIVO";
                                    const alternando = alternandoId === u.id;

                                    return (
                                        <TableRow key={u.id} className="transition-colors hover:bg-muted/30">
                                            <TableCell className="font-medium">
                                                <div className="flex items-center gap-3">
                                                    <div className="flex size-8 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-semibold text-primary">
                                                        {obtenerIniciales(u.nombreCompleto)}
                                                    </div>
                                                    <span className="text-foreground">{u.nombreCompleto}</span>
                                                </div>
                                            </TableCell>
                                            <TableCell className="text-sm text-muted-foreground">
                                                <div className="flex items-center gap-1.5">
                                                    <Mail className="size-3.5 text-muted-foreground/70" />
                                                    {u.correo}
                                                </div>
                                            </TableCell>
                                            <TableCell className="text-sm">
                                                {area ? (
                                                    (() => {
                                                        const { icono: IconoArea, color: colorArea } =
                                                            obtenerIconoArea(area.nombre);

                                                        return (
                                                            <div className="flex items-center gap-2">
                                                                <IconoArea className={`size-4 ${colorArea}`} />
                                                                <span>{area.nombre}</span>
                                                            </div>
                                                        );
                                                    })()
                                                ) : (
                                                    <span className="text-muted-foreground">—</span>
                                                )}
                                            </TableCell>
                                            <TableCell className="text-sm">
                                                <div className="inline-flex items-center gap-2 font-medium">
                                                    <IconoRol className={`size-4 ${colorRol}`} />
                                                    <span>{u.rolEtiqueta}</span>
                                                </div>
                                            </TableCell>
                                            <TableCell>
                                                <EstadoUsuarioBadge estado={u.estado} />
                                            </TableCell>
                                            <TableCell>
                                                <div className="flex justify-end gap-1.5">
                                                    <Button
                                                        size="sm"
                                                        variant="outline"
                                                        className="h-8 gap-1 px-2.5 text-xs"
                                                        onClick={() => abrirEditar(u)}
                                                        disabled={alternando}
                                                    >
                                                        <Pencil className="size-3" />
                                                        Editar
                                                    </Button>
                                                    {u.estado === "BLOQUEADO" ? (
                                                        <Button
                                                            size="sm"
                                                            variant="ghost"
                                                            disabled
                                                            className="h-8 gap-1 px-2.5 text-xs text-muted-foreground"
                                                        >
                                                            <Power className="size-3" />
                                                            Bloqueado
                                                        </Button>
                                                    ) : (
                                                        <Button
                                                            size="sm"
                                                            variant="ghost"
                                                            className={`h-8 gap-1 px-2.5 text-xs ${esActivo ? "text-rose-600 hover:text-rose-700 hover:bg-rose-50 dark:hover:bg-rose-950/50" : "text-emerald-600 hover:text-emerald-700 hover:bg-emerald-50 dark:hover:bg-emerald-950/50"}`}
                                                            disabled={alternando}
                                                            onClick={() => void cambiarEstado(u)}
                                                        >
                                                            <Power className="size-3" />
                                                            {alternando
                                                                ? esActivo
                                                                    ? "Desactivando..."
                                                                    : "Activando..."
                                                                : esActivo
                                                                  ? "Desactivar"
                                                                  : "Activar"}
                                                        </Button>
                                                    )}
                                                </div>
                                            </TableCell>
                                        </TableRow>
                                    );
                                })
                            )}
                        </TableBody>
                    </Table>
                )}
            </Card>

            <Dialog open={abierto} onOpenChange={cerrarDialogo}>
                <DialogContent className="sm:max-w-[480px]">
                    <DialogHeader>
                        <DialogTitle>
                            {modoDialogo === "crear" ? "Crear nuevo usuario" : "Editar usuario"}
                        </DialogTitle>
                        <DialogDescription>
                            Configure los datos de acceso del usuario.
                        </DialogDescription>
                    </DialogHeader>

                    {modoDialogo === "crear" ? (
                        <div className="space-y-4 py-2">
                            {cargandoCatalogos ? (
                                <p className="text-sm text-muted-foreground">Cargando roles y áreas...</p>
                            ) : errorCatalogos ? (
                                <div className="space-y-2 text-center">
                                    <p className="text-sm text-muted-foreground">{errorCatalogos}</p>
                                    <Button
                                        variant="outline"
                                        size="sm"
                                        onClick={() => void cargarCatalogos(true)}
                                    >
                                        Reintentar
                                    </Button>
                                </div>
                            ) : (
                                <>
                                    <div className="grid gap-3 sm:grid-cols-2">
                                        <div className="space-y-1.5">
                                            <Label>Nombres</Label>
                                            <Input
                                                placeholder="Ej. Ana María"
                                                value={formCrear.nombres}
                                                onChange={(e) =>
                                                    setFormCrear({ ...formCrear, nombres: e.target.value })
                                                }
                                                disabled={formularioDeshabilitado}
                                            />
                                            {errores.nombres && (
                                                <p className="text-xs text-destructive">{errores.nombres}</p>
                                            )}
                                        </div>
                                        <div className="space-y-1.5">
                                            <Label>Apellidos</Label>
                                            <Input
                                                placeholder="Ej. Pérez Gómez"
                                                value={formCrear.apellidos}
                                                onChange={(e) =>
                                                    setFormCrear({ ...formCrear, apellidos: e.target.value })
                                                }
                                                disabled={formularioDeshabilitado}
                                            />
                                            {errores.apellidos && (
                                                <p className="text-xs text-destructive">{errores.apellidos}</p>
                                            )}
                                        </div>
                                    </div>
                                    <div className="space-y-1.5">
                                        <Label>Correo electrónico</Label>
                                        <Input
                                            type="email"
                                            placeholder="nombre@ejemplo.com"
                                            value={formCrear.correo}
                                            onChange={(e) =>
                                                setFormCrear({ ...formCrear, correo: e.target.value })
                                            }
                                            disabled={formularioDeshabilitado}
                                        />
                                        {errores.correo && (
                                            <p className="text-xs text-destructive">{errores.correo}</p>
                                        )}
                                    </div>
                                    <div className="space-y-1.5">
                                        <Label>Contraseña inicial</Label>
                                        <Input
                                            type="password"
                                            autoComplete="new-password"
                                            value={formCrear.password}
                                            onChange={(e) =>
                                                setFormCrear({ ...formCrear, password: e.target.value })
                                            }
                                            disabled={formularioDeshabilitado}
                                        />
                                        {errores.password && (
                                            <p className="text-xs text-destructive">{errores.password}</p>
                                        )}
                                    </div>
                                    <div className="grid gap-3 sm:grid-cols-2">
                                        <div className="space-y-1.5">
                                            <Label>Área</Label>
                                            <Select
                                                value={formCrear.areaId}
                                                onValueChange={(v) =>
                                                    setFormCrear({ ...formCrear, areaId: v })
                                                }
                                                disabled={formularioDeshabilitado}
                                            >
                                                <SelectTrigger className="w-full">
                                                    <SelectValue placeholder="Seleccionar…" />
                                                </SelectTrigger>
                                                <SelectContent className="bg-white text-slate-900 dark:bg-zinc-900 dark:text-zinc-50 shadow-2xl border border-slate-200 dark:border-zinc-800 z-[99999]">
                                                    {areasActivas.map((area) => {
                                                        const { icono: Icono, color } = obtenerIconoArea(
                                                            area.nombre,
                                                        );

                                                        return (
                                                            <SelectItem key={area.id} value={area.id}>
                                                                <div className="flex items-center gap-2">
                                                                    <Icono className={`size-4 ${color}`} />
                                                                    <span>{area.nombre}</span>
                                                                </div>
                                                            </SelectItem>
                                                        );
                                                    })}
                                                </SelectContent>
                                            </Select>
                                            {errores.areaId && (
                                                <p className="text-xs text-destructive">{errores.areaId}</p>
                                            )}
                                        </div>
                                        <div className="space-y-1.5">
                                            <Label>Rol</Label>
                                            <Select
                                                value={formCrear.rolId}
                                                onValueChange={(v) =>
                                                    setFormCrear({ ...formCrear, rolId: v })
                                                }
                                                disabled={formularioDeshabilitado}
                                            >
                                                <SelectTrigger className="w-full">
                                                    <SelectValue placeholder="Seleccionar…" />
                                                </SelectTrigger>
                                                <SelectContent className="bg-white text-slate-900 dark:bg-zinc-900 dark:text-zinc-50 shadow-2xl border border-slate-200 dark:border-zinc-800 z-[99999]">
                                                    {rolesActivos.map((item) => {
                                                        const { icono: Icono, color } = obtenerIconoRol(
                                                            item.etiqueta,
                                                        );

                                                        return (
                                                            <SelectItem key={item.id} value={item.id}>
                                                                <div className="flex items-center gap-2">
                                                                    <Icono className={`size-4 ${color}`} />
                                                                    <span>{item.etiqueta}</span>
                                                                </div>
                                                            </SelectItem>
                                                        );
                                                    })}
                                                </SelectContent>
                                            </Select>
                                            {errores.rolId && (
                                                <p className="text-xs text-destructive">{errores.rolId}</p>
                                            )}
                                        </div>
                                    </div>
                                </>
                            )}
                        </div>
                    ) : (
                        <div className="space-y-4 py-2">
                            {cargandoCatalogos ? (
                                <p className="text-sm text-muted-foreground">Cargando roles y áreas...</p>
                            ) : errorCatalogos ? (
                                <div className="space-y-2 text-center">
                                    <p className="text-sm text-muted-foreground">{errorCatalogos}</p>
                                    <Button
                                        variant="outline"
                                        size="sm"
                                        onClick={() => void cargarCatalogos(true)}
                                    >
                                        Reintentar
                                    </Button>
                                </div>
                            ) : (
                                <>
                                    <div className="grid gap-3 sm:grid-cols-2">
                                        <div className="space-y-1.5">
                                            <Label>Nombres</Label>
                                            <Input
                                                placeholder="Ej. Ana María"
                                                value={formEditar.nombres}
                                                onChange={(e) =>
                                                    setFormEditar({ ...formEditar, nombres: e.target.value })
                                                }
                                                disabled={formularioDeshabilitado}
                                            />
                                            {errores.nombres && (
                                                <p className="text-xs text-destructive">{errores.nombres}</p>
                                            )}
                                        </div>
                                        <div className="space-y-1.5">
                                            <Label>Apellidos</Label>
                                            <Input
                                                placeholder="Ej. Pérez Gómez"
                                                value={formEditar.apellidos}
                                                onChange={(e) =>
                                                    setFormEditar({ ...formEditar, apellidos: e.target.value })
                                                }
                                                disabled={formularioDeshabilitado}
                                            />
                                            {errores.apellidos && (
                                                <p className="text-xs text-destructive">{errores.apellidos}</p>
                                            )}
                                        </div>
                                    </div>
                                    <div className="space-y-1.5">
                                        <Label>Correo electrónico</Label>
                                        <Input
                                            type="email"
                                            placeholder="nombre@ejemplo.com"
                                            value={formEditar.correo}
                                            onChange={(e) =>
                                                setFormEditar({ ...formEditar, correo: e.target.value })
                                            }
                                            disabled={formularioDeshabilitado}
                                        />
                                        {errores.correo && (
                                            <p className="text-xs text-destructive">{errores.correo}</p>
                                        )}
                                    </div>
                                    <div className="grid gap-3 sm:grid-cols-2">
                                        <div className="space-y-1.5">
                                            <Label>Área</Label>
                                            {areaInactivaNombre && (
                                                <p className="text-xs text-amber-600 dark:text-amber-400">
                                                    El área actual ({areaInactivaNombre}) está inactiva.
                                                    Seleccione un área activa.
                                                </p>
                                            )}
                                            <Select
                                                value={formEditar.areaId}
                                                onValueChange={(v) =>
                                                    setFormEditar({ ...formEditar, areaId: v })
                                                }
                                                disabled={formularioDeshabilitado}
                                            >
                                                <SelectTrigger className="w-full">
                                                    <SelectValue placeholder="Seleccionar…" />
                                                </SelectTrigger>
                                                <SelectContent className="bg-white text-slate-900 dark:bg-zinc-900 dark:text-zinc-50 shadow-2xl border border-slate-200 dark:border-zinc-800 z-[99999]">
                                                    {areasActivas.map((area) => {
                                                        const { icono: Icono, color } = obtenerIconoArea(
                                                            area.nombre,
                                                        );

                                                        return (
                                                            <SelectItem key={area.id} value={area.id}>
                                                                <div className="flex items-center gap-2">
                                                                    <Icono className={`size-4 ${color}`} />
                                                                    <span>{area.nombre}</span>
                                                                </div>
                                                            </SelectItem>
                                                        );
                                                    })}
                                                </SelectContent>
                                            </Select>
                                            {errores.areaId && (
                                                <p className="text-xs text-destructive">{errores.areaId}</p>
                                            )}
                                        </div>
                                        <div className="space-y-1.5">
                                            <Label>Rol</Label>
                                            <Select
                                                value={formEditar.rolId}
                                                onValueChange={(v) =>
                                                    setFormEditar({ ...formEditar, rolId: v })
                                                }
                                                disabled={formularioDeshabilitado}
                                            >
                                                <SelectTrigger className="w-full">
                                                    <SelectValue placeholder="Seleccionar…" />
                                                </SelectTrigger>
                                                <SelectContent className="bg-white text-slate-900 dark:bg-zinc-900 dark:text-zinc-50 shadow-2xl border border-slate-200 dark:border-zinc-800 z-[99999]">
                                                    {rolesActivos.map((item) => {
                                                        const { icono: Icono, color } = obtenerIconoRol(
                                                            item.etiqueta,
                                                        );

                                                        return (
                                                            <SelectItem key={item.id} value={item.id}>
                                                                <div className="flex items-center gap-2">
                                                                    <Icono className={`size-4 ${color}`} />
                                                                    <span>{item.etiqueta}</span>
                                                                </div>
                                                            </SelectItem>
                                                        );
                                                    })}
                                                </SelectContent>
                                            </Select>
                                            {errores.rolId && (
                                                <p className="text-xs text-destructive">{errores.rolId}</p>
                                            )}
                                        </div>
                                    </div>
                                </>
                            )}
                        </div>
                    )}

                    <DialogFooter>
                        <Button variant="outline" onClick={() => cerrarDialogo(false)} disabled={guardando}>
                            Cancelar
                        </Button>
                        <Button
                            onClick={guardar}
                            disabled={
                                formularioDeshabilitado ||
                                cargandoCatalogos ||
                                !!errorCatalogos
                            }
                        >
                            {modoDialogo === "crear"
                                ? guardando
                                    ? "Creando..."
                                    : "Crear usuario"
                                : guardando
                                  ? "Guardando..."
                                  : "Guardar cambios"}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </AppShell>
    );
}

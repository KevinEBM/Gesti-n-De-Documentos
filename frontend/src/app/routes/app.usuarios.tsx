import { createFileRoute } from "@tanstack/react-router";
import { Search, UserPlus, Pencil, Power, ShieldCheck, Mail, Building2 } from "lucide-react";
import { useMemo, useState } from "react";
import { toast } from "sonner";

import { AppShell } from "@/components/AppShell";
import { ActivoBadge } from "@/components/EstadoBadge";
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
import type { Usuario } from "@/lib/data";
import { etiquetaRol } from "@/lib/data";
import { useIntranet } from "@/lib/store";

import { obtenerIconoArea } from "@/lib/iconos-areas";
import { obtenerIconoRol } from "@/lib/iconos-roles";

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
const vacio = { nombre: "", correo: "", areaId: "", rol: "administrativo", activo: true };

function Usuarios() {
    const { usuarios, areas, nombreArea, guardarUsuario, alternarUsuario } = useIntranet();
    const [busqueda, setBusqueda] = useState("");
    const [rol, setRol] = useState(TODOS);
    const [abierto, setAbierto] = useState(false);
    const [form, setForm] = useState<typeof vacio & { id?: string }>(vacio);
    const [errores, setErrores] = useState<Record<string, string>>({});

    const lista = useMemo(
        () =>
            usuarios.filter((u) => {
                const q = busqueda.trim().toLowerCase();
                if (q && !`${u.nombre} ${u.correo}`.toLowerCase().includes(q)) return false;
                if (rol !== TODOS && u.rol !== rol) return false;
                return true;
            }),
        [usuarios, busqueda, rol],
    );

    const abrirNuevo = () => {
        setForm(vacio);
        setErrores({});
        setAbierto(true);
    };

    const abrirEditar = (u: Usuario) => {
        setForm({ ...u });
        setErrores({});
        setAbierto(true);
    };

    const guardar = () => {
        const e: Record<string, string> = {};
        if (!form.nombre.trim()) e.nombre = "El nombre es obligatorio.";
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.correo)) e.correo = "Ingresa un correo institucional válido.";
        if (!form.areaId) e.areaId = "Selecciona el área.";
        setErrores(e);
        if (Object.keys(e).length) return;
        guardarUsuario(form as Omit<Usuario, "id"> & { id?: string });
        toast.success(form.id ? "Usuario actualizado" : "Usuario creado correctamente");
        setAbierto(false);
    };

    const obtenerIniciales = (nombre: string) => {
        return nombre
            .split(" ")
            .map((n) => n[0])
            .slice(0, 2)
            .join("")
            .toUpperCase();
    };

    return (
        <AppShell
            titulo="Gestión de usuarios"
            descripcion={`${usuarios.length} cuentas registradas en el sistema`}
            acciones={
                <Button size="sm" className="gap-1.5 shadow-sm" onClick={abrirNuevo}>
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
                        />
                    </div>
                    <Select value={rol} onValueChange={setRol}>
                        <SelectTrigger className="bg-background/50">
                            <SelectValue placeholder="Filtrar por rol" />
                        </SelectTrigger>
                        <SelectContent className="bg-white text-slate-900 dark:bg-zinc-900 dark:text-zinc-50 shadow-2xl border border-slate-200 dark:border-zinc-800 z-[99999]">
                            <SelectItem value={TODOS}>Todos los roles</SelectItem>
                            <SelectItem value="administrador">Administrador</SelectItem>
                            <SelectItem value="jefe_area">Jefe de área</SelectItem>
                            <SelectItem value="administrativo">Administrativo</SelectItem>
                        </SelectContent>
                    </Select>
                </CardContent>
            </Card>

            <Card className="overflow-hidden py-0 border-border/60 shadow-xs">
                <Table>
                    <TableHeader>
                        <TableRow className="bg-muted/50 hover:bg-muted/50">
                            <TableHead className="py-3.5">Nombre</TableHead>
                            <TableHead className="py-3.5">Correo institucional</TableHead>
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
                                    No se encontraron usuarios con los filtros actuales.
                                </TableCell>
                            </TableRow>
                        ) : (
                            lista.map((u) => (
                                <TableRow key={u.id} className="transition-colors hover:bg-muted/30">
                                    <TableCell className="font-medium">
                                        <div className="flex items-center gap-3">
                                            <div className="flex size-8 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-semibold text-primary">
                                                {obtenerIniciales(u.nombre)}
                                            </div>
                                            <span className="text-foreground">{u.nombre}</span>
                                        </div>
                                    </TableCell>
                                    <TableCell className="text-sm text-muted-foreground">
                                        <div className="flex items-center gap-1.5">
                                            <Mail className="size-3.5 text-muted-foreground/70" />
                                            {u.correo}
                                        </div>
                                    </TableCell>
                                    <TableCell className="text-sm">
                                        {(() => {
                                            const nombre = nombreArea(u.areaId);
                                            const { icono: Icono, color } = obtenerIconoArea(nombre);

                                            return (
                                                <div className="flex items-center gap-2">
                                                    <Icono className={`size-4 ${color}`} />
                                                    <span>{nombre}</span>
                                                </div>
                                            );
                                        })()}
                                    </TableCell>
                                    <TableCell className="text-sm">
                                        {(() => {
                                            const nombre = etiquetaRol[u.rol];
                                            const { icono: Icono, color } = obtenerIconoRol(nombre);

                                            return (
                                                <div className="inline-flex items-center gap-2 font-medium">
                                                    <Icono className={`size-4 ${color}`} />
                                                    <span>{nombre}</span>
                                                </div>
                                            );
                                        })()}
                                    </TableCell>
                                    <TableCell>
                                        <ActivoBadge activo={u.activo} />
                                    </TableCell>
                                    <TableCell>
                                        <div className="flex justify-end gap-1.5">
                                            <Button
                                                size="sm"
                                                variant="outline"
                                                className="h-8 gap-1 px-2.5 text-xs"
                                                onClick={() => abrirEditar(u)}
                                            >
                                                <Pencil className="size-3" />
                                                Editar
                                            </Button>
                                            <Button
                                                size="sm"
                                                variant="ghost"
                                                className={`h-8 gap-1 px-2.5 text-xs ${u.activo ? "text-rose-600 hover:text-rose-700 hover:bg-rose-50 dark:hover:bg-rose-950/50" : "text-emerald-600 hover:text-emerald-700 hover:bg-emerald-50 dark:hover:bg-emerald-950/50"}`}
                                                onClick={() => {
                                                    alternarUsuario(u.id);
                                                    toast.success(u.activo ? "Usuario desactivado" : "Usuario activado");
                                                }}
                                            >
                                                <Power className="size-3" />
                                                {u.activo ? "Desactivar" : "Activar"}
                                            </Button>
                                        </div>
                                    </TableCell>
                                </TableRow>
                            ))
                        )}
                    </TableBody>
                </Table>
            </Card>

            <Dialog open={abierto} onOpenChange={setAbierto}>
                <DialogContent className="sm:max-w-[480px]">
                    <DialogHeader>
                        <DialogTitle>{form.id ? "Editar usuario" : "Crear nuevo usuario"}</DialogTitle>
                        <DialogDescription>
                            Las cuentas son gestionadas de forma interna; configure los accesos corporativos.
                        </DialogDescription>
                    </DialogHeader>
                    <div className="space-y-4 py-2">
                        <div className="space-y-1.5">
                            <Label>Nombre completo</Label>
                            <Input placeholder="Ej. Ana Pérez" value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} />
                            {errores.nombre && <p className="text-xs text-destructive">{errores.nombre}</p>}
                        </div>
                        <div className="space-y-1.5">
                            <Label>Correo institucional</Label>
                            <Input
                                type="email"
                                placeholder="nombre@empresa.com"
                                value={form.correo}
                                onChange={(e) => setForm({ ...form, correo: e.target.value })}
                            />
                            {errores.correo && <p className="text-xs text-destructive">{errores.correo}</p>}
                        </div>
                        <div className="grid gap-3 sm:grid-cols-2">
                            <div className="space-y-1.5">
                                <Label>Área</Label>
                                <Select value={form.areaId} onValueChange={(v) => setForm({ ...form, areaId: v })}>
                                    <SelectTrigger className="w-full">
                                        <SelectValue placeholder="Seleccionar…" />
                                    </SelectTrigger>
                                    <SelectContent className="bg-white text-slate-900 dark:bg-zinc-900 dark:text-zinc-50 shadow-2xl border border-slate-200 dark:border-zinc-800 z-[99999]">
                                        {areas.map((a) => {
                                            const { icono: Icono, color } = obtenerIconoArea(a.nombre);

                                            return (
                                                <SelectItem key={a.id} value={a.id}>
                                                    <div className="flex items-center gap-2">
                                                        <Icono className={`size-4 ${color}`} />
                                                        <span>{a.nombre}</span>
                                                    </div>
                                                </SelectItem>
                                            );
                                        })}
                                    </SelectContent>
                                </Select>
                                {errores.areaId && <p className="text-xs text-destructive">{errores.areaId}</p>}
                            </div>
                            <div className="space-y-1.5">
                                <Label>Rol</Label>
                                <Select value={form.rol} onValueChange={(v) => setForm({ ...form, rol: v })}>
                                    <SelectTrigger className="w-full">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent className="bg-white text-slate-900 dark:bg-zinc-900 dark:text-zinc-50 shadow-2xl border border-slate-200 dark:border-zinc-800 z-[99999]">
                                        <SelectItem value="jefe_area">Jefe de área</SelectItem>
                                        <SelectItem value="administrativo">Administrativo</SelectItem>
                                        <SelectItem value="administrador">Administrador</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>
                        </div>
                        <div className="space-y-1.5">
                            <Label>Estado</Label>
                            <Select
                                value={form.activo ? "activo" : "inactivo"}
                                onValueChange={(v) => setForm({ ...form, activo: v === "activo" })}
                            >
                                <SelectTrigger className="w-full">
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent className="bg-white text-slate-900 dark:bg-zinc-900 dark:text-zinc-50 shadow-2xl border border-slate-200 dark:border-zinc-800 z-[99999]">
                                    <SelectItem value="activo">Activo</SelectItem>
                                    <SelectItem value="inactivo">Inactivo</SelectItem>
                                </SelectContent>
                            </Select>
                        </div>
                    </div>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setAbierto(false)}>
                            Cancelar
                        </Button>
                        <Button onClick={guardar}>{form.id ? "Guardar cambios" : "Crear usuario"}</Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </AppShell>
    );
}
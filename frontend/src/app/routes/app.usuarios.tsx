import { createFileRoute } from "@tanstack/react-router";
import { Search, UserPlus } from "lucide-react";
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

    return (
        <AppShell
            titulo="Gestión de usuarios"
            descripcion={`${usuarios.length} cuentas registradas`}
            acciones={
                <Button size="sm" className="gap-1.5" onClick={abrirNuevo}>
                    <UserPlus className="size-4" /> Crear usuario
                </Button>
            }
        >
            <Card>
                <CardContent className="grid gap-3 py-5 md:grid-cols-[1fr_220px]">
                    <div className="relative">
                        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                        <Input
                            className="pl-9"
                            placeholder="Buscar por nombre o correo…"
                            value={busqueda}
                            onChange={(e) => setBusqueda(e.target.value)}
                        />
                    </div>
                    <Select value={rol} onValueChange={setRol}>
                        <SelectTrigger>
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value={TODOS}>Todos los roles</SelectItem>
                            <SelectItem value="administrador">Administrador</SelectItem>
                            <SelectItem value="jefe_area">Jefe de área</SelectItem>
                            <SelectItem value="administrativo">Administrativo</SelectItem>
                        </SelectContent>
                    </Select>
                </CardContent>
            </Card>

            <Card className="overflow-hidden py-0">
                <Table>
                    <TableHeader>
                        <TableRow className="bg-secondary/60">
                            <TableHead>Nombre</TableHead>
                            <TableHead>Correo institucional</TableHead>
                            <TableHead>Área</TableHead>
                            <TableHead>Rol</TableHead>
                            <TableHead>Estado</TableHead>
                            <TableHead className="text-right">Acciones</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {lista.map((u) => (
                            <TableRow key={u.id}>
                                <TableCell className="font-medium">{u.nombre}</TableCell>
                                <TableCell className="text-sm">{u.correo}</TableCell>
                                <TableCell className="text-sm">{nombreArea(u.areaId)}</TableCell>
                                <TableCell className="text-sm">{etiquetaRol[u.rol]}</TableCell>
                                <TableCell>
                                    <ActivoBadge activo={u.activo} />
                                </TableCell>
                                <TableCell>
                                    <div className="flex justify-end gap-2">
                                        <Button size="sm" variant="outline" onClick={() => abrirEditar(u)}>
                                            Editar
                                        </Button>
                                        <Button
                                            size="sm"
                                            variant="ghost"
                                            onClick={() => {
                                                alternarUsuario(u.id);
                                                toast.success(u.activo ? "Usuario desactivado" : "Usuario activado");
                                            }}
                                        >
                                            {u.activo ? "Desactivar" : "Activar"}
                                        </Button>
                                    </div>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </Card>

            <Dialog open={abierto} onOpenChange={setAbierto}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>{form.id ? "Editar usuario" : "Crear usuario"}</DialogTitle>
                        <DialogDescription>
                            Las cuentas son creadas por el administrador; no existe registro público.
                        </DialogDescription>
                    </DialogHeader>
                    <div className="space-y-4">
                        <div className="space-y-1.5">
                            <Label>Nombre completo</Label>
                            <Input value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} />
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
                                    <SelectContent>
                                        {areas.map((a) => (
                                            <SelectItem key={a.id} value={a.id}>
                                                {a.nombre}
                                            </SelectItem>
                                        ))}
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
                                    <SelectContent>
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
                                <SelectContent>
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

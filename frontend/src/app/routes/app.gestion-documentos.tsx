import { Link, createFileRoute } from "@tanstack/react-router";
import { Eye, History, Pencil, Search, Upload } from "lucide-react";
import { useMemo, useState } from "react";
import { toast } from "sonner";

import { AppShell } from "@/components/AppShell";
import { AreasAutorizadas } from "@/components/AreasAutorizadas";
import { DialogNuevaVersion } from "@/components/DialogNuevaVersion";
import { EstadoBadge } from "@/components/EstadoBadge";
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
import { Textarea } from "@/components/ui/textarea";
import type { Documento, Estado } from "@/lib/data";
import { useIntranet } from "@/lib/store";

export const Route = createFileRoute("/app/gestion-documentos")({
    head: () => ({
        meta: [
            { title: "Gestión de documentos — Intranet documental" },
            {
                name: "description",
                content: "Administra documentos internos: edita información, publica nuevas versiones y cambia su estado.",
            },
            { property: "og:title", content: "Gestión de documentos — Intranet documental" },
            { property: "og:description", content: "Edita, versiona y cambia el estado de los documentos internos." },
        ],
    }),
    component: GestionDocumentos,
});

const TODOS = "todos";

function GestionDocumentos() {
    const { documentos, areas, nombreArea, nombreTipo, nombreCategoria, actualizarDocumento, permisos } = useIntranet();

    const [busqueda, setBusqueda] = useState("");
    const [area, setArea] = useState(TODOS);
    const [estado, setEstado] = useState(TODOS);
    const [editar, setEditar] = useState<Documento | null>(null);
    const [versionar, setVersionar] = useState<Documento | null>(null);
    const [historial, setHistorial] = useState<Documento | null>(null);

    const lista = useMemo(
        () =>
            documentos.filter((d) => {
                const q = busqueda.trim().toLowerCase();
                if (q && !d.nombre.toLowerCase().includes(q)) return false;
                if (area !== TODOS && d.areaId !== area) return false;
                if (estado !== TODOS && d.estado !== estado) return false;
                return true;
            }),
        [documentos, busqueda, area, estado],
    );

    if (!permisos.actualizarDocumentos) {
        return (
            <AppShell titulo="Gestión de documentos">
                <Card>
                    <CardContent className="py-14 text-center text-sm text-muted-foreground">
                        No cuentas con permisos para administrar documentos.
                    </CardContent>
                </Card>
            </AppShell>
        );
    }

    return (
        <AppShell
            titulo="Gestión de documentos"
            descripcion={`${documentos.length} documentos registrados`}
            acciones={
                <Button asChild size="sm" className="gap-1.5">
                    <Link to="/app/publicar">
                        <Upload className="size-4" /> Publicar documento
                    </Link>
                </Button>
            }
        >
            <Card>
                <CardContent className="grid gap-3 py-5 md:grid-cols-[1fr_200px_200px]">
                    <div className="relative">
                        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                        <Input
                            className="pl-9"
                            placeholder="Buscar documento…"
                            value={busqueda}
                            onChange={(e) => setBusqueda(e.target.value)}
                        />
                    </div>
                    <Select value={area} onValueChange={setArea}>
                        <SelectTrigger>
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value={TODOS}>Todas las áreas</SelectItem>
                            {areas.map((a) => (
                                <SelectItem key={a.id} value={a.id}>
                                    {a.nombre}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                    <Select value={estado} onValueChange={setEstado}>
                        <SelectTrigger>
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value={TODOS}>Todos los estados</SelectItem>
                            <SelectItem value="publicado">Publicado</SelectItem>
                            <SelectItem value="borrador">Borrador</SelectItem>
                            <SelectItem value="inactivo">Inactivo</SelectItem>
                        </SelectContent>
                    </Select>
                </CardContent>
            </Card>

            <Card className="overflow-hidden py-0">
                <Table>
                    <TableHeader>
                        <TableRow className="bg-secondary/60">
                            <TableHead>Documento</TableHead>
                            <TableHead>Área responsable</TableHead>
                            <TableHead>Categoría</TableHead>
                            <TableHead>Tipo</TableHead>
                            <TableHead>Versión vigente</TableHead>
                            <TableHead>Estado</TableHead>
                            <TableHead>Publicación</TableHead>
                            <TableHead className="text-right">Acciones</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {lista.map((d) => (
                            <TableRow key={d.id}>
                                <TableCell className="max-w-xs">
                                    <p className="truncate font-medium">{d.nombre}</p>
                                    <p className="truncate text-xs text-muted-foreground">{d.archivo}</p>
                                </TableCell>
                                <TableCell className="text-sm">{nombreArea(d.areaId)}</TableCell>
                                <TableCell className="text-sm">{nombreCategoria(d.categoriaId)}</TableCell>
                                <TableCell className="text-sm">{nombreTipo(d.tipoId)}</TableCell>
                                <TableCell className="text-sm font-medium">v{d.version}</TableCell>
                                <TableCell>
                                    <Select
                                        value={d.estado}
                                        onValueChange={(v) => {
                                            actualizarDocumento(d.id, { estado: v as Estado });
                                            toast.success(`Estado actualizado a ${v}`);
                                        }}
                                    >
                                        <SelectTrigger className="h-8 w-[130px]">
                                            <SelectValue />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="publicado">Publicado</SelectItem>
                                            <SelectItem value="borrador">Borrador</SelectItem>
                                            <SelectItem value="inactivo">Inactivo</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </TableCell>
                                <TableCell className="text-sm">{d.fechaPublicacion}</TableCell>
                                <TableCell>
                                    <div className="flex justify-end gap-1">
                                        <Button asChild size="icon" variant="ghost" title="Ver detalles">
                                            <Link to="/app/documento/$id" params={{ id: d.id }}>
                                                <Eye className="size-4" />
                                            </Link>
                                        </Button>
                                        <Button size="icon" variant="ghost" title="Editar información" onClick={() => setEditar(d)}>
                                            <Pencil className="size-4" />
                                        </Button>
                                        <Button size="icon" variant="ghost" title="Publicar nueva versión" onClick={() => setVersionar(d)}>
                                            <Upload className="size-4" />
                                        </Button>
                                        <Button size="icon" variant="ghost" title="Historial de versiones" onClick={() => setHistorial(d)}>
                                            <History className="size-4" />
                                        </Button>
                                    </div>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </Card>

            <DialogEditar doc={editar} onClose={() => setEditar(null)} onSave={actualizarDocumento} />
            <DialogNuevaVersion doc={versionar} onClose={() => setVersionar(null)} />

            <Dialog open={!!historial} onOpenChange={(o) => !o && setHistorial(null)}>
                <DialogContent className="max-w-3xl">
                    <DialogHeader>
                        <DialogTitle>Historial de versiones</DialogTitle>
                        <DialogDescription>{historial?.nombre}</DialogDescription>
                    </DialogHeader>
                    <div className="max-h-80 overflow-y-auto">
                        <Table>
                            <TableHeader>
                                <TableRow>
                                    <TableHead>Versión</TableHead>
                                    <TableHead>Fecha</TableHead>
                                    <TableHead>Publicado por</TableHead>
                                    <TableHead>Descripción de cambios</TableHead>
                                    <TableHead>Estado</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {historial?.versiones.map((v, i) => (
                                    <TableRow key={v.numero}>
                                        <TableCell className="font-medium">v{v.numero}</TableCell>
                                        <TableCell className="text-sm">{v.fecha}</TableCell>
                                        <TableCell className="text-sm">{v.autor}</TableCell>
                                        <TableCell className="text-sm text-muted-foreground">{v.notas}</TableCell>
                                        <TableCell className="text-sm">{i === 0 ? "Vigente" : "Histórica"}</TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </div>
                </DialogContent>
            </Dialog>
        </AppShell>
    );
}

function DialogEditar({
                          doc,
                          onClose,
                          onSave,
                      }: {
    doc: Documento | null;
    onClose: () => void;
    onSave: (id: string, cambios: Partial<Documento>) => void;
}) {
    const { areas, categorias, tipos } = useIntranet();
    const [nombre, setNombre] = useState("");
    const [descripcion, setDescripcion] = useState("");
    const [areaId, setAreaId] = useState("");
    const [categoriaId, setCategoriaId] = useState("");
    const [tipoId, setTipoId] = useState("");
    const [visibleTodas, setVisibleTodas] = useState(false);
    const [autorizadas, setAutorizadas] = useState<string[]>([]);
    const [cargado, setCargado] = useState<string | null>(null);

    if (doc && cargado !== doc.id) {
        setCargado(doc.id);
        setNombre(doc.nombre);
        setDescripcion(doc.descripcion);
        setAreaId(doc.areaId);
        setCategoriaId(doc.categoriaId);
        setTipoId(doc.tipoId);
        setVisibleTodas(doc.visibleTodas);
        setAutorizadas(doc.areasAutorizadas);
    }

    return (
        <Dialog open={!!doc} onOpenChange={(o) => !o && onClose()}>
            <DialogContent className="max-h-[85vh] max-w-2xl overflow-y-auto">
                <DialogHeader>
                    <DialogTitle>Editar información</DialogTitle>
                    <DialogDescription>Actualiza los datos del documento sin modificar sus versiones.</DialogDescription>
                </DialogHeader>
                <div className="space-y-5">
                    <div className="space-y-1.5">
                        <Label>Nombre</Label>
                        <Input value={nombre} onChange={(e) => setNombre(e.target.value)} />
                    </div>
                    <div className="space-y-1.5">
                        <Label>Descripción</Label>
                        <Textarea rows={3} value={descripcion} onChange={(e) => setDescripcion(e.target.value)} />
                    </div>
                    <div className="grid gap-3 sm:grid-cols-3">
                        <SelectorSimple label="Área responsable" value={areaId} onChange={setAreaId} opciones={areas} />
                        <SelectorSimple label="Categoría" value={categoriaId} onChange={setCategoriaId} opciones={categorias} />
                        <SelectorSimple label="Tipo" value={tipoId} onChange={setTipoId} opciones={tipos} />
                    </div>
                    <div className="space-y-2">
                        <Label>Áreas autorizadas para visualizar</Label>
                        <AreasAutorizadas
                            idCheckbox="editar-visible-todas"
                            areas={areas}
                            seleccionadas={autorizadas}
                            onChange={setAutorizadas}
                            visibleTodas={visibleTodas}
                            onVisibleTodas={setVisibleTodas}
                        />
                    </div>
                </div>
                <DialogFooter>
                    <Button variant="outline" onClick={onClose}>
                        Cancelar
                    </Button>
                    <Button
                        onClick={() => {
                            if (!doc) return;
                            if (!nombre.trim()) return toast.error("El nombre es obligatorio.");
                            if (!visibleTodas && autorizadas.length === 0)
                                return toast.error("Agrega al menos un área autorizada.");
                            onSave(doc.id, {
                                nombre,
                                descripcion,
                                areaId,
                                categoriaId,
                                tipoId,
                                visibleTodas,
                                areasAutorizadas: visibleTodas ? [] : autorizadas,
                            });
                            toast.success("Documento actualizado");
                            onClose();
                        }}
                    >
                        Guardar cambios
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}

function SelectorSimple({
                            label,
                            value,
                            onChange,
                            opciones,
                        }: {
    label: string;
    value: string;
    onChange: (v: string) => void;
    opciones: { id: string; nombre: string }[];
}) {
    return (
        <div className="space-y-1.5">
            <Label className="text-xs text-muted-foreground">{label}</Label>
            <Select value={value} onValueChange={onChange}>
                <SelectTrigger className="w-full">
                    <SelectValue />
                </SelectTrigger>
                <SelectContent>
                    {opciones.map((o) => (
                        <SelectItem key={o.id} value={o.id}>
                            {o.nombre}
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>
        </div>
    );
}

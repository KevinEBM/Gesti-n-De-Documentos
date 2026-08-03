import { Link, createFileRoute } from "@tanstack/react-router";
import {
    ArrowLeft,
    Clock,
    Download,
    Eye,
    FileText,
    History,
    Pencil,
    Search,
    Upload
} from "lucide-react";
import { useMemo, useState } from "react";
import { toast } from "sonner";

import { AppShell } from "@/components/AppShell";
import { AreasAutorizadas } from "@/components/AreasAutorizadas";
import { DialogNuevaVersion } from "@/components/DialogNuevaVersion";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
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

    // Modales de acción
    const [editar, setEditar] = useState<Documento | null>(null);
    const [versionar, setVersionar] = useState<Documento | null>(null);
    const [historial, setHistorial] = useState<Documento | null>(null);

    // Vistas adicionales
    const [documentoSeleccionado, setDocumentoSeleccionado] = useState<Documento | null>(null);
    const [previsualizar, setPrevisualizar] = useState<Documento | null>(null);

    const lista = useMemo(
        () =>
            documentos.filter((d) => {
                const q = busqueda.trim().toLowerCase();
                if (q && !d.nombre.toLowerCase().includes(q) && !d.archivo.toLowerCase().includes(q)) return false;
                if (area !== TODOS && d.areaId !== area) return false;
                if (estado !== TODOS && d.estado !== estado) return false;
                return true;
            }),
        [documentos, busqueda, area, estado],
    );

    // Función de descarga compatible con Microsoft Word y visores PDF/Texto
    const ejecutarDescarga = (nombreDoc: string, archivoNombre: string, versionTag: string) => {
        const ext = archivoNombre.split(".").pop()?.toLowerCase();
        let blob: Blob;
        const nombreFinal = `${versionTag}_${archivoNombre}`;

        if (ext === "docx" || ext === "doc") {
            // Genera una estructura RTF válida para que Microsoft Word lo abra nativamente sin alertas de corrupción
            const rtfContent = `{\\rtf1\\ansi\\deff0 {\\fonttbl {\\f0 Arial;}}
\\f0\\fs24 \\b INSTITUCI\\'d3N / GESTI\\'d3N DOCUMENTAL\\b0\\par\\par
\\b Documento:\\b0  ${nombreDoc}\\par
\\b Versi\\'f3n:\\b0  ${versionTag}\\par
\\b Archivo:\\b0  ${archivoNombre}\\par
\\b Fecha de Descarga:\\b0  ${new Date().toLocaleString()}\\par
\\line
--------------------------------------------------\\par
[Contenido del documento listo para producci\\'f3n]\\par
}`;
            blob = new Blob([rtfContent], { type: "application/msword" });
        } else {
            // Contenido en texto plano para otros tipos de archivo
            const contenido = `================================================
INSTITUCIÓN / GESTIÓN DOCUMENTAL
Documento: ${nombreDoc}
Versión: ${versionTag}
Archivo: ${archivoNombre}
Fecha de Descarga: ${new Date().toLocaleString()}
================================================

[Contenido del documento listo para producción]`;
            blob = new Blob([contenido], { type: "text/plain;charset=utf-8" });
        }

        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = nombreFinal;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);

        toast.success(`Descargando: ${nombreFinal}`);
    };

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

    // VISTA DE DETALLE COMPLETO DEL DOCUMENTO
    if (documentoSeleccionado) {
        const totalAreasAutorizadas = documentoSeleccionado.visibleTodas
            ? areas.length
            : documentoSeleccionado.areasAutorizadas.length;

        const textoAreasAutorizadas = documentoSeleccionado.visibleTodas
            ? "Todas las áreas"
            : documentoSeleccionado.areasAutorizadas.map((id) => nombreArea(id)).join(", ");

        return (
            <AppShell titulo="Detalle del documento">
                <div className="space-y-6">
                    <div>
                        <Button
                            variant="ghost"
                            size="sm"
                            className="gap-2"
                            onClick={() => setDocumentoSeleccionado(null)}
                        >
                            <ArrowLeft className="size-4" /> Volver a la biblioteca
                        </Button>
                    </div>

                    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                        {/* Información Principal */}
                        <Card className="lg:col-span-2">
                            <CardContent className="p-6 space-y-6">
                                <div className="flex items-start justify-between gap-4">
                                    <div className="flex items-start gap-3">
                                        <div className="p-2 bg-secondary rounded-lg text-secondary-foreground mt-1">
                                            <FileText className="size-6" />
                                        </div>
                                        <div>
                                            <h1 className="text-xl font-bold">{documentoSeleccionado.nombre}</h1>
                                            <p className="text-xs text-muted-foreground font-mono mt-0.5">
                                                {documentoSeleccionado.archivo}
                                            </p>
                                        </div>
                                    </div>
                                    <Select
                                        value={documentoSeleccionado.estado}
                                        onValueChange={(v) => {
                                            const nuevoEstado = v as Estado;
                                            actualizarDocumento(documentoSeleccionado.id, { estado: nuevoEstado });
                                            setDocumentoSeleccionado({ ...documentoSeleccionado, estado: nuevoEstado });
                                            toast.success(`Estado actualizado a ${nuevoEstado}`);
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
                                </div>

                                <p className="text-sm text-muted-foreground">{documentoSeleccionado.descripcion}</p>

                                <div className="grid grid-cols-2 gap-y-4 gap-x-6 text-xs pt-4 border-t">
                                    <div>
                                        <span className="text-muted-foreground font-semibold uppercase block mb-1">
                                            Área Responsable
                                        </span>
                                        <span className="font-medium text-sm">
                                            {nombreArea(documentoSeleccionado.areaId)}
                                        </span>
                                    </div>

                                    <div>
                                        <span className="text-muted-foreground font-semibold uppercase block mb-1">
                                            Categoría
                                        </span>
                                        <span className="font-medium text-sm">
                                            {nombreCategoria(documentoSeleccionado.categoriaId)}
                                        </span>
                                    </div>

                                    <div>
                                        <span className="text-muted-foreground font-semibold uppercase block mb-1">
                                            Tipo de Documento
                                        </span>
                                        <span className="font-medium text-sm">
                                            {nombreTipo(documentoSeleccionado.tipoId)}
                                        </span>
                                    </div>

                                    <div>
                                        <span className="text-muted-foreground font-semibold uppercase block mb-1">
                                            Versión Vigente
                                        </span>
                                        <span className="font-medium text-sm font-mono">
                                            v{documentoSeleccionado.version}
                                        </span>
                                    </div>

                                    <div>
                                        <span className="text-muted-foreground font-semibold uppercase block mb-1">
                                            Fecha de Publicación
                                        </span>
                                        <span className="font-medium text-sm">
                                            {documentoSeleccionado.fechaPublicacion}
                                        </span>
                                    </div>

                                    <div>
                                        <span className="text-muted-foreground font-semibold uppercase block mb-1">
                                            Áreas Autorizadas
                                        </span>
                                        <span className="font-medium text-sm">{textoAreasAutorizadas}</span>
                                    </div>

                                    <div>
                                        <span className="text-muted-foreground font-semibold uppercase block mb-1">
                                            Total de Áreas
                                        </span>
                                        <span className="font-medium text-sm">{totalAreasAutorizadas}</span>
                                    </div>
                                </div>
                            </CardContent>
                        </Card>

                        {/* Lateral de Acciones */}
                        <Card className="flex flex-col justify-between">
                            <CardHeader>
                                <CardTitle className="text-base font-bold">Acciones</CardTitle>
                            </CardHeader>
                            <CardContent className="space-y-3">
                                <Button
                                    variant="outline"
                                    className="w-full justify-between"
                                    onClick={() => setPrevisualizar(documentoSeleccionado)}
                                >
                                    <span>Visualizar versión vigente</span>
                                    <Eye className="size-4" />
                                </Button>

                                <Button
                                    variant="outline"
                                    className="w-full justify-between"
                                    onClick={() =>
                                        ejecutarDescarga(
                                            documentoSeleccionado.nombre,
                                            documentoSeleccionado.archivo,
                                            `v${documentoSeleccionado.version}`
                                        )
                                    }
                                >
                                    <span>Descargar</span>
                                    <Download className="size-4" />
                                </Button>
                            </CardContent>
                            <div className="p-6 pt-0 text-xs text-center text-muted-foreground border-t mt-auto">
                                {documentoSeleccionado.versiones?.length || 1} versión(es) registrada(s) en el historial.
                            </div>
                        </Card>
                    </div>

                    {/* Historial de versiones del documento actual */}
                    <Card>
                        <CardHeader className="flex-row items-center gap-2">
                            <Clock className="size-5 text-muted-foreground" />
                            <CardTitle className="text-base font-bold">Historial de versiones</CardTitle>
                        </CardHeader>
                        <CardContent className="p-0">
                            <Table>
                                <TableHeader>
                                    <TableRow className="bg-secondary/60">
                                        <TableHead>Versión</TableHead>
                                        <TableHead>Fecha</TableHead>
                                        <TableHead>Publicado por</TableHead>
                                        <TableHead>Descripción de cambios</TableHead>
                                        <TableHead>Estado</TableHead>
                                        <TableHead className="text-right">Acción</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {documentoSeleccionado.versiones.map((v, i) => (
                                        <TableRow key={v.numero}>
                                            <TableCell className="font-medium font-mono">v{v.numero}</TableCell>
                                            <TableCell className="text-sm">{v.fecha}</TableCell>
                                            <TableCell className="text-sm">{v.autor}</TableCell>
                                            <TableCell className="text-sm text-muted-foreground">{v.notas}</TableCell>
                                            <TableCell className="text-sm">
                                                {i === 0 ? (
                                                    <span className="font-semibold text-emerald-600">Vigente</span>
                                                ) : (
                                                    "Histórica"
                                                )}
                                            </TableCell>
                                            <TableCell className="text-right">
                                                <Button
                                                    size="icon"
                                                    variant="ghost"
                                                    title="Descargar versión"
                                                    onClick={() =>
                                                        ejecutarDescarga(
                                                            documentoSeleccionado.nombre,
                                                            v.archivo || documentoSeleccionado.archivo,
                                                            `v${v.numero}`
                                                        )
                                                    }
                                                >
                                                    <Download className="size-4" />
                                                </Button>
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        </CardContent>
                    </Card>
                </div>

                {/* Dialog Previsualizador */}
                <DialogPrevisualizar doc={previsualizar} onClose={() => setPrevisualizar(null)} />
            </AppShell>
        );
    }

    // VISTA PRINCIPAL (TABLA BIBLIOTECA)
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
                                    <p
                                        className="truncate font-medium cursor-pointer hover:underline text-primary"
                                        onClick={() => setDocumentoSeleccionado(d)}
                                    >
                                        {d.nombre}
                                    </p>
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
                                        <Button
                                            size="icon"
                                            variant="ghost"
                                            title="Ver detalles"
                                            onClick={() => setDocumentoSeleccionado(d)}
                                        >
                                            <Eye className="size-4" />
                                        </Button>
                                        <Button
                                            size="icon"
                                            variant="ghost"
                                            title="Editar información"
                                            onClick={() => setEditar(d)}
                                        >
                                            <Pencil className="size-4" />
                                        </Button>
                                        <Button
                                            size="icon"
                                            variant="ghost"
                                            title="Publicar nueva versión"
                                            onClick={() => setVersionar(d)}
                                        >
                                            <Upload className="size-4" />
                                        </Button>
                                        <Button
                                            size="icon"
                                            variant="ghost"
                                            title="Historial de versiones"
                                            onClick={() => setHistorial(d)}
                                        >
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

            {/* Modal de Historial en la tabla principal */}
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
                                    <TableHead className="text-right">Acción</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {historial?.versiones.map((v, i) => (
                                    <TableRow key={v.numero}>
                                        <TableCell className="font-medium font-mono">v{v.numero}</TableCell>
                                        <TableCell className="text-sm">{v.fecha}</TableCell>
                                        <TableCell className="text-sm">{v.autor}</TableCell>
                                        <TableCell className="text-sm text-muted-foreground">{v.notas}</TableCell>
                                        <TableCell className="text-sm">{i === 0 ? "Vigente" : "Histórica"}</TableCell>
                                        <TableCell className="text-right">
                                            <Button
                                                size="icon"
                                                variant="ghost"
                                                title="Descargar"
                                                onClick={() =>
                                                    ejecutarDescarga(historial.nombre, v.archivo || historial.archivo, `v${v.numero}`)
                                                }
                                            >
                                                <Download className="size-4" />
                                            </Button>
                                        </TableCell>
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

function DialogPrevisualizar({ doc, onClose }: { doc: Documento | null; onClose: () => void }) {
    return (
        <Dialog open={!!doc} onOpenChange={(o) => !o && onClose()}>
            <DialogContent className="max-w-3xl">
                <DialogHeader>
                    <DialogTitle>Previsualización del documento</DialogTitle>
                    <DialogDescription>{doc?.nombre} — Version v{doc?.version}</DialogDescription>
                </DialogHeader>
                <div className="bg-muted p-6 rounded-lg border text-sm font-mono space-y-3 min-h-[200px]">
                    <p className="font-semibold border-b pb-2">Visualizador interno de documentos v1.0</p>
                    <p><strong>Archivo:</strong> {doc?.archivo}</p>
                    <p><strong>Descripción:</strong> {doc?.descripcion}</p>
                    <div className="mt-4 p-4 bg-background border rounded text-xs leading-relaxed text-muted-foreground">
                        Este es el contenido simulado del documento. En un entorno de producción, aquí se renderizará el visor interactivo de PDF o el archivo binario almacenado.
                    </div>
                </div>
                <DialogFooter>
                    <Button onClick={onClose}>Cerrar vista previa</Button>
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
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
    Upload,
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
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table";
import { Textarea } from "@/components/ui/textarea";
import type { Documento, Estado } from "@/lib/data";
import { useIntranet } from "@/lib/store";
import { obtenerIconoArea } from "@/lib/iconos-areas";
import { obtenerIconoFormato } from "@/lib/iconos-formatos";
import { obtenerIconoSubProceso } from "@/lib/iconos-subprocesos";

export const Route = createFileRoute("/app/gestion-documentos")({
    head: () => ({
        meta: [
            { title: "Gestión de documentos — Intranet documental" },
            {
                name: "description",
                content:
                    "Administra documentos internos: edita información, publica nuevas versiones y cambia su estado.",
            },
            {
                property: "og:title",
                content: "Gestión de documentos — Intranet documental",
            },
            {
                property: "og:description",
                content:
                    "Edita, versiona y cambia el estado de los documentos internos.",
            },
        ],
    }),
    component: GestionDocumentos,
});

const TODOS = "todos";

function GestionDocumentos() {
    const {
        documentos,
        areas,
        nombreArea,
        nombreTipo,
        nombreSub_Proceso,
        actualizarDocumento,
        permisos,
    } = useIntranet();

    const [busqueda, setBusqueda] = useState("");
    const [area, setArea] = useState(TODOS);
    const [estado, setEstado] = useState(TODOS);

    const [editar, setEditar] = useState<Documento | null>(null);
    const [versionar, setVersionar] = useState<Documento | null>(null);
    const [historial, setHistorial] = useState<Documento | null>(null);

    const [documentoSeleccionado, setDocumentoSeleccionado] =
        useState<Documento | null>(null);

    const [previsualizar, setPrevisualizar] =
        useState<Documento | null>(null);

    const lista = useMemo(
        () =>
            documentos.filter((d) => {
                const q = busqueda.trim().toLowerCase();

                if (
                    q &&
                    !d.nombre.toLowerCase().includes(q) &&
                    !d.archivo.toLowerCase().includes(q) &&
                    !d.codigo.toLowerCase().includes(q)
                )
                    return false;

                if (area !== TODOS && d.areaId !== area) return false;
                if (estado !== TODOS && d.estado !== estado) return false;

                return true;
            }),
        [documentos, busqueda, area, estado],
    );

    const ejecutarDescarga = (
        nombreDoc: string,
        archivoNombre: string,
        versionTag: string,
    ) => {
        const ext = archivoNombre.split(".").pop()?.toLowerCase();

        let blob: Blob;

        const nombreFinal = `${versionTag}_${archivoNombre}`;

        if (ext === "doc" || ext === "docx") {
            const contenido = `{\\rtf1\\ansi\\deff0
    {\\fonttbl{\\f0 Arial;}}
    
    \\fs22
    \\b SISTEMA DE GESTIÓN DOCUMENTAL\\b0\\par
    \\par
    \\b Documento:\\b0 ${nombreDoc}\\par
    \\b Versión:\\b0 ${versionTag}\\par
    \\b Archivo:\\b0 ${archivoNombre}\\par
    \\b Fecha:\\b0 ${new Date().toLocaleString()}\\par
    \\par
    ------------------------------------------------------------\\par
    Documento de demostración generado automáticamente.\\par
    }`;

            blob = new Blob([contenido], {
                type: "application/msword",
            });
        } else {
            const contenido = `SISTEMA DE GESTIÓN DOCUMENTAL
    
    Documento : ${nombreDoc}
    Versión    : ${versionTag}
    Archivo    : ${archivoNombre}
    Fecha      : ${new Date().toLocaleString()}
    
    --------------------------------------------------------
    
    Documento de demostración generado automáticamente.
    `;

            blob = new Blob([contenido], {
                type: "text/plain;charset=utf-8",
            });
        }

        const url = URL.createObjectURL(blob);

        const link = document.createElement("a");

        link.href = url;
        link.download = nombreFinal;

        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        URL.revokeObjectURL(url);

        toast.success(`Descargando ${nombreFinal}`);
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

    if (documentoSeleccionado) {
        const totalAreas = documentoSeleccionado.visibleTodas
            ? areas.length
            : documentoSeleccionado.areasAutorizadas.length;

        const textoAreas = documentoSeleccionado.visibleTodas
            ? "Todas las áreas"
            : documentoSeleccionado.areasAutorizadas
                .map((id) => nombreArea(id))
                .join(", ");

        return (
            <AppShell titulo="Detalle del documento">
                <div className="space-y-6">

                    <Button
                        variant="ghost"
                        size="sm"
                        className="gap-2"
                        onClick={() => setDocumentoSeleccionado(null)}
                    >
                        <ArrowLeft className="size-4" />
                        Volver
                    </Button>

                    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

                        <Card className="lg:col-span-2">

                            <CardContent className="p-6 space-y-6">

                                <div className="flex items-start justify-between">

                                    <div className="flex gap-3">

                                        <div className="rounded-lg bg-secondary p-2">
                                            <FileText className="size-6" />
                                        </div>

                                        <div>

                                            <h1 className="text-xl font-bold">
                                                {documentoSeleccionado.nombre}
                                            </h1>

                                            <p className="font-mono text-xs text-muted-foreground">
                                                {documentoSeleccionado.archivo}
                                            </p>

                                        </div>

                                    </div>

                                    <Select
                                        value={documentoSeleccionado.estado}
                                        onValueChange={(v) => {
                                            actualizarDocumento(
                                                documentoSeleccionado.id,
                                                {
                                                    estado: v as Estado,
                                                },
                                            );

                                            setDocumentoSeleccionado({
                                                ...documentoSeleccionado,
                                                estado: v as Estado,
                                            });

                                            toast.success(
                                                "Estado actualizado.",
                                            );
                                        }}
                                    >
                                        <SelectTrigger className="w-[150px]">
                                            <SelectValue />
                                        </SelectTrigger>

                                        <SelectContent>
                                            <SelectItem value="publicado">
                                                Publicado
                                            </SelectItem>

                                            <SelectItem value="borrador">
                                                Borrador
                                            </SelectItem>

                                            <SelectItem value="inactivo">
                                                Inactivo
                                            </SelectItem>
                                        </SelectContent>
                                    </Select>

                                </div>

                                <p className="text-sm text-muted-foreground">
                                    {documentoSeleccionado.descripcion}
                                </p>

                                <div className="grid grid-cols-2 gap-5 border-t pt-5">

                                    <Dato titulo="Código">
                                            <span className="font-mono">
                                                {documentoSeleccionado.codigo}
                                            </span>
                                    </Dato>

                                    <Dato titulo="Área responsable">
                                        {nombreArea(documentoSeleccionado.areaId)}
                                    </Dato>

                                    <Dato titulo="Subproceso">
                                        {nombreSub_Proceso(
                                            documentoSeleccionado.subProcesoId,
                                        )}
                                    </Dato>

                                    <Dato titulo="Tipo">
                                        {nombreTipo(documentoSeleccionado.tipoId)}
                                    </Dato>

                                    <Dato titulo="Versión vigente">
                                        v{documentoSeleccionado.version}
                                    </Dato>

                                    <Dato titulo="Fecha de publicación">
                                        {new Date(
                                            documentoSeleccionado.fechaPublicacion,
                                        ).toLocaleDateString("es-CO", {
                                            day: "2-digit",
                                            month: "long",
                                            year: "numeric",
                                        })}
                                    </Dato>

                                    <Dato titulo="Áreas autorizadas">
                                        {textoAreas}
                                    </Dato>

                                    <Dato titulo="Total de áreas">
                                        {totalAreas}
                                    </Dato>

                                </div>

                            </CardContent>

                        </Card>

                        <Card className="flex flex-col">

                            <CardHeader>
                                <CardTitle>Acciones</CardTitle>
                            </CardHeader>

                            <CardContent className="space-y-3">

                                <Button
                                    variant="outline"
                                    className="justify-between"
                                    onClick={() =>
                                        setPrevisualizar(
                                            documentoSeleccionado,
                                        )
                                    }
                                >
                                    Visualizar
                                    <Eye className="size-4" />
                                </Button>

                                <Button
                                    variant="outline"
                                    className="justify-between"
                                    onClick={() =>
                                        ejecutarDescarga(
                                            documentoSeleccionado.nombre,
                                            documentoSeleccionado.archivo,
                                            `v${documentoSeleccionado.version}`,
                                        )
                                    }
                                >
                                    Descargar
                                    <Download className="size-4" />
                                </Button>

                            </CardContent>

                            <div className="border-t p-6 text-center text-xs text-muted-foreground">

                                Historial:
                                {" "}
                                {documentoSeleccionado.versiones.length}
                                {" "}
                                versión(es)

                            </div>

                        </Card>

                    </div>
                    {/* Historial de versiones */}

                    <Card>

                        <CardHeader className="flex flex-row items-center gap-2">

                            <Clock className="size-5 text-muted-foreground" />

                            <CardTitle>
                                Historial de versiones
                            </CardTitle>

                        </CardHeader>

                        <CardContent className="p-0">

                            <div className="overflow-x-auto">

                                <Table className="min-w-[2200px]">

                                    <TableHeader>

                                        <TableRow className="bg-secondary/60">

                                            <TableHead>Código</TableHead>
                                            <TableHead>Versión</TableHead>
                                            <TableHead>Fecha</TableHead>
                                            <TableHead>Publicado por</TableHead>
                                            <TableHead>Descripción de cambios</TableHead>
                                            <TableHead>Estado</TableHead>
                                            <TableHead className="text-right">
                                                Acción
                                            </TableHead>

                                        </TableRow>

                                    </TableHeader>

                                    <TableBody>

                                        {documentoSeleccionado.versiones.map(
                                            (v, indice) => (
                                                <TableRow key={v.numero}>

                                                    <TableCell className="font-mono">
                                                        {documentoSeleccionado.codigo}
                                                    </TableCell>

                                                    <TableCell className="font-mono">
                                                        v{v.numero}
                                                    </TableCell>

                                                    <TableCell className="w-[170px]">
                                                        {new Date(
                                                            v.fecha,
                                                        ).toLocaleDateString(
                                                            "es-CO",
                                                            {
                                                                day: "2-digit",
                                                                month: "long",
                                                                year: "numeric",
                                                            },
                                                        )}
                                                    </TableCell>

                                                    <TableCell className="w-[170px]">
                                                        {v.autor}
                                                    </TableCell>

                                                    <TableCell className="text-muted-foreground">
                                                        {v.notas}
                                                    </TableCell>

                                                    <TableCell className="w-[170px]">

                                                        {indice === 0 ? (
                                                            <span className="font-semibold text-emerald-600">
                                                                    Vigente
                                                                </span>
                                                        ) : (
                                                            <span className="text-muted-foreground">
                                                                    Histórica
                                                                </span>
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
                                                                    v.archivo ??
                                                                    documentoSeleccionado.archivo,
                                                                    `v${v.numero}`,
                                                                )
                                                            }
                                                        >
                                                            <Download className="size-4" />
                                                        </Button>

                                                    </TableCell>

                                                </TableRow>
                                            ),
                                        )}

                                    </TableBody>

                                </Table>

                            </div>

                        </CardContent>

                    </Card>

                </div>

                <DialogPrevisualizar
                    doc={previsualizar}
                    onClose={() => setPrevisualizar(null)}
                />

            </AppShell>
        );
    }

    return (

        <AppShell
            titulo="Gestión de documentos"
            descripcion={`${documentos.length} documentos publicados`}
            acciones={
                <Button
                    asChild
                    size="sm"
                    className="gap-2"
                >
                    <Link to="/app/publicar">
                        <Upload className="size-4" />
                        Publicar documento
                    </Link>
                </Button>
            }
        >

            <Card>

                <CardContent className="grid gap-3 py-5 md:grid-cols-[1fr_220px_220px]">

                    <div className="relative">

                        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />

                        <Input
                            className="pl-9"
                            placeholder="Buscar por código, nombre o archivo..."
                            value={busqueda}
                            onChange={(e) => setBusqueda(e.target.value)}
                        />

                    </div>

                    <Select
                        value={area}
                        onValueChange={setArea}
                    >

                        <SelectTrigger>
                            <SelectValue placeholder="Área" />
                        </SelectTrigger>

                        <SelectContent>

                            <SelectItem value={TODOS}>
                                Todas las áreas
                            </SelectItem>

                            {areas.map((a) => {

                                const {
                                    icono: Icono,
                                    color,
                                } = obtenerIconoArea(a.nombre);

                                return (
                                    <SelectItem
                                        key={a.id}
                                        value={a.id}
                                    >
                                        <div className="flex items-center gap-2">

                                            <Icono
                                                className={`size-4 ${color}`}
                                            />

                                            {a.nombre}

                                        </div>
                                    </SelectItem>
                                );
                            })}

                        </SelectContent>

                    </Select>

                    <Select
                        value={estado}
                        onValueChange={setEstado}
                    >

                        <SelectTrigger>
                            <SelectValue />
                        </SelectTrigger>

                        <SelectContent>

                            <SelectItem value={TODOS}>
                                Todos los estados
                            </SelectItem>

                            <SelectItem value="publicado">
                                Publicado
                            </SelectItem>

                            <SelectItem value="borrador">
                                Borrador
                            </SelectItem>

                            <SelectItem value="inactivo">
                                Inactivo
                            </SelectItem>

                        </SelectContent>

                    </Select>

                </CardContent>

            </Card>

            <Card className="w-full max-w-full overflow-hidden">

                <CardContent className="p-0">

                    <div className="w-full overflow-x-auto">

                        <Table className="w-full min-w-[1250px]">

                            <TableHeader>

                                <TableRow className="bg-secondary/60">

                                    <TableHead>Código</TableHead>
                                    <TableHead>Documento</TableHead>
                                    <TableHead>Área</TableHead>
                                    <TableHead>Subproceso</TableHead>
                                    <TableHead>Tipo</TableHead>
                                    <TableHead>Versión</TableHead>
                                    <TableHead>Estado</TableHead>
                                    <TableHead>Publicación</TableHead>
                                    <TableHead className="w-[170px] text-right">
                                        Acciones
                                    </TableHead>

                                </TableRow>

                            </TableHeader>

                            <TableBody>

                                {lista.map((d) => (
                                    <TableRow key={d.id}>

                                        <TableCell className="font-mono font-semibold whitespace-nowrap">
                                            {d.codigo}
                                        </TableCell>

                                        <TableCell className="w-[320px]">

                                            <button
                                                type="button"
                                                className="truncate text-left font-medium text-primary hover:underline"
                                                onClick={() => setDocumentoSeleccionado(d)}
                                            >
                                                {d.nombre}
                                            </button>

                                            <p className="truncate text-xs text-muted-foreground">
                                                {d.archivo}
                                            </p>

                                        </TableCell>

                                        <TableCell className="w-[170px]">

                                            {(() => {

                                                const nombre = nombreArea(d.areaId);

                                                const {
                                                    icono: Icono,
                                                    color,
                                                } = obtenerIconoArea(nombre);

                                                return (

                                                    <div className="flex items-center gap-2">

                                                        <Icono
                                                            className={`size-4 ${color}`}
                                                        />

                                                        <span>{nombre}</span>

                                                    </div>

                                                );

                                            })()}

                                        </TableCell>

                                        <TableCell className="w-[170px]">

                                            {(() => {

                                                const nombre =
                                                    nombreSub_Proceso(
                                                        d.subProcesoId,
                                                    );

                                                const {
                                                    icono: Icono,
                                                    color,
                                                } =
                                                    obtenerIconoSubProceso(
                                                        nombre,
                                                    );

                                                return (

                                                    <div className="flex items-center gap-2">

                                                        <Icono
                                                            className={`size-4 ${color}`}
                                                        />

                                                        <span>{nombre}</span>

                                                    </div>

                                                );

                                            })()}

                                        </TableCell>

                                        <TableCell className="w-[170px]">

                                            {(() => {

                                                const nombre = nombreTipo(d.tipoId);

                                                const {
                                                    icono: Icono,
                                                    color,
                                                } = obtenerIconoFormato(nombre);

                                                return (

                                                    <div className="flex items-center gap-2">
                                                        <Icono
                                                            className={`size-4 ${color}`}
                                                        />
                                                        <span>{nombre}</span>
                                                    </div>

                                                );

                                            })()}

                                        </TableCell>

                                        <TableCell className="font-mono">
                                            v{d.version}
                                        </TableCell>

                                        <TableCell className="w-[170px]">

                                            <Select
                                                value={d.estado}
                                                onValueChange={(v) => {

                                                    actualizarDocumento(
                                                        d.id,
                                                        {
                                                            estado:
                                                                v as Estado,
                                                        },
                                                    );

                                                    toast.success(
                                                        "Estado actualizado.",
                                                    );

                                                }}
                                            >

                                                <SelectTrigger className="h-8 w-[140px]">

                                                    <SelectValue />

                                                </SelectTrigger>

                                                <SelectContent>

                                                    <SelectItem value="publicado">
                                                        Publicado
                                                    </SelectItem>

                                                    <SelectItem value="borrador">
                                                        Borrador
                                                    </SelectItem>

                                                    <SelectItem value="inactivo">
                                                        Inactivo
                                                    </SelectItem>

                                                </SelectContent>

                                            </Select>

                                        </TableCell>

                                        <TableCell className="w-[170px]">

                                            {new Date(
                                                d.fechaPublicacion,
                                            ).toLocaleDateString(
                                                "es-CO",
                                                {
                                                    day: "2-digit",
                                                    month: "short",
                                                    year: "numeric",
                                                },
                                            )}

                                        </TableCell>

                                        <TableCell className="w-[170px]">

                                            <div className="flex justify-end gap-1">

                                                <Button
                                                    size="icon"
                                                    variant="ghost"
                                                    title="Ver detalle"
                                                    onClick={() =>
                                                        setDocumentoSeleccionado(d)
                                                    }
                                                >
                                                    <Eye className="size-4" />
                                                </Button>

                                                <Button
                                                    size="icon"
                                                    variant="ghost"
                                                    title="Editar"
                                                    onClick={() =>
                                                        setEditar(d)
                                                    }
                                                >
                                                    <Pencil className="size-4" />
                                                </Button>

                                                <Button
                                                    size="icon"
                                                    variant="ghost"
                                                    title="Nueva versión"
                                                    onClick={() =>
                                                        setVersionar(d)
                                                    }
                                                >
                                                    <Upload className="size-4" />
                                                </Button>

                                                <Button
                                                    size="icon"
                                                    variant="ghost"
                                                    title="Historial"
                                                    onClick={() =>
                                                        setHistorial(d)
                                                    }
                                                >
                                                    <History className="size-4" />
                                                </Button>

                                            </div>

                                        </TableCell>

                                    </TableRow>
                                ))}

                            </TableBody>


                        </Table>

                    </div>

                </CardContent>

            </Card>

            <DialogEditar
                doc={editar}
                onClose={() => setEditar(null)}
                onSave={actualizarDocumento}
            />

            <DialogNuevaVersion
                doc={versionar}
                onClose={() => setVersionar(null)}
            />

            <Dialog
                open={!!historial}
                onOpenChange={(o) =>
                    !o && setHistorial(null)
                }
            >

                <DialogContent className="max-w-4xl">

                    <DialogHeader>

                        <DialogTitle>
                            Historial de versiones
                        </DialogTitle>

                        <DialogDescription>

                            {historial?.nombre}

                        </DialogDescription>

                    </DialogHeader>

                    <div className="max-h-[420px] overflow-y-auto">

                        <Table>

                            <TableHeader>

                                <TableRow>

                                    <TableHead>#</TableHead>
                                    <TableHead>Versión</TableHead>
                                    <TableHead>Fecha</TableHead>
                                    <TableHead>Publicado por</TableHead>
                                    <TableHead>Descripción de cambios</TableHead>
                                    <TableHead>Estado</TableHead>
                                    <TableHead className="text-right">
                                        Acción
                                    </TableHead>

                                </TableRow>

                            </TableHeader>

                            <TableBody>
                                {historial?.versiones.map((v, indice) => (
                                    <TableRow key={v.numero}>

                                        <TableCell className="font-mono">
                                            #{indice + 1}
                                        </TableCell>

                                        <TableCell className="font-mono">
                                            v{v.numero}
                                        </TableCell>

                                        <TableCell className="w-[170px]">
                                            {new Date(v.fecha).toLocaleDateString(
                                                "es-CO",
                                                {
                                                    day: "2-digit",
                                                    month: "long",
                                                    year: "numeric",
                                                },
                                            )}
                                        </TableCell>

                                        <TableCell className="w-[170px]">{v.autor}</TableCell>

                                        <TableCell className="text-muted-foreground">
                                            {v.notas}
                                        </TableCell>

                                        <TableCell className="w-[170px]">
                                            {indice === 0 ? (
                                                <span className="font-semibold text-emerald-600">
                                                Vigente
                                            </span>
                                            ) : (
                                                <span className="text-muted-foreground">
                                                Histórica
                                            </span>
                                            )}
                                        </TableCell>

                                        <TableCell className="text-right">

                                            <Button
                                                size="icon"
                                                variant="ghost"
                                                title="Descargar versión"
                                                onClick={() =>
                                                    ejecutarDescarga(
                                                        historial.nombre,
                                                        v.archivo ??
                                                        historial.archivo,
                                                        `v${v.numero}`,
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
    const { areas, sub_proceso, tipos } = useIntranet();

    const [nombre, setNombre] = useState("");
    const [descripcion, setDescripcion] = useState("");
    const [areaId, setAreaId] = useState("");
    const [subProcesoId, setSubProcesoId] = useState("");
    const [tipoId, setTipoId] = useState("");
    const [visibleTodas, setVisibleTodas] = useState(false);
    const [autorizadas, setAutorizadas] = useState<string[]>([]);
    const [cargado, setCargado] = useState<string | null>(null);

    if (doc && cargado !== doc.id) {
        setCargado(doc.id);
        setNombre(doc.nombre);
        setDescripcion(doc.descripcion);
        setAreaId(doc.areaId);
        setSubProcesoId(doc.subProcesoId);
        setTipoId(doc.tipoId);
        setVisibleTodas(doc.visibleTodas);
        setAutorizadas(doc.areasAutorizadas);
    }

    return (
        <Dialog
            open={!!doc}
            onOpenChange={(o) => !o && onClose()}
        >
            <DialogContent className="max-w-2xl max-h-[85vh] overflow-y-auto">

                <DialogHeader>

                    <DialogTitle>
                        Editar documento
                    </DialogTitle>

                    <DialogDescription>
                        Actualiza la información sin crear una nueva versión.
                    </DialogDescription>

                </DialogHeader>

                <div className="space-y-5">

                    <div className="space-y-2">
                        <Label>Código</Label>
                        <Input
                            value={doc?.codigo ?? ""}
                            disabled
                        />
                    </div>

                    <div className="space-y-2">
                        <Label>Nombre</Label>
                        <Input
                            value={nombre}
                            onChange={(e) =>
                                setNombre(e.target.value)
                            }
                        />
                    </div>

                    <div className="space-y-2">
                        <Label>Descripción</Label>

                        <Textarea
                            rows={4}
                            value={descripcion}
                            onChange={(e) =>
                                setDescripcion(
                                    e.target.value,
                                )
                            }
                        />
                    </div>

                    <div className="grid gap-3 sm:grid-cols-3">

                        <SelectorSimple
                            label="Área"
                            value={areaId}
                            onChange={setAreaId}
                            opciones={areas}
                        />

                        <SelectorSimple
                            label="Subproceso"
                            value={subProcesoId}
                            onChange={setSubProcesoId}
                            opciones={sub_proceso}
                        />

                        <SelectorSimple
                            label="Tipo"
                            value={tipoId}
                            onChange={setTipoId}
                            opciones={tipos}
                        />

                    </div>

                    <div className="space-y-2">

                        <Label>
                            Áreas autorizadas
                        </Label>

                        <AreasAutorizadas
                            idCheckbox="editar-visible"
                            areas={areas}
                            seleccionadas={autorizadas}
                            onChange={setAutorizadas}
                            visibleTodas={visibleTodas}
                            onVisibleTodas={setVisibleTodas}
                        />

                    </div>

                </div>

                <DialogFooter>

                    <Button
                        variant="outline"
                        onClick={onClose}
                    >
                        Cancelar
                    </Button>

                    <Button
                        onClick={() => {

                            if (!doc) return;

                            if (!nombre.trim())
                                return toast.error(
                                    "El nombre es obligatorio.",
                                );

                            if (
                                !visibleTodas &&
                                autorizadas.length === 0
                            )
                                return toast.error(
                                    "Seleccione al menos un área.",
                                );

                            onSave(doc.id, {
                                nombre,
                                descripcion,
                                areaId,
                                subProcesoId,
                                tipoId,
                                visibleTodas,
                                areasAutorizadas:
                                    visibleTodas
                                        ? []
                                        : autorizadas,
                            });

                            toast.success(
                                "Documento actualizado.",
                            );

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

function DialogPrevisualizar({
                                 doc,
                                 onClose,
                             }: {
    doc: Documento | null;
    onClose: () => void;
}) {
    return (
        <Dialog
            open={!!doc}
            onOpenChange={(o) => !o && onClose()}
        >
            <DialogContent className="max-w-3xl">

                <DialogHeader>

                    <DialogTitle>
                        Vista previa
                    </DialogTitle>

                    <DialogDescription>
                        {doc?.nombre} · Versión v{doc?.version}
                    </DialogDescription>

                </DialogHeader>

                <div className="rounded-lg border bg-muted p-6">

                    <p className="font-semibold border-b pb-2">
                        Visualizador interno
                    </p>

                    <div className="mt-4 space-y-3 text-sm">

                        <p>
                            <strong>Archivo:</strong>{" "}
                            {doc?.archivo}
                        </p>

                        <p>
                            <strong>Descripción:</strong>{" "}
                            {doc?.descripcion}
                        </p>

                        <div className="rounded border bg-background p-4 text-xs leading-relaxed text-muted-foreground">

                            Aquí se mostrará el visor PDF,
                            Word o cualquier formato soportado
                            cuando el proyecto se conecte con
                            el backend.

                        </div>

                    </div>

                </div>

                <DialogFooter>

                    <Button onClick={onClose}>
                        Cerrar
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
        <div className="space-y-2">

            <Label className="text-xs text-muted-foreground">
                {label}
            </Label>

            <Select
                value={value}
                onValueChange={onChange}
            >
                <SelectTrigger>
                    <SelectValue />
                </SelectTrigger>

                <SelectContent>

                    {opciones.map((o) => (
                        <SelectItem
                            key={o.id}
                            value={o.id}
                        >
                            {o.nombre}
                        </SelectItem>
                    ))}

                </SelectContent>

            </Select>

        </div>
    );
}

function Dato({
                  titulo,
                  children,
              }: {
    titulo: string;
    children: React.ReactNode;
}) {
    return (
        <div>

            <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                {titulo}
            </p>

            <div className="text-sm">
                {children}
            </div>

        </div>
    );
}
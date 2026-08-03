import { Link, createFileRoute, useNavigate } from "@tanstack/react-router";
import { ArrowLeft, Save, FileUp, Upload } from "lucide-react";
import React, { useState } from "react";
import { toast } from "sonner";

import { AppShell } from "@/components/AppShell";
import { AreasAutorizadas } from "@/components/AreasAutorizadas";
import { DropzoneArea } from "@/components/ui/drop-zonearea";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { Separator } from "@/components/ui/separator";

import type { Estado } from "@/lib/data";
import { useIntranet } from "@/lib/store";

export const Route = createFileRoute("/app/publicar")({
    head: () => ({
        meta: [
            { title: "Publicar documento — Intranet documental" },
            {
                name: "description",
                content: "Registra y publica un nuevo documento en la intranet documental.",
            },
            { property: "og:title", content: "Publicar documento — Intranet documental" },
        ],
    }),
    component: NuevoDocumentoPage,
});

function NuevoDocumentoPage() {
    const navigate = useNavigate();
    const store = useIntranet();
    const { areas, categorias, tipos, permisos } = store;

    // Detectar la función de publicación según el nombre expuesto en tu store
    const publicarFn =
        (store as any).publicarDocumento ||
        (store as any).crearDocumento ||
        (store as any).agregarDocumento;

    // Fecha actual predeterminada (YYYY-MM-DD)
    const fechaHoy = new Date().toISOString().split("T")[0];

    // Estados de la sección 1: Información
    const [nombre, setNombre] = useState("");
    const [descripcion, setDescripcion] = useState("");
    const [archivo, setArchivo] = useState<File | null>(null);

    // Estados de la sección 2: Clasificación y visibilidad
    const [areaId, setAreaId] = useState("");
    const [categoriaId, setCategoriaId] = useState("");
    const [tipoId, setTipoId] = useState("");
    const [visibleTodas, setVisibleTodas] = useState(false);
    const [autorizadas, setAutorizadas] = useState<string[]>([]);

    // Estados de la sección 3: Versión y estado
    const [version, setVersion] = useState("1.0");
    const [fechaPublicacion, setFechaPublicacion] = useState(fechaHoy);
    const [estado, setEstado] = useState<Estado>("publicado");

    const [subiendo, setSubiendo] = useState(false);

    if (!permisos?.actualizarDocumentos) {
        return (
            <AppShell titulo="Publicar documento">
                <Card>
                    <CardContent className="py-14 text-center text-sm text-muted-foreground">
                        No cuentas con permisos para publicar nuevos documentos.
                    </CardContent>
                </Card>
            </AppShell>
        );
    }

    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();

        // Validaciones
        if (!nombre.trim()) return toast.error("El nombre del documento es obligatorio.");
        if (!descripcion.trim()) return toast.error("La descripción del documento es obligatoria.");
        if (!areaId) return toast.error("Selecciona un área responsable.");
        if (!categoriaId) return toast.error("Selecciona una categoría.");
        if (!tipoId) return toast.error("Selecciona un tipo de documento.");
        if (!visibleTodas && autorizadas.length === 0) {
            return toast.error("Selecciona al menos un área autorizada o marca 'Visible para todas las áreas'.");
        }
        if (!archivo) return toast.error("Debes adjuntar un archivo para el documento.");

        setSubiendo(true);

        try {
            const formData = new FormData();
            formData.append("nombre", nombre.trim());
            formData.append("descripcion", descripcion.trim());
            formData.append("areaId", areaId);
            formData.append("categoriaId", categoriaId);
            formData.append("tipoId", tipoId);
            formData.append("archivo", archivo);

            // Simulación de carga
            await new Promise((resolve) => setTimeout(resolve, 1500));

            const nuevoDocumento = {
                nombre: nombre.trim(),
                descripcion: descripcion.trim(),
                areaId,
                categoriaId,
                tipoId,
                visibleTodas,
                areasAutorizadas: visibleTodas ? [] : autorizadas,
                archivo: archivo.name,
                version,
                fechaPublicacion,
                estado,
            };

            if (typeof publicarFn === "function") {
                publicarFn(nuevoDocumento);
            } else {
                console.warn("No se encontró una función de publicación directa en useIntranet().");
            }

            toast.success("Documento publicado exitosamente");
            navigate({ to: "/app/gestion-documentos" });
        } catch (error) {
            console.error("Error al publicar:", error);
            toast.error("Ocurrió un error al intentar publicar el documento.");
        } finally {
            setSubiendo(false);
        }
    };

    return (
        <AppShell
            titulo="Publicar nuevo documento"
            descripcion="Diligencia los metadatos del documento y adjunta el archivo oficial."
        >
            <div className="space-y-6 max-w-4xl mx-auto pb-10 p-4 md:p-6">

                {/* Encabezado y Navegación */}
                <div className="flex items-center space-x-4 mb-4">
                    <Button variant="outline" size="icon" asChild>
                        <Link to="/app/gestion-documentos">
                            <ArrowLeft className="w-5 h-5" />
                        </Link>
                    </Button>
                    <div>
                        <h1 className="text-2xl font-bold tracking-tight">Cargar Nuevo Documento</h1>
                        <p className="text-muted-foreground text-sm">
                            Completa los metadatos y adjunta el archivo correspondiente.
                        </p>
                    </div>
                </div>

                <form onSubmit={handleSubmit} className="space-y-6">
                    {/* TARJETA 1: Información del documento */}
                    <Card>
                        <CardHeader>
                            <CardTitle className="text-base font-bold">Información del documento</CardTitle>
                            <CardDescription>
                                Los metadatos permitirán clasificar y buscar el archivo fácilmente.
                            </CardDescription>
                        </CardHeader>
                        <CardContent className="space-y-5">
                            <div className="space-y-1.5">
                                <Label htmlFor="nombre">
                                    Nombre del documento <span className="text-destructive">*</span>
                                </Label>
                                <Input
                                    id="nombre"
                                    placeholder="Ej: PR-L&D-PG-01 PROGRAMA LIMPIEZA Y DESINFECCIÓN V5"
                                    value={nombre}
                                    onChange={(e) => setNombre(e.target.value)}
                                />
                            </div>

                            <div className="space-y-1.5">
                                <Label htmlFor="descripcion">
                                    Descripción / Alcance <span className="text-destructive">*</span>
                                </Label>
                                <Textarea
                                    id="descripcion"
                                    rows={3}
                                    placeholder="Describe brevemente el alcance y propósito del documento…"
                                    value={descripcion}
                                    onChange={(e) => setDescripcion(e.target.value)}
                                />
                            </div>

                            <Separator />

                            {/* Zona de Carga de Archivo */}
                            <div className="space-y-3 pt-2">
                                <div className="flex flex-col">
                                    <Label className="text-sm font-medium">
                                        Archivo Adjunto <span className="text-destructive">*</span>
                                    </Label>
                                    <span className="text-xs text-muted-foreground mb-2">
                                        Por seguridad institucional, solo se permiten formatos PDF, Word o Excel.
                                    </span>
                                </div>
                                <DropzoneArea
                                    selectedFile={archivo}
                                    onFileSelect={setArchivo}
                                    accept={{
                                        'application/pdf': ['.pdf'],
                                        'application/msword': ['.doc'],
                                        'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx'],
                                        'application/vnd.ms-excel': ['.xls'],
                                        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': ['.xlsx']
                                    }}
                                    maxSize={10 * 1024 * 1024} // 10 MB límite
                                />
                            </div>
                        </CardContent>
                    </Card>

                    {/* TARJETA 2: Clasificación y visibilidad */}
                    <Card>
                        <CardHeader>
                            <CardTitle className="text-base font-bold">Clasificación y visibilidad</CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-5">
                            <div className="grid gap-4 sm:grid-cols-3">
                                <div className="space-y-1.5">
                                    <Label>
                                        Área responsable <span className="text-destructive">*</span>
                                    </Label>
                                    <Select value={areaId} onValueChange={setAreaId}>
                                        <SelectTrigger>
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
                                </div>

                                <div className="space-y-1.5">
                                    <Label>
                                        Categoría <span className="text-destructive">*</span>
                                    </Label>
                                    <Select value={categoriaId} onValueChange={setCategoriaId}>
                                        <SelectTrigger>
                                            <SelectValue placeholder="Seleccionar…" />
                                        </SelectTrigger>
                                        <SelectContent>
                                            {categorias.map((c) => (
                                                <SelectItem key={c.id} value={c.id}>
                                                    {c.nombre}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                </div>

                                <div className="space-y-1.5">
                                    <Label>
                                        Tipo de documento <span className="text-destructive">*</span>
                                    </Label>
                                    <Select value={tipoId} onValueChange={setTipoId}>
                                        <SelectTrigger>
                                            <SelectValue placeholder="Seleccionar…" />
                                        </SelectTrigger>
                                        <SelectContent>
                                            {tipos.map((t) => (
                                                <SelectItem key={t.id} value={t.id}>
                                                    {t.nombre}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                </div>
                            </div>

                            <div className="space-y-2 pt-2">
                                <Label>Áreas autorizadas para visualizar</Label>
                                <AreasAutorizadas
                                    idCheckbox="publicar-visible-todas"
                                    areas={areas}
                                    seleccionadas={autorizadas}
                                    onChange={setAutorizadas}
                                    visibleTodas={visibleTodas}
                                    onVisibleTodas={setVisibleTodas}
                                />
                            </div>
                        </CardContent>
                    </Card>

                    {/* TARJETA 3: Versión y estado */}
                    <Card>
                        <CardHeader>
                            <CardTitle className="text-base font-bold">Versión y estado</CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-6">
                            <div className="grid gap-4 sm:grid-cols-3">
                                <div className="space-y-1.5">
                                    <Label htmlFor="version">Versión</Label>
                                    <Input
                                        id="version"
                                        value={version}
                                        onChange={(e) => setVersion(e.target.value)}
                                        placeholder="1.0"
                                    />
                                    <p className="text-xs text-muted-foreground">
                                        Se sugiere 1.0 para la primera publicación.
                                    </p>
                                </div>

                                <div className="space-y-1.5">
                                    <Label htmlFor="fecha">Fecha de publicación</Label>
                                    <Input
                                        id="fecha"
                                        type="date"
                                        value={fechaPublicacion}
                                        onChange={(e) => setFechaPublicacion(e.target.value)}
                                    />
                                </div>

                                <div className="space-y-1.5">
                                    <Label>Estado</Label>
                                    <Select value={estado} onValueChange={(v) => setEstado(v as Estado)}>
                                        <SelectTrigger>
                                            <SelectValue />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="publicado">Publicado</SelectItem>
                                            <SelectItem value="borrador">Borrador</SelectItem>
                                            <SelectItem value="inactivo">Inactivo</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>
                            </div>

                            <div className="flex justify-end gap-3 pt-4 border-t">
                                <Button asChild variant="outline" type="button" disabled={subiendo}>
                                    <Link to="/app/gestion-documentos">Cancelar</Link>
                                </Button>
                                <Button type="submit" disabled={subiendo} className="gap-2">
                                    {subiendo ? (
                                        "Guardando..."
                                    ) : (
                                        <>
                                            <Save className="size-4 mr-2" />
                                            Guardar Documento
                                        </>
                                    )}
                                </Button>
                            </div>
                        </CardContent>
                    </Card>
                </form>
            </div>
        </AppShell>
    );
}
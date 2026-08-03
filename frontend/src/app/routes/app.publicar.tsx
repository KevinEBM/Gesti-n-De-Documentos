import { Link, createFileRoute, useNavigate } from "@tanstack/react-router";
import { ArrowLeft, FileUp, Upload } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { AppShell } from "@/components/AppShell";
import { AreasAutorizadas } from "@/components/AreasAutorizadas";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
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
    component: PublicarDocumento,
});

export function PublicarDocumento() {
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

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();

        // Validaciones
        if (!nombre.trim()) return toast.error("El nombre del documento es obligatorio.");
        if (!areaId) return toast.error("Selecciona un área responsable.");
        if (!categoriaId) return toast.error("Selecciona una categoría.");
        if (!tipoId) return toast.error("Selecciona un tipo de documento.");
        if (!visibleTodas && autorizadas.length === 0) {
            return toast.error("Selecciona al menos un área autorizada o marca 'Visible para todas las áreas'.");
        }
        if (!archivo) return toast.error("Debes adjuntar un archivo para el documento.");

        setSubiendo(true);

        try {
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
            <div className="space-y-6 max-w-4xl mx-auto pb-10">
                <div>
                    <Button asChild variant="ghost" size="sm" className="gap-2">
                        <Link to="/app/gestion-documentos">
                            <ArrowLeft className="size-4" /> Volver a gestión de documentos
                        </Link>
                    </Button>
                </div>

                <form onSubmit={handleSubmit} className="space-y-6">
                    {/* TARJETA 1: Información del documento */}
                    <Card>
                        <CardHeader>
                            <CardTitle className="text-base font-bold">Información del documento</CardTitle>
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
                                <Label htmlFor="descripcion">Descripción / Alcance</Label>
                                <Textarea
                                    id="descripcion"
                                    rows={3}
                                    placeholder="Describe brevemente el alcance y propósito del documento…"
                                    value={descripcion}
                                    onChange={(e) => setDescripcion(e.target.value)}
                                />
                            </div>

                            <div className="space-y-1.5 pt-2">
                                <Label>
                                    Archivo adjunto <span className="text-destructive">*</span>
                                </Label>
                                <label
                                    htmlFor="archivo-input"
                                    className="flex flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed border-muted-foreground/25 p-6 text-center cursor-pointer hover:bg-accent/50 transition-colors"
                                >
                                    <FileUp className="size-8 text-muted-foreground" />
                                    <span className="text-sm font-medium text-foreground">
                                        {archivo ? archivo.name : "Haz clic para seleccionar el archivo"}
                                    </span>
                                    <span className="text-xs text-muted-foreground">PDF, DOCX o XLSX (simulado)</span>
                                    <input
                                        id="archivo-input"
                                        type="file"
                                        className="hidden"
                                        accept=".pdf,.doc,.docx,.xls,.xlsx"
                                        onChange={(e) => {
                                            if (e.target.files && e.target.files[0]) {
                                                setArchivo(e.target.files[0]);
                                            }
                                        }}
                                    />
                                </label>
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
                                <Button asChild variant="outline" type="button">
                                    <Link to="/app/gestion-documentos">Cancelar</Link>
                                </Button>
                                <Button type="submit" disabled={subiendo} className="gap-2">
                                    <Upload className="size-4" />
                                    {subiendo ? "Publicando..." : "Publicar documento"}
                                </Button>
                            </div>
                        </CardContent>
                    </Card>
                </form>
            </div>
        </AppShell>
    );
}
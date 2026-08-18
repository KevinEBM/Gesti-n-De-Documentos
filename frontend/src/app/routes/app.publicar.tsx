import { Link, createFileRoute, useNavigate } from "@tanstack/react-router";
import { ArrowLeft, Save, FileUp, Upload, FileText } from "lucide-react";
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
import { obtenerIconoArea } from "@/lib/iconos-areas";
import { obtenerIconoSubProceso } from "@/lib/iconos-subprocesos";
import { obtenerIconoFormato } from "@/lib/iconos-formatos";
import type { Estado } from "@/lib/data";
import { useIntranet } from "@/lib/store";
import { extraerInformacionDocumento } from "@/lib/extraer-info-doc";


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
    const { areas, sub_proceso, tipos, permisos } = store;

    // Detectar la función de publicación según el nombre expuesto en tu store
    const publicarFn =
        (store as any).publicarDocumento ||
        (store as any).crearDocumento ||
        (store as any).agregarDocumento;

    // Fecha actual predeterminada (YYYY-MM-DD)
    const fechaHoy = new Date().toISOString().split("T")[0];

    //Estados de la sección 0
    const [codigo, setCodigo] = useState("");
    // Estados de la sección 1: Información
    const [nombre, setNombre] = useState("");
    const [descripcion, setDescripcion] = useState("");
    const [archivo, setArchivo] = useState<File | null>(null);

    // Estados de la sección 2: Clasificación y visibilidad
    const [areaId, setAreaId] = useState("");
    const [subProcesoId, setSub_procesoId] = useState("");
    const [tipoId, setTipoId] = useState("");
    const [visibleTodas, setVisibleTodas] = useState(false);
    const [autorizadas, setAutorizadas] = useState<string[]>([]);

    // Estados de la sección 3: Versión y estado
    const [version, setVersion] = useState("1.0");
    const [fechaPublicacion, setFechaPublicacion] = useState(fechaHoy);
    const [estado, setEstado] = useState<Estado>("publicado");

    const [subiendo, setSubiendo] = useState(false);

    const subProcesosFiltrados = sub_proceso.filter(
        (sp) => sp.areaId === areaId
    );

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
        if (!codigo.trim()) return toast.error("El código del documento es obligatorio.");
        if (!nombre.trim()) return toast.error("El nombre del documento es obligatorio.");
        if (!descripcion.trim()) return toast.error("La descripción del documento es obligatoria.");
        if (!areaId) return toast.error("Selecciona un área responsable.");
        if (!subProcesoId) return toast.error("Selecciona un subproceso.");
        if (!tipoId) return toast.error("Selecciona un tipo de documento.");
        if (!visibleTodas && autorizadas.length === 0) {
            return toast.error("Selecciona al menos un área autorizada o marca 'Visible para todas las áreas'.");
        }
        if (!archivo) return toast.error("Debes adjuntar un archivo para el documento.");

        setSubiendo(true);

        try {
            const formData = new FormData();
            formData.append("codigo", codigo.trim());
            formData.append("nombre", nombre.trim());
            formData.append("descripcion", descripcion.trim());
            formData.append("areaId", areaId);
            formData.append("subProcesoId", subProcesoId);
            formData.append("tipoId", tipoId);
            formData.append("archivo", archivo);

            // Simulación de carga
            await new Promise((resolve) => setTimeout(resolve, 1500));

            const nuevoDocumento = {
                codigo,
                nombre: nombre.trim(),
                descripcion: descripcion.trim(),
                areaId,
                subProcesoId,
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

                                <p>Los metadatos permitirán clasificar y buscar el archivo fácilmente.</p>
                                <p><b>Se recomienda subir el archivo antes de rellenar los campos</b></p>
                            </CardDescription>
                        </CardHeader>
                        <CardContent className="space-y-5">
                            <div className="space-y-1.5">
                                <Label htmlFor="codigo">
                                    Código <span className="text-destructive">*</span>
                                </Label>

                                <Input
                                    id="codigo"
                                    placeholder="PR-LD-PO-01"
                                    value={codigo}
                                    onChange={(e) => setCodigo(e.target.value.toUpperCase())}
                                />

                                <p className="text-xs text-muted-foreground">
                                    Se detectará automáticamente desde el nombre del archivo cuando siga la nomenclatura
                                    institucional.
                                </p>
                            </div>

                            <div className="space-y-1.5">
                                <Label htmlFor="nombre">
                                    Nombre del documento <span className="text-destructive">*</span>
                                </Label>
                                <Input
                                    id="nombre"
                                    placeholder="Ej: PROGRAMA LIMPIEZA Y DESINFECCIÓN"
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

                            <Separator/>

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
                                    onFileSelect={(file) => {

                                        if (!file) {
                                            setArchivo(null);
                                            return;
                                        }

                                        console.log("Archivo seleccionado:", file.name);

                                        const resultado = extraerInformacionDocumento(file.name);

                                        if (!resultado.valido) {

                                            toast.error(
                                                resultado.error ??
                                                "El archivo no cumple la nomenclatura institucional."
                                            );

                                            setArchivo(null);

                                            return;
                                        }

                                        // Guardar archivo
                                        setArchivo(file);

                                        // Solo completar el código automáticamente si el usuario no escribió uno
                                        if (!codigo.trim()) {
                                            setCodigo(resultado.codigo ?? "");
                                        }

                                        // Siempre actualizar nombre desde el archivo
                                        if (resultado.nombre !== null) {
                                            setNombre(resultado.nombre);
                                        }

                                        // Siempre actualizar versión
                                        setVersion(resultado.version ?? "1.0");

                                        toast.success("Archivo cargado correctamente.");
                                    }}
                                    accept={{
                                        "application/pdf": [".pdf"],
                                        "application/msword": [".doc"],
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document": [".docx"],
                                        "application/vnd.ms-excel": [".xls"],
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet": [".xlsx"],
                                    }}
                                    maxSize={15 * 1024 * 1024}
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
                                    <Select
                                        value={areaId}
                                        onValueChange={(value) => {
                                            setAreaId(value);
                                            setSub_procesoId("");
                                        }}>
                                        <SelectTrigger>
                                            <SelectValue placeholder="Seleccionar…" />
                                        </SelectTrigger>
                                        <SelectContent>
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

                                </div>

                                <div className="space-y-1.5">
                                    <Label>
                                        Subproceso <span className="text-destructive">*</span>
                                    </Label>
                                    <Select value={subProcesoId} onValueChange={setSub_procesoId}>
                                        <SelectTrigger>
                                            <SelectValue placeholder="Seleccionar…" />
                                        </SelectTrigger>

                                        <SelectContent>
                                            {subProcesosFiltrados.map((c) => {
                                                const { icono: Icono, color } = obtenerIconoSubProceso(c.nombre);

                                                return (
                                                    <SelectItem key={c.id} value={c.id}>
                                                        <div className="flex items-center gap-2">
                                                            <Icono className={`size-4 ${color}`} />
                                                            <span>{c.nombre}</span>
                                                        </div>
                                                    </SelectItem>
                                                );
                                            })}
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
                                                    <div className="flex items-center gap-2">
                                                        <FileText className="size-4 text-slate-500" />
                                                        <span>{t.nombre}</span>
                                                    </div>
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
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { FileUp } from "lucide-react";
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
                content:
                    "Formulario para registrar un documento interno: área responsable, áreas autorizadas, categoría, tipo, versión y estado.",
            },
            { property: "og:title", content: "Publicar documento — Intranet documental" },
            { property: "og:description", content: "Registra y publica documentos internos con control de versiones." },
        ],
    }),
    component: Publicar,
});

function Publicar() {
    const { areas, categorias, tipos, crearDocumento, sesion, permisos } = useIntranet();
    const navigate = useNavigate();

    const [errores, setErrores] = useState<Record<string, string>>({});
    const [form, setForm] = useState({
        nombre: "",
        descripcion: "",
        archivo: "",
        areaId: "",
        categoriaId: "",
        tipoId: "",
        version: "1.0",
        fechaPublicacion: new Date().toISOString().slice(0, 10),
        estado: "publicado" as Estado,
    });
    const [visibleTodas, setVisibleTodas] = useState(false);
    const [autorizadas, setAutorizadas] = useState<string[]>([]);

    const set = (k: keyof typeof form, v: string) => setForm((f) => ({ ...f, [k]: v }));

    if (!permisos.publicarDocumentos) {
        return (
            <AppShell titulo="Publicar documento">
                <Card>
                    <CardContent className="py-14 text-center text-sm text-muted-foreground">
                        No cuentas con permisos para publicar documentos.
                    </CardContent>
                </Card>
            </AppShell>
        );
    }

    const publicar = () => {
        const e: Record<string, string> = {};
        if (!form.nombre.trim()) e.nombre = "Ingresa el nombre del documento.";
        if (form.descripcion.trim().length < 10) e.descripcion = "La descripción debe tener al menos 10 caracteres.";
        if (!form.archivo.trim()) e.archivo = "Selecciona el archivo del documento.";
        if (!form.areaId) e.areaId = "Selecciona el área responsable.";
        if (!form.categoriaId) e.categoriaId = "Selecciona la categoría.";
        if (!form.tipoId) e.tipoId = "Selecciona el tipo de documento.";
        if (!/^\d+(\.\d+)?$/.test(form.version)) e.version = "Usa un formato de versión válido, por ejemplo 1.0.";
        if (!form.fechaPublicacion) e.fechaPublicacion = "Indica la fecha de publicación.";
        if (!visibleTodas && autorizadas.length === 0)
            e.autorizadas = "Agrega al menos un área autorizada o marca «Visible para todas las áreas».";
        setErrores(e);
        if (Object.keys(e).length) return;

        crearDocumento({
            ...form,
            visibleTodas,
            areasAutorizadas: visibleTodas ? [] : autorizadas,
            publicadoPor: sesion?.nombre ?? "Administrador",
        });
        toast.success(
            form.estado === "publicado" ? "Documento publicado correctamente" : "Borrador guardado correctamente",
            { description: form.nombre },
        );
        navigate({ to: "/app/gestion-documentos" });
    };

    return (
        <AppShell titulo="Publicar documento" descripcion="Registra un nuevo documento en la intranet">
            <Card>
                <CardHeader>
                    <CardTitle className="text-base">Información del documento</CardTitle>
                </CardHeader>
                <CardContent className="space-y-6">
                    <Campo label="Nombre del documento" error={errores.nombre}>
                        <Input
                            value={form.nombre}
                            onChange={(e) => set("nombre", e.target.value)}
                            placeholder="Ej. Manual de seguridad y salud en el trabajo"
                        />
                    </Campo>

                    <Campo label="Descripción" error={errores.descripcion}>
                        <Textarea
                            rows={4}
                            value={form.descripcion}
                            onChange={(e) => set("descripcion", e.target.value)}
                            placeholder="Describe brevemente el alcance y propósito del documento"
                        />
                    </Campo>

                    <Campo label="Archivo" error={errores.archivo}>
                        <label className="flex cursor-pointer flex-col items-center justify-center gap-2 rounded-md border border-dashed border-border bg-secondary/40 px-4 py-7 text-center hover:border-primary/40">
                            <FileUp className="size-5 text-muted-foreground" />
                            <span className="text-sm font-medium">{form.archivo || "Haz clic para seleccionar el archivo"}</span>
                            <span className="text-xs text-muted-foreground">PDF, DOCX o XLSX (simulado)</span>
                            <input
                                type="file"
                                className="hidden"
                                onChange={(e) => set("archivo", e.target.files?.[0]?.name ?? "")}
                            />
                        </label>
                    </Campo>
                </CardContent>
            </Card>

            <Card>
                <CardHeader>
                    <CardTitle className="text-base">Clasificación y visibilidad</CardTitle>
                </CardHeader>
                <CardContent className="space-y-6">
                    <div className="grid gap-5 sm:grid-cols-3">
                        <Campo label="Área responsable" error={errores.areaId}>
                            <Selector
                                value={form.areaId}
                                onChange={(v) => set("areaId", v)}
                                opciones={areas.filter((a) => a.activo)}
                            />
                        </Campo>
                        <Campo label="Categoría" error={errores.categoriaId}>
                            <Selector
                                value={form.categoriaId}
                                onChange={(v) => set("categoriaId", v)}
                                opciones={categorias.filter((c) => c.activo)}
                            />
                        </Campo>
                        <Campo label="Tipo de documento" error={errores.tipoId}>
                            <Selector
                                value={form.tipoId}
                                onChange={(v) => set("tipoId", v)}
                                opciones={tipos.filter((t) => t.activo)}
                            />
                        </Campo>
                    </div>

                    <Campo label="Áreas autorizadas para visualizar" error={errores.autorizadas}>
                        <AreasAutorizadas
                            areas={areas}
                            seleccionadas={autorizadas}
                            onChange={setAutorizadas}
                            visibleTodas={visibleTodas}
                            onVisibleTodas={setVisibleTodas}
                        />
                    </Campo>
                </CardContent>
            </Card>

            <Card>
                <CardHeader>
                    <CardTitle className="text-base">Versión y estado</CardTitle>
                </CardHeader>
                <CardContent className="grid gap-5 sm:grid-cols-3">
                    <Campo label="Versión" error={errores.version}>
                        <Input value={form.version} onChange={(e) => set("version", e.target.value)} placeholder="1.0" />
                        <p className="text-xs text-muted-foreground">Se sugiere 1.0 para la primera publicación.</p>
                    </Campo>
                    <Campo label="Fecha de publicación" error={errores.fechaPublicacion}>
                        <Input
                            type="date"
                            value={form.fechaPublicacion}
                            onChange={(e) => set("fechaPublicacion", e.target.value)}
                        />
                    </Campo>
                    <Campo label="Estado">
                        <Select value={form.estado} onValueChange={(v) => set("estado", v)}>
                            <SelectTrigger className="w-full">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value="borrador">Borrador</SelectItem>
                                <SelectItem value="publicado">Publicado</SelectItem>
                                <SelectItem value="inactivo">Inactivo</SelectItem>
                            </SelectContent>
                        </Select>
                    </Campo>
                </CardContent>
            </Card>

            <div className="flex justify-end gap-2">
                <Button variant="outline" onClick={() => navigate({ to: "/app/gestion-documentos" })}>
                    Cancelar
                </Button>
                <Button onClick={publicar}>
                    {form.estado === "publicado" ? "Publicar documento" : "Guardar"}
                </Button>
            </div>
        </AppShell>
    );
}

function Campo({ label, error, children }: { label: string; error?: string; children: React.ReactNode }) {
    return (
        <div className="space-y-1.5">
            <Label>{label}</Label>
            {children}
            {error && <p className="text-xs text-destructive">{error}</p>}
        </div>
    );
}

function Selector({
                      value,
                      onChange,
                      opciones,
                  }: {
    value: string;
    onChange: (v: string) => void;
    opciones: { id: string; nombre: string }[];
}) {
    return (
        <Select value={value} onValueChange={onChange}>
            <SelectTrigger className="w-full">
                <SelectValue placeholder="Seleccionar…" />
            </SelectTrigger>
            <SelectContent>
                {opciones.map((o) => (
                    <SelectItem key={o.id} value={o.id}>
                        {o.nombre}
                    </SelectItem>
                ))}
            </SelectContent>
        </Select>
    );
}

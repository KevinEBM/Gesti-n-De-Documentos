import { Link, createFileRoute, useNavigate } from "@tanstack/react-router";
import {
    ArrowLeft,
    Save,
} from "lucide-react";
import React, { useState } from "react";
import { toast } from "sonner";

import { AppShell } from "@/components/AppShell";
import { AreasAutorizadas } from "@/components/AreasAutorizadas";
import { DropzoneArea } from "@/components/ui/drop-zonearea";
import { Button } from "@/components/ui/button";
import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    CardDescription,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
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
            {
                title: "Publicar documento — Intranet documental",
            },
            {
                name: "description",
                content:
                    "Registra y publica un nuevo documento en la intranet documental.",
            },
            {
                property: "og:title",
                content:
                    "Publicar documento — Intranet documental",
            },
        ],
    }),
    component: NuevoDocumentoPage,
});


function NuevoDocumentoPage() {

    const navigate = useNavigate();

    const store = useIntranet();

    const {
        areas,
        sub_proceso,
        tipos,
        permisos,
    } = store;


    /* ===================================================
     * FUNCIÓN DE PUBLICACIÓN
     * =================================================== */

    const publicarFn =
        (store as any).publicarDocumento ||
        (store as any).crearDocumento ||
        (store as any).agregarDocumento;


    /* ===================================================
     * FECHA
     * =================================================== */

    const fechaHoy =
        new Date()
            .toISOString()
            .split("T")[0];


    /* ===================================================
     * ESTADOS
     * =================================================== */

    // Sección 0: Código
    const [codigo, setCodigo] =
        useState("");


    // Sección 1: Información
    const [nombre, setNombre] =
        useState("");

    const [descripcion, setDescripcion] =
        useState("");

    const [archivo, setArchivo] =
        useState<File | null>(null);


    // Sección 2: Clasificación
    const [areaId, setAreaId] =
        useState("");

    const [subProcesoId, setSub_procesoId] =
        useState("");

    const [tipoId, setTipoId] =
        useState("");


    // Visibilidad
    const [visibleTodas, setVisibleTodas] =
        useState(false);

    const [autorizadas, setAutorizadas] =
        useState<string[]>([]);


    // Sección 3: Versión
    const [version, setVersion] =
        useState("1.0");

    const [fechaPublicacion, setFechaPublicacion] =
        useState(fechaHoy);

    const [estado, setEstado] =
        useState<Estado>("publicado");


    const [subiendo, setSubiendo] =
        useState(false);


    /* ===================================================
     * SUBPROCESOS FILTRADOS (CON SEGURO DE ESTADO)
     * =================================================== */

    const subProcesosFiltrados =
        sub_proceso.filter(
            (sp) =>
                sp.areaId === areaId ||
                (subProcesoId && sp.id === subProcesoId)
        );


    /* ===================================================
     * PERMISOS
     * =================================================== */

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


    /* ===================================================
     * CARGA Y EXTRACCIÓN DEL ARCHIVO
     * =================================================== */

    const manejarArchivoSeleccionado = (
        file: File | null,
    ) => {

        /* -----------------------------------------------
         * Si se elimina el archivo
         * ----------------------------------------------- */

        if (!file) {

            setArchivo(null);

            return;

        }


        try {
            /* -----------------------------------------------
             * Extraer información del nombre
             * ----------------------------------------------- */

            const resultado =
                extraerInformacionDocumento(
                    file.name,
                );


            /* -----------------------------------------------
             * Validar nomenclatura
             * ----------------------------------------------- */

            if (!resultado.valido) {

                toast.error(
                    resultado.mensaje ??
                    "El archivo no cumple la nomenclatura institucional.",
                );

                setArchivo(null);

                return;

            }


            /* -----------------------------------------------
             * Guardar archivo
             * ----------------------------------------------- */

            setArchivo(file);


            /* -----------------------------------------------
             * CÓDIGO Y DATOS BÁSICOS
             * ----------------------------------------------- */

            if (resultado.codigo) {
                setCodigo(resultado.codigo);
            }

            if (resultado.nombre) {
                setNombre(resultado.nombre);
            }

            setVersion(resultado.version ?? "1.0");


            /* -----------------------------------------------
             * CLASIFICACIÓN (Área, Subproceso y Tipo)
             * ----------------------------------------------- */

            if (resultado.areaId) {
                setAreaId(resultado.areaId);
            }

            if (resultado.subProcesoId) {
                setSub_procesoId(resultado.subProcesoId);
            }

            if (resultado.tipoId) {
                setTipoId(resultado.tipoId);
            }


            /* -----------------------------------------------
             * Confirmación
             * ----------------------------------------------- */

            toast.success(
                "Documento analizado correctamente. Los campos fueron completados automáticamente.",
            );
        } catch (error) {

            console.error("Error al extraer información del documento:", error);

            toast.error("Ocurrió un error al procesar el archivo.");

            setArchivo(null);

        }

    };


    /* ===================================================
     * GUARDAR DOCUMENTO
     * =================================================== */

    const handleSubmit = async (
        e: React.FormEvent<HTMLFormElement>,
    ) => {

        e.preventDefault();


        /* -----------------------------------------------
         * Validaciones
         * ----------------------------------------------- */

        if (!codigo.trim()) {

            return toast.error(
                "El código del documento es obligatorio.",
            );

        }


        if (!nombre.trim()) {

            return toast.error(
                "El nombre del documento es obligatorio.",
            );

        }


        if (!descripcion.trim()) {

            return toast.error(
                "La descripción del documento es obligatoria.",
            );

        }


        if (!areaId) {

            return toast.error(
                "Selecciona un área responsable.",
            );

        }


        if (!subProcesoId) {

            return toast.error(
                "Selecciona un subproceso.",
            );

        }


        if (!tipoId) {

            return toast.error(
                "Selecciona un tipo de documento.",
            );

        }


        if (
            !visibleTodas &&
            autorizadas.length === 0
        ) {

            return toast.error(
                "Selecciona al menos un área autorizada o marca 'Visible para todas las áreas'.",
            );

        }


        if (!archivo) {

            return toast.error(
                "Debes adjuntar un archivo para el documento.",
            );

        }


        setSubiendo(true);


        try {

            const formData =
                new FormData();


            formData.append(
                "codigo",
                codigo.trim(),
            );


            formData.append(
                "nombre",
                nombre.trim(),
            );


            formData.append(
                "descripcion",
                descripcion.trim(),
            );


            formData.append(
                "areaId",
                areaId,
            );


            formData.append(
                "subProcesoId",
                subProcesoId,
            );


            formData.append(
                "tipoId",
                tipoId,
            );


            formData.append(
                "archivo",
                archivo,
            );


            /* -------------------------------------------
             * Simulación de carga
             * ------------------------------------------- */

            await new Promise(
                (resolve) =>
                    setTimeout(
                        resolve,
                        1500,
                    ),
            );


            /* -------------------------------------------
             * Documento
             * ------------------------------------------- */

            const nuevoDocumento = {

                codigo,

                nombre:
                    nombre.trim(),

                descripcion:
                    descripcion.trim(),

                areaId,

                subProcesoId,

                tipoId,

                visibleTodas,

                areasAutorizadas:
                    visibleTodas
                        ? []
                        : autorizadas,

                archivo:
                archivo.name,

                version,

                fechaPublicacion,

                estado,

            };


            /* -------------------------------------------
             * Publicar
             * ------------------------------------------- */

            if (
                typeof publicarFn ===
                "function"
            ) {

                publicarFn(
                    nuevoDocumento,
                );

            } else {

                console.warn(
                    "No se encontró una función de publicación directa en useIntranet().",
                );

            }


            toast.success(
                "Documento publicado exitosamente",
            );


            navigate({
                to:
                    "/app/gestion-documentos",
            });


        } catch (error) {

            console.error(
                "Error al publicar:",
                error,
            );

            toast.error(
                "Ocurrió un error al intentar publicar el documento.",
            );

        } finally {

            setSubiendo(false);

        }

    };


    /* ===================================================
     * INTERFAZ
     * =================================================== */

    return (

        <AppShell
            titulo="Publicar nuevo documento"
            descripcion="Diligencia los metadatos del documento y adjunta el archivo oficial."
        >

            <div className="space-y-6 max-w-4xl mx-auto pb-10 p-4 md:p-6">


                {/* =================================================
                 * ENCABEZADO
                 * ================================================= */}

                <div className="flex items-center space-x-4 mb-4">

                    <Button
                        variant="outline"
                        size="icon"
                        asChild
                    >

                        <Link to="/app/gestion-documentos">

                            <ArrowLeft className="w-5 h-5" />

                        </Link>

                    </Button>


                    <div>

                        <h1 className="text-2xl font-bold tracking-tight">

                            Cargar Nuevo Documento

                        </h1>


                        <p className="text-muted-foreground text-sm">

                            Completa los metadatos y adjunta el archivo correspondiente.

                        </p>

                    </div>

                </div>


                <form
                    onSubmit={handleSubmit}
                    className="space-y-6"
                >


                    {/* =================================================
                     * TARJETA 1
                     * ================================================= */}

                    <Card>

                        <CardHeader>

                            <CardTitle className="text-base font-bold">

                                Información del documento

                            </CardTitle>


                            <CardDescription>

                                <p>
                                    Los metadatos permitirán clasificar y buscar el archivo fácilmente.
                                </p>

                                <p>
                                    <b>
                                        Se recomienda subir el archivo antes de rellenar los campos
                                    </b>
                                </p>

                            </CardDescription>

                        </CardHeader>


                        <CardContent className="space-y-5">


                            {/* Código */}

                            <div className="space-y-1.5">

                                <Label htmlFor="codigo">

                                    Código{" "}

                                    <span className="text-destructive">
                                        *
                                    </span>

                                </Label>


                                <Input
                                    id="codigo"
                                    placeholder="PR-L&D-IN-03"
                                    value={codigo}
                                    onChange={(e) =>
                                        setCodigo(
                                            e.target.value.toUpperCase(),
                                        )
                                    }
                                />


                                <p className="text-xs text-muted-foreground">

                                    Se detectará automáticamente desde el nombre del archivo cuando siga la nomenclatura institucional.

                                </p>

                            </div>


                            {/* Nombre */}

                            <div className="space-y-1.5">

                                <Label htmlFor="nombre">

                                    Nombre del documento{" "}

                                    <span className="text-destructive">
                                        *
                                    </span>

                                </Label>


                                <Input
                                    id="nombre"
                                    placeholder="Ej: PROGRAMA LIMPIEZA Y DESINFECCIÓN"
                                    value={nombre}
                                    onChange={(e) =>
                                        setNombre(
                                            e.target.value,
                                        )
                                    }
                                />

                            </div>


                            {/* Descripción */}

                            <div className="space-y-1.5">

                                <Label htmlFor="descripcion">

                                    Descripción / Alcance{" "}

                                    <span className="text-destructive">
                                        *
                                    </span>

                                </Label>


                                <Textarea
                                    id="descripcion"
                                    rows={3}
                                    placeholder="Describe brevemente el alcance y propósito del documento…"
                                    value={descripcion}
                                    onChange={(e) =>
                                        setDescripcion(
                                            e.target.value,
                                        )
                                    }
                                />

                            </div>


                            <Separator />


                            {/* =================================================
                             * ARCHIVO
                             * ================================================= */}

                            <div className="space-y-3 pt-2">

                                <div className="flex flex-col">

                                    <Label className="text-sm font-medium">

                                        Archivo Adjunto{" "}

                                        <span className="text-destructive">
                                            *
                                        </span>

                                    </Label>


                                    <span className="text-xs text-muted-foreground mb-2">

                                        Por seguridad institucional, solo se permiten formatos PDF, Word o Excel.

                                    </span>

                                </div>


                                <DropzoneArea
                                    selectedFile={archivo}

                                    onFileSelect={
                                        manejarArchivoSeleccionado
                                    }

                                    accept={{
                                        "application/pdf": [
                                            ".pdf",
                                        ],

                                        "application/msword": [
                                            ".doc",
                                        ],

                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                                            [".docx"],

                                        "application/vnd.ms-excel": [
                                            ".xls",
                                        ],

                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
                                            [".xlsx"],
                                    }}

                                    maxSize={
                                        15 *
                                        1024 *
                                        1024
                                    }
                                />

                            </div>

                        </CardContent>

                    </Card>


                    {/* =================================================
                     * TARJETA 2
                     * ================================================= */}

                    <Card>

                        <CardHeader>

                            <CardTitle className="text-base font-bold">

                                Clasificación y visibilidad

                            </CardTitle>

                        </CardHeader>


                        <CardContent className="space-y-5">


                            <div className="grid gap-4 sm:grid-cols-3">


                                {/* =================================================
                                 * ÁREA
                                 * ================================================= */}

                                <div className="space-y-1.5">

                                    <Label>

                                        Área responsable{" "}

                                        <span className="text-destructive">
                                            *
                                        </span>

                                    </Label>


                                    <Select
                                        value={areaId}
                                        onValueChange={(value) => {

                                            setAreaId(
                                                value,
                                            );

                                            setSub_procesoId(
                                                "",
                                            );

                                        }}
                                    >

                                        <SelectTrigger>

                                            <SelectValue placeholder="Seleccionar…" />

                                        </SelectTrigger>


                                        <SelectContent>

                                            {areas.map(
                                                (a) => {

                                                    const {
                                                        icono:
                                                            Icono,
                                                        color,
                                                    } =
                                                        obtenerIconoArea(
                                                            a.nombre,
                                                        );


                                                    return (

                                                        <SelectItem
                                                            key={
                                                                a.id
                                                            }
                                                            value={
                                                                a.id
                                                            }
                                                        >

                                                            <div className="flex items-center gap-2">

                                                                <Icono
                                                                    className={`size-4 ${color}`}
                                                                />

                                                                <span>
                                                                    {
                                                                        a.nombre
                                                                    }
                                                                </span>

                                                            </div>

                                                        </SelectItem>

                                                    );

                                                },
                                            )}

                                        </SelectContent>

                                    </Select>

                                </div>


                                {/* =================================================
                                 * SUBPROCESO
                                 * ================================================= */}

                                <div className="space-y-1.5">

                                    <Label>

                                        Subproceso{" "}

                                        <span className="text-destructive">
                                            *
                                        </span>

                                    </Label>


                                    {/* EL FIX: AGREGAMOS KEY PARA FORZAR EL REMONTAJE DE RADIX UI CUANDO CAMBIA EL ÁREA */}
                                    <Select
                                        key={`select-subproceso-${areaId || "vacio"}`}
                                        value={
                                            subProcesoId
                                        }
                                        onValueChange={
                                            setSub_procesoId
                                        }
                                    >

                                        <SelectTrigger>

                                            <SelectValue placeholder="Seleccionar…" />

                                        </SelectTrigger>


                                        <SelectContent>

                                            {subProcesosFiltrados.map(
                                                (c) => {

                                                    const {
                                                        icono:
                                                            Icono,
                                                        color,
                                                    } =
                                                        obtenerIconoSubProceso(
                                                            c.nombre,
                                                        );


                                                    return (

                                                        <SelectItem
                                                            key={
                                                                c.id
                                                            }
                                                            value={
                                                                c.id
                                                            }
                                                        >

                                                            <div className="flex items-center gap-2">

                                                                <Icono
                                                                    className={`size-4 ${color}`}
                                                                />

                                                                <span>
                                                                    {
                                                                        c.nombre
                                                                    }
                                                                </span>

                                                            </div>

                                                        </SelectItem>

                                                    );

                                                },
                                            )}

                                        </SelectContent>

                                    </Select>

                                </div>


                                {/* =================================================
                                 * TIPO
                                 * ================================================= */}

                                <div className="space-y-1.5">

                                    <Label>

                                        Tipo de documento{" "}

                                        <span className="text-destructive">
                                            *
                                        </span>

                                    </Label>


                                    <Select
                                        value={tipoId}
                                        onValueChange={
                                            setTipoId
                                        }
                                    >

                                        <SelectTrigger>

                                            <SelectValue placeholder="Seleccionar…" />

                                        </SelectTrigger>


                                        <SelectContent>

                                            {tipos.map(
                                                (t) => {
                                                    const { icono: Icono, color } = obtenerIconoFormato(t.nombre);

                                                    return (

                                                        <SelectItem
                                                            key={
                                                                t.id
                                                            }
                                                            value={
                                                                t.id
                                                            }
                                                        >

                                                            <div className="flex items-center gap-2">

                                                                <Icono className={`size-4 ${color}`} />

                                                                <span>
                                                                    {
                                                                        t.nombre
                                                                    }
                                                                </span>

                                                            </div>

                                                        </SelectItem>

                                                    );
                                                }
                                            )}

                                        </SelectContent>

                                    </Select>

                                </div>

                            </div>


                            {/* =================================================
                             * ÁREAS AUTORIZADAS
                             * ================================================= */}

                            <div className="space-y-2 pt-2">

                                <Label>

                                    Áreas autorizadas para visualizar

                                </Label>


                                <AreasAutorizadas
                                    idCheckbox="publicar-visible-todas"
                                    areas={areas}
                                    seleccionadas={
                                        autorizadas
                                    }
                                    onChange={
                                        setAutorizadas
                                    }
                                    visibleTodas={
                                        visibleTodas
                                    }
                                    onVisibleTodas={
                                        setVisibleTodas
                                    }
                                />

                            </div>

                        </CardContent>

                    </Card>


                    {/* =================================================
                     * TARJETA 3
                     * ================================================= */}

                    <Card>

                        <CardHeader>

                            <CardTitle className="text-base font-bold">

                                Versión y estado

                            </CardTitle>

                        </CardHeader>


                        <CardContent className="space-y-6">


                            <div className="grid gap-4 sm:grid-cols-3">


                                {/* Versión */}

                                <div className="space-y-1.5">

                                    <Label htmlFor="version">

                                        Versión

                                    </Label>


                                    <Input
                                        id="version"
                                        value={version}
                                        onChange={(e) =>
                                            setVersion(e.target.value)
                                        }
                                    />
                                </div>
                            </div>
                        </CardContent>
                    </Card>
                </form>
            </div>
        </AppShell>
    );
}
import { Link, createFileRoute, useParams } from "@tanstack/react-router";
import type { Icon } from "@tabler/icons-react";
import type { LucideIcon } from "lucide-react";
import { ArrowLeft, Download, History, Pencil, Upload } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import { toast } from "sonner";

import { DocumentoEstadoBadge } from "@/components/documentos-consulta-ui";
import { AppShell } from "@/components/AppShell";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { ApiError } from "@/lib/api";
import {
    dispararDescargaEnNavegador,
    etiquetasAlcance,
    formatFechaDocumento,
} from "@/lib/documentos-consulta-shared";
import { obtenerIconoFormato } from "@/lib/iconos-formatos";
import { obtenerIconoArea } from "@/lib/iconos-areas";
import { obtenerIconoSubProceso } from "@/lib/iconos-subprocesos";
import {
    descargarVersionVigente,
    obtenerDocumento,
    type DocumentoDetalle,
} from "@/lib/documentos-api";
import { useIntranet } from "@/lib/store";

export const Route = createFileRoute("/app/documento/$id")({
    head: () => ({
        meta: [
            { title: "Detalle del documento — Intranet documental" },
            {
                name: "description",
                content: "Ficha del documento con metadatos, alcance, estado y descarga de la versión vigente.",
            },
            { property: "og:title", content: "Detalle del documento — Intranet documental" },
            {
                property: "og:description",
                content: "Ficha del documento con metadatos, alcance, estado y descarga de la versión vigente.",
            },
        ],
    }),
    component: DetalleDocumento,
});

function DetalleDocumento() {
    const { id } = useParams({ from: "/app/documento/$id" });
    const { permisos } = useIntranet();

    const [documento, setDocumento] = useState<DocumentoDetalle | null>(null);
    const [cargando, setCargando] = useState(true);
    const [noDisponible, setNoDisponible] = useState(false);
    const [errorCarga, setErrorCarga] = useState<string | null>(null);
    const [descargando, setDescargando] = useState(false);
    const requestIdRef = useRef(0);

    const cargarDocumento = useCallback(async () => {
        const requestId = ++requestIdRef.current;
        setCargando(true);
        setNoDisponible(false);
        setErrorCarga(null);
        setDocumento(null);

        try {
            const detalle = await obtenerDocumento(id);
            if (requestId !== requestIdRef.current) return;
            setDocumento(detalle);
        } catch (err) {
            if (requestId !== requestIdRef.current) return;
            if (err instanceof ApiError && (err.status === 404 || err.status === 403)) {
                setNoDisponible(true);
                return;
            }
            const mensaje =
                err instanceof ApiError
                    ? err.message
                    : "No fue posible cargar el documento.";
            setErrorCarga(mensaje);
        } finally {
            if (requestId === requestIdRef.current) {
                setCargando(false);
            }
        }
    }, [id]);

    useEffect(() => {
        cargarDocumento();
        return () => {
            requestIdRef.current += 1;
        };
    }, [cargarDocumento]);

    const descargarDocumento = async () => {
        if (!documento) return;

        setDescargando(true);
        try {
            const { blob, nombreArchivo } = await descargarVersionVigente(documento.id);
            const nombre = nombreArchivo ?? documento.nombreArchivoOriginal ?? documento.codigo;
            dispararDescargaEnNavegador(blob, nombre);
        } catch (err) {
            const mensaje =
                err instanceof ApiError
                    ? err.message
                    : "No fue posible descargar el documento.";
            toast.error(mensaje);
        } finally {
            setDescargando(false);
        }
    };

    if (cargando) {
        return (
            <AppShell titulo="Detalle del documento">
                <Card>
                    <CardContent className="py-14 text-center text-sm text-muted-foreground">
                        Cargando documento...
                    </CardContent>
                </Card>
            </AppShell>
        );
    }

    if (noDisponible) {
        return (
            <AppShell titulo="Documento no disponible">
                <Button asChild variant="ghost" size="sm" className="-ml-2 mb-4 gap-1.5">
                    <Link to="/app/documentos">
                        <ArrowLeft className="size-4" /> Volver
                    </Link>
                </Button>
                <Card>
                    <CardContent className="py-14 text-center">
                        <p className="text-sm font-medium">Documento no disponible</p>
                        <p className="mt-2 text-sm text-muted-foreground">
                            El documento no existe o no está disponible para tu usuario.
                        </p>
                    </CardContent>
                </Card>
            </AppShell>
        );
    }

    if (errorCarga || !documento) {
        return (
            <AppShell titulo="Detalle del documento">
                <Button asChild variant="ghost" size="sm" className="-ml-2 mb-4 gap-1.5">
                    <Link to="/app/documentos">
                        <ArrowLeft className="size-4" /> Volver
                    </Link>
                </Button>
                <Card>
                    <CardContent className="space-y-3 py-14 text-center">
                        <p className="text-sm font-medium">{errorCarga ?? "Error inesperado."}</p>
                        <Button variant="outline" size="sm" onClick={cargarDocumento}>
                            Reintentar
                        </Button>
                    </CardContent>
                </Card>
            </AppShell>
        );
    }

    const { icono: IconoTipo, color: colorTipo } = obtenerIconoFormato(
        documento.tipoDocumentoNombre,
    );
    const { icono: IconoArea, color: colorArea } = obtenerIconoArea(documento.areaNombre);
    const { icono: IconoSub, color: colorSub } = obtenerIconoSubProceso(
        documento.subprogramaNombre,
    );

    return (
        <AppShell titulo="Detalle del documento" descripcion={documento.codigo}>
            <Button asChild variant="ghost" size="sm" className="-ml-2 gap-1.5">
                <Link to="/app/documentos">
                    <ArrowLeft className="size-4" /> Volver
                </Link>
            </Button>

            <div className="grid gap-4 lg:grid-cols-3">
                <Card className="lg:col-span-2">
                    <CardHeader className="flex-row items-start justify-between gap-4 space-y-0">
                        <div className="flex min-w-0 items-start gap-3">
                            <div className="flex size-11 shrink-0 items-center justify-center rounded-md bg-secondary">
                                <IconoTipo className={`size-5 ${colorTipo}`} />
                            </div>
                            <div className="min-w-0">
                                <CardTitle className="text-lg leading-snug break-words">
                                    {documento.titulo}
                                </CardTitle>
                                <p className="mt-1 font-mono text-xs text-muted-foreground">
                                    {documento.codigo}
                                </p>
                            </div>
                        </div>
                        <DocumentoEstadoBadge estado={documento.estado} />
                    </CardHeader>
                    <CardContent className="space-y-5">
                        <p className="text-sm leading-relaxed text-muted-foreground break-words">
                            {documento.descripcion?.trim() ? documento.descripcion : "Sin descripción"}
                        </p>
                        <Separator />
                        <dl className="grid gap-4 sm:grid-cols-2">
                            <Campo k="Código" v={documento.codigo} mono />
                            <Campo
                                k="Área responsable"
                                v={documento.areaNombre}
                                icono={IconoArea}
                                colorIcono={colorArea}
                            />
                            <Campo
                                k="Descripción de la publicación"
                                v={
                                    documento.descripcionVersionActual?.trim()
                                        ? documento.descripcionVersionActual
                                        : "Sin descripción"
                                }
                            />
                            <Campo
                                k="Subproceso"
                                v={documento.subprogramaNombre}
                                icono={IconoSub}
                                colorIcono={colorSub}
                            />
                            <Campo
                                k="Tipo de documento"
                                v={documento.tipoDocumentoNombre}
                                icono={IconoTipo}
                                colorIcono={colorTipo}
                            />
                            <Campo k="Alcance" v={etiquetasAlcance[documento.alcance]} />
                            <Campo
                                k="Versión vigente"
                                v={String(documento.numeroVersionActual)}
                            />
                            <Campo
                                k="Fecha de publicación"
                                v={formatFechaDocumento(documento.fechaPublicacionVersion)}
                            />
                            <Campo
                                k="Última actualización"
                                v={formatFechaDocumento(documento.fechaActualizacion)}
                            />
                        </dl>
                        <Separator />
                        <VisibilidadDocumento documento={documento} />
                    </CardContent>
                </Card>

                <Card className="h-fit">
                    <CardHeader>
                        <CardTitle className="text-base">Acciones</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-2">
                        <Button
                            variant="outline"
                            className="w-full justify-between"
                            disabled={descargando}
                            onClick={descargarDocumento}
                        >
                            {descargando ? "Descargando..." : "Descargar"}
                            <Download className="size-4" />
                        </Button>
                        {permisos.actualizarDocumentos && documento.estado !== "OBSOLETO" ? (
                            <Button asChild variant="outline" className="w-full justify-between">
                                <Link to="/app/documento/$id/editar" params={{ id }}>
                                    Editar publicación
                                    <Pencil className="size-4" />
                                </Link>
                            </Button>
                        ) : null}
                        {permisos.actualizarDocumentos && documento.estado !== "OBSOLETO" ? (
                            <Button asChild variant="outline" className="w-full justify-between">
                                <Link to="/app/documento/$id/actualizar" params={{ id }}>
                                    Actualizar documento
                                    <Upload className="size-4" />
                                </Link>
                            </Button>
                        ) : null}
                        {permisos.verHistorialGlobal ? (
                            <Button asChild variant="outline" className="w-full justify-between">
                                <Link to="/app/documento/$id/historial" params={{ id }}>
                                    Historial
                                    <History className="size-4" />
                                </Link>
                            </Button>
                        ) : null}
                    </CardContent>
                </Card>
            </div>
        </AppShell>
    );
}

function VisibilidadDocumento({ documento }: { documento: DocumentoDetalle }) {
    const alcance = documento.alcance;

    if (alcance === "GLOBAL") {
        return (
            <div className="space-y-1">
                <p className="text-xs uppercase tracking-wide text-muted-foreground">Visible para</p>
                <p className="text-sm font-medium">Todas las áreas</p>
            </div>
        );
    }

    if (alcance === "AREA_RESPONSABLE") {
        return (
            <div className="space-y-1">
                <p className="text-xs uppercase tracking-wide text-muted-foreground">Visible para</p>
                <p className="text-sm font-medium">Área responsable</p>
            </div>
        );
    }

    const { icono: IconoArea, color: colorArea } = obtenerIconoArea(documento.areaNombre);

    return (
        <div className="space-y-3">
            <div>
                <p className="text-xs uppercase tracking-wide text-muted-foreground">Área responsable</p>
                <p className="mt-1 flex items-center gap-2 text-sm font-medium">
                    <IconoArea className={`size-4 shrink-0 ${colorArea}`} />
                    {documento.areaNombre}
                </p>
            </div>
            <div>
                <p className="text-xs uppercase tracking-wide text-muted-foreground">
                    Áreas adicionales
                </p>
                {documento.areasAdicionales.length > 0 ? (
                    <div className="mt-2 flex flex-wrap gap-2">
                        {documento.areasAdicionales.map((area) => {
                            const { icono: Icono, color } = obtenerIconoArea(area.nombre);

                            return (
                                <Badge key={area.id} variant="secondary" className="gap-1.5">
                                    <Icono className={`size-3.5 shrink-0 ${color}`} />
                                    {area.nombre}
                                </Badge>
                            );
                        })}
                    </div>
                ) : (
                    <p className="mt-1 text-sm text-muted-foreground">Ninguna</p>
                )}
            </div>
        </div>
    );
}

function Campo({
    k,
    v,
    mono = false,
    icono: Icono,
    colorIcono,
}: {
    k: string;
    v: string;
    mono?: boolean;
    icono?: LucideIcon | Icon;
    colorIcono?: string;
}) {
    return (
        <div>
            <dt className="text-xs uppercase tracking-wide text-muted-foreground">{k}</dt>
            <dd className={`mt-1 text-sm font-medium break-words ${mono ? "font-mono" : ""}`}>
                {Icono ? (
                    <span className="flex items-center gap-1.5">
                        <Icono className={`size-4 shrink-0 ${colorIcono ?? ""}`} />
                        <span>{v}</span>
                    </span>
                ) : (
                    v
                )}
            </dd>
        </div>
    );
}

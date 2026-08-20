import { Link, createFileRoute, useParams } from "@tanstack/react-router";
import { ArrowLeft, Download, FileText } from "lucide-react";
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
import {
    descargarVersionVigente,
    obtenerDocumento,
    type DocumentoDetalle,
} from "@/lib/documentos-api";

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
                            <div className="flex size-11 shrink-0 items-center justify-center rounded-md bg-secondary text-secondary-foreground">
                                <FileText className="size-5" />
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
                            <Campo k="Área responsable" v={documento.areaNombre} />
                            <Campo k="Subprograma" v={documento.subprogramaNombre} />
                            <Campo k="Tipo de documento" v={documento.tipoDocumentoNombre} />
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
                    <CardContent>
                        <Button
                            variant="outline"
                            className="w-full justify-between"
                            disabled={descargando}
                            onClick={descargarDocumento}
                        >
                            {descargando ? "Descargando..." : "Descargar"}
                            <Download className="size-4" />
                        </Button>
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

    return (
        <div className="space-y-3">
            <div>
                <p className="text-xs uppercase tracking-wide text-muted-foreground">Área responsable</p>
                <p className="mt-1 text-sm font-medium">{documento.areaNombre}</p>
            </div>
            <div>
                <p className="text-xs uppercase tracking-wide text-muted-foreground">
                    Áreas adicionales
                </p>
                {documento.areasAdicionales.length > 0 ? (
                    <div className="mt-2 flex flex-wrap gap-2">
                        {documento.areasAdicionales.map((area) => (
                            <Badge key={area.id} variant="secondary">
                                {area.nombre}
                            </Badge>
                        ))}
                    </div>
                ) : (
                    <p className="mt-1 text-sm text-muted-foreground">Ninguna</p>
                )}
            </div>
        </div>
    );
}

function Campo({ k, v, mono = false }: { k: string; v: string; mono?: boolean }) {
    return (
        <div>
            <dt className="text-xs uppercase tracking-wide text-muted-foreground">{k}</dt>
            <dd className={`mt-1 text-sm font-medium break-words ${mono ? "font-mono" : ""}`}>{v}</dd>
        </div>
    );
}

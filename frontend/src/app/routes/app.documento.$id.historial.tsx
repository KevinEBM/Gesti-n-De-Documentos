import { Link, createFileRoute, useParams } from "@tanstack/react-router";
import { ArrowLeft } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";

import { DocumentoEstadoBadge } from "@/components/documentos-consulta-ui";
import { AppShell } from "@/components/AppShell";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { ApiError } from "@/lib/api";
import { formatFechaDocumento } from "@/lib/documentos-consulta-shared";
import {
    listarVersionesDocumento,
    obtenerDocumento,
    type DocumentoDetalle,
    type VersionHistorica,
} from "@/lib/documentos-api";
import { useIntranet } from "@/lib/store";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/app/documento/$id/historial")({
    head: () => ({
        meta: [{ title: "Historial de versiones — Intranet documental" }],
    }),
    component: HistorialVersionesPage,
});

function formatearTamano(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

function HistorialVersionesPage() {
    const { id } = useParams({ from: "/app/documento/$id/historial" });
    const { permisos } = useIntranet();

    const [documento, setDocumento] = useState<DocumentoDetalle | null>(null);
    const [versiones, setVersiones] = useState<VersionHistorica[] | null>(null);
    const [cargando, setCargando] = useState(true);
    const [noDisponible, setNoDisponible] = useState(false);
    const [errorCarga, setErrorCarga] = useState<string | null>(null);

    const requestIdRef = useRef(0);

    const cargarDatos = useCallback(async () => {
        const requestId = ++requestIdRef.current;
        setCargando(true);
        setNoDisponible(false);
        setErrorCarga(null);
        setDocumento(null);
        setVersiones(null);

        try {
            const [detalle, historico] = await Promise.all([
                obtenerDocumento(id),
                listarVersionesDocumento(id),
            ]);
            if (requestId !== requestIdRef.current) return;
            setDocumento(detalle);
            setVersiones(historico);
        } catch (err) {
            if (requestId !== requestIdRef.current) return;
            if (err instanceof ApiError && (err.status === 404 || err.status === 403)) {
                setNoDisponible(true);
                return;
            }
            setErrorCarga(
                err instanceof ApiError
                    ? err.message
                    : "No fue posible cargar el historial de versiones.",
            );
        } finally {
            if (requestId === requestIdRef.current) {
                setCargando(false);
            }
        }
    }, [id]);

    useEffect(() => {
        if (!permisos.verHistorialGlobal) return;
        cargarDatos();
        return () => {
            requestIdRef.current += 1;
        };
    }, [cargarDatos, permisos.verHistorialGlobal]);

    if (!permisos.verHistorialGlobal) {
        return (
            <AppShell titulo="Historial de versiones">
                <Card>
                    <CardContent className="py-14 text-center text-sm text-muted-foreground">
                        No cuentas con permisos para consultar el historial de versiones.
                    </CardContent>
                </Card>
            </AppShell>
        );
    }

    if (cargando) {
        return (
            <AppShell titulo="Historial de versiones">
                <Card>
                    <CardContent className="py-14 text-center text-sm text-muted-foreground">
                        Cargando historial...
                    </CardContent>
                </Card>
            </AppShell>
        );
    }

    if (noDisponible) {
        return (
            <AppShell titulo="Historial de versiones">
                <Button asChild variant="ghost" size="sm" className="-ml-2 mb-4 gap-1.5">
                    <Link to="/app/documentos">
                        <ArrowLeft className="size-4" /> Volver
                    </Link>
                </Button>
                <Card>
                    <CardContent className="py-14 text-center">
                        <p className="text-sm font-medium">Historial no disponible</p>
                        <p className="mt-2 text-sm text-muted-foreground">
                            El documento no existe o no está disponible para tu usuario.
                        </p>
                    </CardContent>
                </Card>
            </AppShell>
        );
    }

    if (errorCarga || !documento || versiones === null) {
        return (
            <AppShell titulo="Historial de versiones">
                <Button asChild variant="ghost" size="sm" className="-ml-2 mb-4 gap-1.5">
                    <Link to="/app/documento/$id" params={{ id }}>
                        <ArrowLeft className="size-4" /> Volver al detalle
                    </Link>
                </Button>
                <Card>
                    <CardContent className="space-y-3 py-14 text-center">
                        <p className="text-sm font-medium">{errorCarga ?? "Error inesperado."}</p>
                        <Button variant="outline" size="sm" onClick={cargarDatos}>
                            Reintentar
                        </Button>
                    </CardContent>
                </Card>
            </AppShell>
        );
    }

    return (
        <AppShell titulo="Historial de versiones" descripcion={documento.codigo}>
            <Button asChild variant="ghost" size="sm" className="-ml-2 mb-4 gap-1.5">
                <Link to="/app/documento/$id" params={{ id }}>
                    <ArrowLeft className="size-4" /> Volver al detalle
                </Link>
            </Button>

            <Card className="mb-6">
                <CardHeader className="pb-3">
                    <CardTitle className="text-base">{documento.titulo}</CardTitle>
                    <CardDescription className="font-mono">{documento.codigo}</CardDescription>
                </CardHeader>
                <CardContent className="flex flex-wrap items-center gap-3 text-sm">
                    <DocumentoEstadoBadge estado={documento.estado} />
                    <span className="text-muted-foreground">
                        Versión vigente:{" "}
                        <span className="font-medium text-foreground">
                            {documento.numeroVersionActual}
                        </span>
                    </span>
                </CardContent>
            </Card>

            {versiones.length === 0 ? (
                <Card>
                    <CardContent className="py-10 text-center text-sm text-muted-foreground">
                        No hay versiones registradas para este documento.
                    </CardContent>
                </Card>
            ) : (
                <div className="space-y-4">
                    {versiones.map((version) => (
                        <Card
                            key={version.id}
                            className={cn(
                                version.vigente && "border-emerald-500/40 bg-emerald-500/5",
                            )}
                        >
                            <CardContent className="space-y-3 py-5">
                                <div className="flex flex-wrap items-center gap-2">
                                    <h3 className="text-base font-semibold">
                                        Versión {version.numeroVersion}
                                    </h3>
                                    {version.vigente ? (
                                        <Badge className="border-emerald-500/30 bg-emerald-500/15 text-emerald-700 hover:bg-emerald-500/15">
                                            Vigente
                                        </Badge>
                                    ) : null}
                                </div>

                                <p className="text-sm text-muted-foreground">
                                    {formatFechaDocumento(version.fechaPublicacion)}
                                </p>

                                <div>
                                    <p className="text-xs uppercase tracking-wide text-muted-foreground">
                                        Descripción del cambio
                                    </p>
                                    <p className="mt-0.5 text-sm leading-relaxed">
                                        {version.descripcionCambio?.trim()
                                            ? version.descripcionCambio
                                            : "Sin descripción del cambio"}
                                    </p>
                                </div>

                                <Separator />

                                <dl className="grid gap-2 text-sm sm:grid-cols-2">
                                    <div>
                                        <dt className="text-xs uppercase tracking-wide text-muted-foreground">
                                            Publicado por
                                        </dt>
                                        <dd className="mt-0.5 font-medium">
                                            {version.publicadoPorNombre}
                                        </dd>
                                    </div>
                                    <div>
                                        <dt className="text-xs uppercase tracking-wide text-muted-foreground">
                                            Archivo
                                        </dt>
                                        <dd className="mt-0.5 break-all font-medium">
                                            {version.nombreArchivoOriginal}
                                        </dd>
                                        <dd className="text-xs text-muted-foreground">
                                            {formatearTamano(version.tamanoBytes)} ·{" "}
                                            {version.tipoMime || "tipo desconocido"}
                                        </dd>
                                    </div>
                                </dl>
                            </CardContent>
                        </Card>
                    ))}
                </div>
            )}
        </AppShell>
    );
}

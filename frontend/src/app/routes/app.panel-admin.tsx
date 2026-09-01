import { Link, createFileRoute } from "@tanstack/react-router";
import {
    Activity,
    Building2,
    FileStack,
    Files,
    Settings2,
    Upload,
    Users,
} from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";

import { AppShell } from "@/components/AppShell";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ApiError } from "@/lib/api";
import {
    etiquetaTipoActividad,
    obtenerActividadReciente,
    obtenerDashboard,
    type ActividadDocumental,
    type DashboardMetricas,
} from "@/lib/dashboard-api";
import { formatFechaDocumento } from "@/lib/documentos-consulta-shared";
import { useIntranet } from "@/lib/store";

export const Route = createFileRoute("/app/panel-admin")({
    head: () => ({
        meta: [
            { title: "Panel administrativo — Intranet documental" },
            {
                name: "description",
                content: "Indicadores de usuarios activos y documentos publicados, con accesos a la gestión documental.",
            },
            { property: "og:title", content: "Panel administrativo — Intranet documental" },
            { property: "og:description", content: "Usuarios activos, documentos publicados y actividad reciente." },
        ],
    }),
    component: PanelAdmin,
});

function PanelAdmin() {
    const { permisos } = useIntranet();

    const [metricas, setMetricas] = useState<DashboardMetricas | null>(null);
    const [actividad, setActividad] = useState<ActividadDocumental[] | null>(null);
    const [cargando, setCargando] = useState(true);
    const [errorCarga, setErrorCarga] = useState<string | null>(null);

    const requestIdRef = useRef(0);

    const cargarDatos = useCallback(async () => {
        const requestId = ++requestIdRef.current;
        setCargando(true);
        setErrorCarga(null);
        setMetricas(null);
        setActividad(null);

        try {
            const [dashboard, actividadReciente] = await Promise.all([
                obtenerDashboard(),
                obtenerActividadReciente(),
            ]);
            if (requestId !== requestIdRef.current) return;
            setMetricas(dashboard);
            setActividad(actividadReciente);
        } catch (err) {
            if (requestId !== requestIdRef.current) return;
            setErrorCarga(
                err instanceof ApiError
                    ? err.message
                    : "No fue posible cargar el panel administrativo.",
            );
        } finally {
            if (requestId === requestIdRef.current) {
                setCargando(false);
            }
        }
    }, []);

    useEffect(() => {
        if (!permisos.gestionarUsuarios) return;
        cargarDatos();
        return () => {
            requestIdRef.current += 1;
        };
    }, [cargarDatos, permisos.gestionarUsuarios]);

    if (!permisos.gestionarUsuarios) {
        return (
            <AppShell titulo="Panel administrativo">
                <Card>
                    <CardContent className="py-14 text-center text-sm text-muted-foreground">
                        No cuentas con permisos para acceder al panel administrativo.
                    </CardContent>
                </Card>
            </AppShell>
        );
    }

    if (cargando) {
        return (
            <AppShell titulo="Panel administrativo">
                <Card>
                    <CardContent className="py-14 text-center text-sm text-muted-foreground">
                        Cargando panel administrativo...
                    </CardContent>
                </Card>
            </AppShell>
        );
    }

    if (errorCarga || !metricas || actividad === null) {
        return (
            <AppShell titulo="Panel administrativo">
                <Card>
                    <CardContent className="space-y-3 py-14 text-center">
                        <p className="text-sm font-medium">
                            {errorCarga ?? "Error inesperado al cargar el panel."}
                        </p>
                        <Button variant="outline" size="sm" onClick={cargarDatos}>
                            Reintentar
                        </Button>
                    </CardContent>
                </Card>
            </AppShell>
        );
    }

    return (
        <AppShell titulo="Panel administrativo" descripcion="Resumen general de la intranet documental">
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                <Metrica
                    icono={Files}
                    etiqueta="Documentos publicados"
                    valor={metricas.documentosPublicados}
                />
                <Metrica icono={Users} etiqueta="Usuarios activos" valor={metricas.usuariosActivos} />
                <Metrica
                    icono={Building2}
                    etiqueta="Áreas registradas"
                    valor={metricas.areasRegistradas}
                />
            </div>

            <Card>
                <CardHeader>
                    <CardTitle className="text-base">Accesos rápidos</CardTitle>
                </CardHeader>
                <CardContent className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                    <Acceso to="/app/publicar" label="Publicar documento" icono={Upload} />
                    <Acceso to="/app/gestion-documentos" label="Gestión de documentos" icono={FileStack} />
                    <Acceso to="/app/usuarios" label="Gestión de usuarios" icono={Users} />
                    <Acceso to="/app/parametrizacion" label="Parametrización" icono={Settings2} />
                </CardContent>
            </Card>

            <Card>
                <CardHeader className="flex-row items-center gap-2 space-y-0">
                    <Activity className="size-4 text-muted-foreground" />
                    <CardTitle className="text-base">Actividad reciente</CardTitle>
                </CardHeader>
                <CardContent className="space-y-1">
                    {actividad.length === 0 ? (
                        <p className="px-3 py-6 text-center text-sm text-muted-foreground">
                            No hay actividad documental reciente.
                        </p>
                    ) : (
                        actividad.map((item) => (
                            <div
                                key={`${item.documentoId}-${item.numeroVersion}-${item.fechaPublicacion}`}
                                className="flex items-start gap-3 rounded-md px-3 py-2.5 hover:bg-secondary"
                            >
                                <div className="min-w-0 flex-1">
                                    <p className="text-sm">
                                        <span className="font-medium">{item.publicadoPorNombre}</span>
                                        {" · "}
                                        {etiquetaTipoActividad(item.tipoActividad)}
                                    </p>
                                    <p className="truncate text-xs text-muted-foreground">
                                        {item.codigoDocumento} · {item.tituloDocumento} · Versión{" "}
                                        {item.numeroVersion}
                                        {item.descripcionCambio?.trim()
                                            ? ` · ${item.descripcionCambio}`
                                            : ""}
                                    </p>
                                </div>
                                <p className="shrink-0 text-xs text-muted-foreground">
                                    {formatFechaDocumento(item.fechaPublicacion)}
                                </p>
                            </div>
                        ))
                    )}
                </CardContent>
            </Card>
        </AppShell>
    );
}

function Metrica({ icono: Icono, etiqueta, valor }: { icono: typeof Files; etiqueta: string; valor: number }) {
    return (
        <Card>
            <CardContent className="flex items-center gap-4 py-6">
                <div className="flex size-12 items-center justify-center rounded-md bg-secondary text-secondary-foreground">
                    <Icono className="size-6" />
                </div>
                <div className="min-w-0">
                    <p className="text-3xl font-semibold leading-none">{valor}</p>
                    <p className="mt-2 text-sm leading-tight text-muted-foreground">{etiqueta}</p>
                </div>
            </CardContent>
        </Card>
    );
}

function Acceso({ to, label, icono: Icono }: { to: string; label: string; icono: typeof Files }) {
    return (
        <Button asChild variant="outline" className="h-auto w-full justify-between gap-2 whitespace-normal py-3 text-left">
            <Link to={to}>
                <span className="text-sm">{label}</span>
                <Icono className="size-4 shrink-0" />
            </Link>
        </Button>
    );
}

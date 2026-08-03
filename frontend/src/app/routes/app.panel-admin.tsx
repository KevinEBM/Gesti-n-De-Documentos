import { Link, createFileRoute } from "@tanstack/react-router";
import { Activity, FileStack, Files, Settings2, Upload, Users } from "lucide-react";

import { AppShell } from "@/components/AppShell";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
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
    const { usuarios, documentos, actividad, permisos } = useIntranet();

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

    const usuariosActivos = usuarios.filter((u) => u.activo).length;
    const publicados = documentos.filter((d) => d.estado === "publicado").length;

    return (
        <AppShell titulo="Panel administrativo" descripcion="Resumen general de la intranet documental">
            <div className="grid gap-4 sm:grid-cols-2">
                <Metrica icono={Users} etiqueta="Usuarios activos" valor={usuariosActivos} />
                <Metrica icono={Files} etiqueta="Documentos publicados" valor={publicados} />
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
                    {actividad.slice(0, 8).map((a) => (
                        <div key={a.id} className="flex items-start gap-3 rounded-md px-3 py-2.5 hover:bg-secondary">
                            <div className="min-w-0 flex-1">
                                <p className="text-sm">
                                    <span className="font-medium">{a.usuario}</span> · {a.accion}
                                </p>
                                <p className="truncate text-xs text-muted-foreground">{a.detalle}</p>
                            </div>
                            <p className="shrink-0 text-xs text-muted-foreground">{a.fecha}</p>
                        </div>
                    ))}
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

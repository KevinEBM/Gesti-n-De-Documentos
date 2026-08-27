import { Link, createFileRoute } from "@tanstack/react-router";
import { Files, Info, TriangleAlert, UserRound } from "lucide-react";
import { useEffect, useState } from "react";

import { AppShell } from "@/components/AppShell";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ApiError } from "@/lib/api";
import { etiquetaAreaPrincipal } from "@/lib/auth-storage";
import { descripcionRolInicio, etiquetaRol } from "@/lib/data";
import { obtenerIconoArea } from "@/lib/iconos-areas";
import { useIntranet } from "@/lib/store";

export const Route = createFileRoute("/app/inicio")({
    head: () => ({
        meta: [
            { title: "Inicio — Intranet documental" },
            {
                name: "description",
                content: "Pantalla de bienvenida con la información de tu cuenta en la intranet documental.",
            },
            { property: "og:title", content: "Inicio — Intranet documental" },
            {
                property: "og:description",
                content: "Bienvenida e información de la cuenta del usuario autenticado.",
            },
        ],
    }),
    component: Inicio,
});

function Inicio() {
    const { sesion, refrescarPerfil, cerrarSesion } = useIntranet();
    const [intento, setIntento] = useState(0);
    const [areaSinConfirmar, setAreaSinConfirmar] = useState(false);

    useEffect(() => {
        if (!sesion) {
            return;
        }

        let activo = true;

        const refrescar = async () => {
            try {
                await refrescarPerfil();
                if (activo) setAreaSinConfirmar(false);
            } catch (err) {
                if (!activo) return;
                if (err instanceof ApiError && err.status === 401) {
                    cerrarSesion();
                    return;
                }
                setAreaSinConfirmar(true);
            }
        };

        void refrescar();

        return () => {
            activo = false;
        };
    }, [sesion, refrescarPerfil, cerrarSesion, intento]);

    useEffect(() => {
        const alVolverVisible = () => {
            if (document.visibilityState === "visible") {
                setIntento((valor) => valor + 1);
            }
        };
        const alEnfocar = () => setIntento((valor) => valor + 1);

        document.addEventListener("visibilitychange", alVolverVisible);
        window.addEventListener("focus", alEnfocar);

        return () => {
            document.removeEventListener("visibilitychange", alVolverVisible);
            window.removeEventListener("focus", alEnfocar);
        };
    }, []);

    if (!sesion) return null;

    const nombreSaludo = sesion.nombre.trim() || etiquetaRol[sesion.rol];
    const etiquetaArea = etiquetaAreaPrincipal(sesion);
    const tieneAreaAsignada = Boolean(sesion.areaPrincipalNombre?.trim());
    const { icono: IconoArea, color: colorArea } = obtenerIconoArea(etiquetaArea);

    return (
        <AppShell titulo={`Bienvenido, ${nombreSaludo}`}>
            <div className="space-y-4">
                <Card>
                    <CardHeader>
                        <CardTitle className="text-base">Información de tu cuenta</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <dl className="grid gap-4 sm:grid-cols-2">
                            <div>
                                <dt className="text-xs uppercase tracking-wide text-muted-foreground">
                                    Nombre
                                </dt>
                                <dd className="mt-1 text-sm font-medium">{sesion.nombre}</dd>
                            </div>
                            <div>
                                <dt className="text-xs uppercase tracking-wide text-muted-foreground">
                                    Rol
                                </dt>
                                <dd className="mt-1 text-sm font-medium">{etiquetaRol[sesion.rol]}</dd>
                            </div>
                            <div className="sm:col-span-2">
                                <dt className="text-xs uppercase tracking-wide text-muted-foreground">
                                    Área
                                </dt>
                                <dd className="mt-1 flex items-center gap-2 text-sm font-medium">
                                    {tieneAreaAsignada ? (
                                        <IconoArea className={`size-4 shrink-0 ${colorArea}`} />
                                    ) : null}
                                    {etiquetaArea}
                                </dd>
                                {areaSinConfirmar ? (
                                    <p className="mt-1.5 flex flex-wrap items-center gap-1.5 text-xs text-muted-foreground">
                                        <TriangleAlert className="size-3.5 shrink-0" />
                                        No se pudo confirmar tu área con el servidor; este es el
                                        último dato conocido.
                                        <Button
                                            variant="link"
                                            className="h-auto p-0 text-xs"
                                            onClick={() => setIntento((valor) => valor + 1)}
                                        >
                                            Reintentar
                                        </Button>
                                    </p>
                                ) : null}
                            </div>
                        </dl>

                        <div className="pt-2">
                            <Button asChild className="gap-2">
                                <Link to="/app/documentos">
                                    Ir a la biblioteca <Files className="size-4" />
                                </Link>
                            </Button>
                        </div>
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader className="pb-3">
                        <CardTitle className="text-base">Acerca de SIG Plantar S.A.S.</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="grid gap-5 sm:grid-cols-2 sm:gap-6">
                            <div className="space-y-1.5">
                                <div className="flex items-center gap-2">
                                    <Info className="size-4 shrink-0 text-muted-foreground" />
                                    <h3 className="text-sm font-semibold">¿Qué es SIG Plantar S.A.S.?</h3>
                                </div>
                                <p className="text-sm leading-relaxed text-muted-foreground">
                                    SIG (Sistema Integrado de Gestión) es la plataforma interna de
                                    Plantar S.A.S. para consultar, organizar y mantener actualizada la
                                    documentación, facilitando el acceso según el rol y el área del
                                    usuario.
                                </p>
                            </div>
                            <div className="space-y-1.5">
                                <div className="flex items-center gap-2">
                                    <UserRound className="size-4 shrink-0 text-muted-foreground" />
                                    <h3 className="text-sm font-semibold">Tu rol</h3>
                                </div>
                                <p className="text-sm font-medium">{etiquetaRol[sesion.rol]}</p>
                                <p className="text-sm leading-relaxed text-muted-foreground">
                                    {descripcionRolInicio[sesion.rol]}
                                </p>
                            </div>
                        </div>
                    </CardContent>
                </Card>
            </div>
        </AppShell>
    );
}

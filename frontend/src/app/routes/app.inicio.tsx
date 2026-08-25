import { Link, createFileRoute } from "@tanstack/react-router";
import { Files, Info, UserRound } from "lucide-react";
import { useEffect } from "react";

import { AppShell } from "@/components/AppShell";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { listarAreas } from "@/lib/areas-api";
import { etiquetaAreaPrincipal } from "@/lib/auth-storage";
import { descripcionRolInicio, etiquetaRol } from "@/lib/data";
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
    const { sesion, sincronizarAreaDesdeCatalogo } = useIntranet();

    useEffect(() => {
        if (!sesion || sesion.rol === "administrador") {
            return;
        }

        let activo = true;

        const refrescarArea = async () => {
            try {
                const areas = await listarAreas();
                if (activo) {
                    sincronizarAreaDesdeCatalogo(areas);
                }
            } catch {
                // Si falla la API, se conserva el snapshot de sesión existente.
            }
        };

        void refrescarArea();

        return () => {
            activo = false;
        };
    }, [sesion, sincronizarAreaDesdeCatalogo]);

    if (!sesion) return null;

    const nombreSaludo = sesion.nombre.trim() || etiquetaRol[sesion.rol];

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
                                <dd className="mt-1 text-sm font-medium">
                                    {etiquetaAreaPrincipal(sesion)}
                                </dd>
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
                        <CardTitle className="text-base">Acerca de SID Plantar S.A.S.</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="grid gap-5 sm:grid-cols-2 sm:gap-6">
                            <div className="space-y-1.5">
                                <div className="flex items-center gap-2">
                                    <Info className="size-4 shrink-0 text-muted-foreground" />
                                    <h3 className="text-sm font-semibold">¿Qué es SID Plantar S.A.S.?</h3>
                                </div>
                                <p className="text-sm leading-relaxed text-muted-foreground">
                                    SID (Sistema Integrado Documental) es la plataforma interna de
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

import { Link, createFileRoute } from "@tanstack/react-router";
import { Files } from "lucide-react";

import { AppShell } from "@/components/AppShell";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { etiquetaAreaPrincipal } from "@/lib/auth-storage";
import { etiquetaRol } from "@/lib/data";
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
    const { sesion } = useIntranet();
    if (!sesion) return null;

    const primerNombre = sesion.nombre.split(" ")[0] || sesion.nombre;

    return (
        <AppShell titulo={`Bienvenido, ${primerNombre}`}>
            <Card>
                <CardHeader>
                    <CardTitle className="text-base">Información de tu cuenta</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <dl className="grid gap-4 sm:grid-cols-2">
                        <div>
                            <dt className="text-xs uppercase tracking-wide text-muted-foreground">Nombre</dt>
                            <dd className="mt-1 text-sm font-medium">{sesion.nombre}</dd>
                        </div>
                        <div>
                            <dt className="text-xs uppercase tracking-wide text-muted-foreground">Rol</dt>
                            <dd className="mt-1 text-sm font-medium">{etiquetaRol[sesion.rol]}</dd>
                        </div>
                        <div className="sm:col-span-2">
                            <dt className="text-xs uppercase tracking-wide text-muted-foreground">Área</dt>
                            <dd className="mt-1 text-sm font-medium">{etiquetaAreaPrincipal(sesion)}</dd>
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
        </AppShell>
    );
}

import { Link, createFileRoute } from "@tanstack/react-router";
import { ArrowRight/*, Bell*/, FileText, Files } from "lucide-react";

import { AppShell } from "@/components/AppShell";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useIntranet } from "@/lib/store";

export const Route = createFileRoute("/app/inicio")({
    head: () => ({
        meta: [
            { title: "Inicio — Intranet documental" },
            {
                name: "description",
                content: "Pantalla de bienvenida con los documentos vigentes recientes autorizados para tu área.",
            },
            { property: "og:title", content: "Inicio — Intranet documental" },
            { property: "og:description", content: "Documentos vigentes recientes autorizados para tu área." },
        ],
    }),
    component: Inicio,
});

function Inicio() {
    const { sesion, documentosVisibles/*, notificacionesVisibles*/, nombreArea, nombreTipo, nombreSub_Proceso } =
        useIntranet();
    if (!sesion) return null;

    const recientes = [...documentosVisibles]
        .sort((a, b) => b.fechaPublicacion.localeCompare(a.fechaPublicacion))
        .slice(0, 6);
  //  const pendientes = notificacionesVisibles.filter((n) => !n.leida);

    return (
        <AppShell titulo={`Bienvenido, ${sesion.nombre.split(" ")[0]}`} descripcion={`Área de ${nombreArea(sesion.areaId)}`}>
            <Card>
                <CardContent className="py-6">
                    <h2 className="text-lg font-semibold">Documentos internos de {nombreArea(sesion.areaId)}</h2>
                    <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
                        Aquí encuentras los documentos vigentes autorizados para tu área. Utiliza la biblioteca para buscar y
                        filtrar por categoría o tipo de documento.
                    </p>
                    <div className="mt-4 flex flex-wrap gap-2">
                        <Button asChild className="gap-2">
                            <Link to="/app/documentos">
                                Ir a la biblioteca <Files className="size-4" />
                            </Link>
                        </Button>
                        {/*      <Button asChild variant="outline" className="gap-2">
                            <Link to="/app/notificaciones">
                                Notificaciones
                                {pendientes.length > 0 && (
                                    <span className="rounded-full bg-destructive px-1.5 text-[10px] font-semibold text-destructive-foreground">
                    {pendientes.length}
                  </span>
                                )}
                                <Bell className="size-4" />
                            </Link>
                        </Button>*/}
                    </div>
                </CardContent>
            </Card>

            <Card>
                <CardHeader className="flex-row items-center justify-between space-y-0">
                    <CardTitle className="text-base">Documentos recientes de tu área</CardTitle>
                    <Button asChild variant="ghost" size="sm" className="gap-1.5">
                        <Link to="/app/documentos">
                            Ver todos <ArrowRight className="size-4" />
                        </Link>
                    </Button>
                </CardHeader>
                <CardContent className="space-y-1">
                    {recientes.map((d) => (
                        <Link
                            key={d.id}
                            to="/app/documento/$id"
                            params={{ id: d.id }}
                            className="flex items-center gap-3 rounded-md border border-transparent px-3 py-2.5 transition-colors hover:border-border hover:bg-secondary"
                        >
                            <div className="flex size-9 shrink-0 items-center justify-center rounded-md bg-secondary text-secondary-foreground">
                                <FileText className="size-4" />
                            </div>
                            <div className="min-w-0 flex-1">
                                <p className="truncate text-sm font-medium">{d.nombre}</p>
                                <p className="truncate text-xs text-muted-foreground">
                                    {nombreTipo(d.tipoId)} · {nombreSub_Proceso(d.subProcesoId)} · {nombreArea(d.areaId)}

                                </p>
                            </div>
                            <div className="hidden shrink-0 text-right sm:block">
                                <p className="text-xs font-medium">Versión vigente v{d.version}</p>
                                <p className="text-xs text-muted-foreground">{d.fechaPublicacion}</p>
                            </div>
                        </Link>
                    ))}
                    {recientes.length === 0 && (
                        <p className="py-8 text-center text-sm text-muted-foreground">
                            Aún no hay documentos autorizados para tu área.
                        </p>
                    )}
                </CardContent>
            </Card>
        </AppShell>
    );
}

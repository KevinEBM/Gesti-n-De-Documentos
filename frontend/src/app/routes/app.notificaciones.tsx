/*import { Link, createFileRoute } from "@tanstack/react-router";
import { Bell, BellOff, CheckCheck, FilePlus2, RefreshCw } from "lucide-react";
import { useState } from "react";

import { AppShell } from "@/components/AppShell";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useIntranet } from "@/lib/store";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/app/notificaciones")({
    head: () => ({
        meta: [
            { title: "Centro de notificaciones — Intranet documental" },
            {
                name: "description",
                content: "Avisos de documentos nuevos y nuevas versiones publicadas, con estado leído y no leído.",
            },
            { property: "og:title", content: "Centro de notificaciones — Intranet documental" },
            { property: "og:description", content: "Avisos de documentos nuevos y nuevas versiones publicadas." },
        ],
    }),
    component: Notificaciones,
});

function Notificaciones() {
    const { notificacionesVisibles: notificaciones, marcarLeida, marcarTodasLeidas, documentos } = useIntranet();
    const [filtro, setFiltro] = useState<"todas" | "no-leidas" | "leidas">("todas");

    const lista = notificaciones.filter((n) =>
        filtro === "todas" ? true : filtro === "leidas" ? n.leida : !n.leida,
    );
    const sinLeer = notificaciones.filter((n) => !n.leida).length;

    return (
        <AppShell
            titulo="Centro de notificaciones"
            descripcion={`${sinLeer} sin leer de ${notificaciones.length} en total`}
            acciones={
                <Button variant="outline" size="sm" className="gap-1.5" onClick={marcarTodasLeidas} disabled={sinLeer === 0}>
                    <CheckCheck className="size-4" /> Marcar todas
                </Button>
            }
        >
            <Tabs value={filtro} onValueChange={(v) => setFiltro(v as typeof filtro)}>
                <TabsList>
                    <TabsTrigger value="todas">Todas ({notificaciones.length})</TabsTrigger>
                    <TabsTrigger value="no-leidas">No leídas ({sinLeer})</TabsTrigger>
                    <TabsTrigger value="leidas">Leídas ({notificaciones.length - sinLeer})</TabsTrigger>
                </TabsList>
            </Tabs>

            <div className="space-y-3">
                {lista.map((n) => {
                    const doc = documentos.find((d) => d.id === n.documentoId);
                    const Icono = n.tipo === "nuevo" ? FilePlus2 : RefreshCw;
                    return (
                        <Card key={n.id} className={cn(!n.leida && "border-primary/30 bg-accent/25")}>
                            <CardContent className="flex flex-col gap-4 py-4 sm:flex-row sm:items-center">
                                <div
                                    className={cn(
                                        "flex size-10 shrink-0 items-center justify-center rounded-md",
                                        n.tipo === "nuevo"
                                            ? "bg-success/12 text-success"
                                            : "bg-secondary text-secondary-foreground",
                                    )}
                                >
                                    <Icono className="size-5" />
                                </div>
                                <div className="min-w-0 flex-1">
                                    <div className="flex items-center gap-2">
                                        <p className="text-sm font-medium">{n.titulo}</p>
                                        {!n.leida && <span className="size-2 rounded-full bg-primary" />}
                                    </div>
                                    <p className="mt-0.5 text-sm text-muted-foreground">{n.mensaje}</p>
                                    <p className="mt-1 text-xs text-muted-foreground">{n.fecha}</p>
                                </div>
                                <div className="flex shrink-0 gap-2">
                                    {doc && (
                                        <Button asChild size="sm" variant="outline">
                                            <Link to="/app/documento/$id" params={{ id: doc.id }}>
                                                Ir al documento
                                            </Link>
                                        </Button>
                                    )}
                                    <Button size="sm" variant="ghost" onClick={() => marcarLeida(n.id)} disabled={n.leida}>
                                        {n.leida ? "Leída" : "Marcar como leída"}
                                    </Button>
                                </div>
                            </CardContent>
                        </Card>
                    );
                })}

                {lista.length === 0 && (
                    <Card>
                        <CardContent className="flex flex-col items-center py-14 text-center">
                            {filtro === "no-leidas" ? (
                                <BellOff className="mb-3 size-8 text-muted-foreground" />
                            ) : (
                                <Bell className="mb-3 size-8 text-muted-foreground" />
                            )}
                            <p className="text-sm font-medium">Sin notificaciones en esta vista</p>
                        </CardContent>
                    </Card>
                )}
            </div>
        </AppShell>
    );
}
*/
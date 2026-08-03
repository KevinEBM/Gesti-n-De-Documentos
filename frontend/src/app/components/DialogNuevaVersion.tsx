import { useEffect, useState } from "react";
import { toast } from "sonner";

import { AreasAutorizadas } from "@/components/AreasAutorizadas";
import { Button } from "@/components/ui/button";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import type { Documento } from "@/lib/data";
import { sugerirVersion, useIntranet } from "@/lib/store";

/** Formulario de publicación de una nueva versión de un documento existente. */
export function DialogNuevaVersion({ doc, onClose }: { doc: Documento | null; onClose: () => void }) {
    const { areas, nuevaVersion, sesion } = useIntranet();
    const [numero, setNumero] = useState("");
    const [archivo, setArchivo] = useState("");
    const [fecha, setFecha] = useState(new Date().toISOString().slice(0, 10));
    const [notas, setNotas] = useState("");
    const [visibleTodas, setVisibleTodas] = useState(false);
    const [autorizadas, setAutorizadas] = useState<string[]>([]);

    useEffect(() => {
        if (!doc) return;
        setNumero(sugerirVersion(doc.version));
        setArchivo("");
        setFecha(new Date().toISOString().slice(0, 10));
        setNotas("");
        setVisibleTodas(doc.visibleTodas);
        setAutorizadas(doc.areasAutorizadas);
    }, [doc]);

    const publicar = () => {
        if (!doc) return;
        if (!/^\d+(\.\d+)?$/.test(numero)) return toast.error("Ingresa una versión válida, por ejemplo 2.0.");
        if (numero === doc.version) return toast.error("La nueva versión debe ser diferente a la vigente.");
        if (!visibleTodas && autorizadas.length === 0)
            return toast.error("Agrega al menos un área autorizada o marca «Visible para todas las áreas».");
        nuevaVersion(
            doc.id,
            {
                numero,
                fecha,
                autor: sesion?.nombre ?? "Administrador",
                notas: notas.trim() || "Actualización de contenido.",
                archivo: archivo || doc.archivo,
            },
            { visibleTodas, areasAutorizadas: visibleTodas ? [] : autorizadas },
        );
        toast.success(`Versión v${numero} publicada`, { description: doc.nombre });
        onClose();
    };

    return (
        <Dialog open={!!doc} onOpenChange={(o) => !o && onClose()}>
            <DialogContent className="max-h-[85vh] max-w-2xl overflow-y-auto">
                <DialogHeader>
                    <DialogTitle>Publicar nueva versión</DialogTitle>
                    <DialogDescription>
                        La versión anterior se conserva en el historial y no se sobrescribe.
                    </DialogDescription>
                </DialogHeader>

                <div className="space-y-5">
                    <div className="grid gap-4 sm:grid-cols-2">
                        <Dato k="Documento original" v={doc?.nombre ?? "—"} />
                        <Dato k="Versión vigente" v={`v${doc?.version ?? "—"}`} />
                    </div>

                    <div className="grid gap-4 sm:grid-cols-2">
                        <div className="space-y-1.5">
                            <Label>Nueva versión</Label>
                            <Input value={numero} onChange={(e) => setNumero(e.target.value)} placeholder="Ej. 1.1" />
                            <p className="text-xs text-muted-foreground">
                                Sugerida a partir de la versión vigente. Puedes modificarla.
                            </p>
                        </div>
                        <div className="space-y-1.5">
                            <Label>Fecha de actualización</Label>
                            <Input type="date" value={fecha} onChange={(e) => setFecha(e.target.value)} />
                        </div>
                    </div>

                    <div className="space-y-1.5">
                        <Label>Nuevo archivo</Label>
                        <label className="flex cursor-pointer items-center justify-between gap-3 rounded-md border border-dashed border-border bg-secondary/40 px-4 py-3 text-sm hover:border-primary/40">
                            <span className="truncate">{archivo || "Seleccionar archivo (simulado)"}</span>
                            <span className="shrink-0 text-xs text-primary">Examinar</span>
                            <input
                                type="file"
                                className="hidden"
                                onChange={(e) => setArchivo(e.target.files?.[0]?.name ?? "")}
                            />
                        </label>
                    </div>

                    <div className="space-y-1.5">
                        <Label>Descripción de los cambios</Label>
                        <Textarea rows={3} value={notas} onChange={(e) => setNotas(e.target.value)} />
                    </div>

                    <div className="space-y-2">
                        <Label>Áreas autorizadas para visualizar</Label>
                        <AreasAutorizadas
                            idCheckbox="version-visible-todas"
                            areas={areas}
                            seleccionadas={autorizadas}
                            onChange={setAutorizadas}
                            visibleTodas={visibleTodas}
                            onVisibleTodas={setVisibleTodas}
                        />
                    </div>

                    <p className="rounded-md bg-secondary p-3 text-xs text-secondary-foreground">
                        Al publicar esta versión, se convertirá en la versión vigente. La versión anterior continuará almacenada
                        en el historial.
                    </p>
                </div>

                <DialogFooter>
                    <Button variant="outline" onClick={onClose}>
                        Cancelar
                    </Button>
                    <Button onClick={publicar}>Publicar nueva versión</Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}

function Dato({ k, v }: { k: string; v: string }) {
    return (
        <div>
            <p className="text-xs uppercase tracking-wide text-muted-foreground">{k}</p>
            <p className="mt-1 text-sm font-medium">{v}</p>
        </div>
    );
}

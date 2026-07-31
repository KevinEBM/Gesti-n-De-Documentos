import { Plus, X } from "lucide-react";
import { useState } from "react";

import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import type { Area } from "@/lib/data";

/**
 * Selector múltiple de áreas autorizadas para visualizar un documento.
 * Permite agregar áreas una a una, eliminarlas y marcar "visible para todas las áreas".
 */
export function AreasAutorizadas({
                                     areas,
                                     seleccionadas,
                                     onChange,
                                     visibleTodas,
                                     onVisibleTodas,
                                     error,
                                     idCheckbox = "visible-todas",
                                 }: {
    areas: Area[];
    seleccionadas: string[];
    onChange: (ids: string[]) => void;
    visibleTodas: boolean;
    onVisibleTodas: (v: boolean) => void;
    error?: string;
    idCheckbox?: string;
}) {
    const [pendiente, setPendiente] = useState("");
    const disponibles = areas.filter((a) => a.activo && !seleccionadas.includes(a.id));

    const agregar = () => {
        if (!pendiente || seleccionadas.includes(pendiente)) return;
        onChange([...seleccionadas, pendiente]);
        setPendiente("");
    };

    return (
        <div className="space-y-3">
            <div className="flex items-center gap-2">
                <Checkbox id={idCheckbox} checked={visibleTodas} onCheckedChange={(v) => onVisibleTodas(v === true)} />
                <Label htmlFor={idCheckbox} className="text-sm font-normal">
                    Visible para todas las áreas
                </Label>
            </div>

            {!visibleTodas && (
                <>
                    <div className="flex flex-col gap-2 sm:flex-row">
                        <Select value={pendiente} onValueChange={setPendiente} disabled={disponibles.length === 0}>
                            <SelectTrigger className="w-full sm:max-w-xs">
                                <SelectValue placeholder={disponibles.length ? "Seleccionar área…" : "No hay más áreas"} />
                            </SelectTrigger>
                            <SelectContent>
                                {disponibles.map((a) => (
                                    <SelectItem key={a.id} value={a.id}>
                                        {a.nombre}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                        <Button type="button" variant="outline" onClick={agregar} disabled={!pendiente} className="gap-1.5">
                            <Plus className="size-4" /> Agregar otra área
                        </Button>
                    </div>

                    {seleccionadas.length > 0 ? (
                        <ul className="flex flex-wrap gap-2">
                            {seleccionadas.map((id) => (
                                <li
                                    key={id}
                                    className="flex items-center gap-2 rounded-md border border-border bg-secondary px-2.5 py-1.5 text-sm"
                                >
                                    {areas.find((a) => a.id === id)?.nombre ?? id}
                                    <button
                                        type="button"
                                        aria-label={`Quitar ${areas.find((a) => a.id === id)?.nombre ?? id}`}
                                        onClick={() => onChange(seleccionadas.filter((x) => x !== id))}
                                        className="text-muted-foreground hover:text-destructive"
                                    >
                                        <X className="size-3.5" />
                                    </button>
                                </li>
                            ))}
                        </ul>
                    ) : (
                        <p className="text-sm text-muted-foreground">Aún no has agregado áreas autorizadas.</p>
                    )}
                </>
            )}

            {error && <p className="text-xs text-destructive">{error}</p>}
        </div>
    );
}

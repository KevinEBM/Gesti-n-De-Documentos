import { Check, ChevronsUpDown } from "lucide-react";
import { useMemo, useState } from "react";

import { Button } from "@/components/ui/button";
import {
    Command,
    CommandEmpty,
    CommandGroup,
    CommandInput,
    CommandItem,
    CommandList,
} from "@/components/ui/command";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import type { AreaCatalogo } from "@/lib/areas-api";
import { obtenerIconoArea } from "@/lib/iconos-areas";
import { cn } from "@/lib/utils";

const POPOVER_CONTENT_CLASS =
    "w-[var(--radix-popover-trigger-width)] p-0 !bg-white !text-slate-900 border border-slate-200 shadow-2xl z-[99999]";

const COMMAND_CLASS = "!bg-white !text-slate-900";

const COMMAND_ITEM_CLASS =
    "!text-slate-900 data-[selected=true]:!bg-slate-100 data-[selected=true]:!text-slate-900";

function filtrarArea(value: string, search: string): number {
    if (!search.trim()) return 1;
    return value.toLowerCase().includes(search.trim().toLowerCase()) ? 1 : 0;
}

export function etiquetaAreaResponsable(area: Pick<AreaCatalogo, "codigo" | "nombre">): string {
    return `${area.codigo} - ${area.nombre}`;
}

function EtiquetaAreaConIcono({
    area,
}: {
    area: Pick<AreaCatalogo, "codigo" | "nombre">;
}) {
    const { icono: Icono, color } = obtenerIconoArea(area.nombre);

    return (
        <span className="flex min-w-0 items-center gap-2">
            <Icono className={`size-4 shrink-0 ${color}`} />
            <span className="truncate">{etiquetaAreaResponsable(area)}</span>
        </span>
    );
}

export function SelectorAreaResponsable({
    areas,
    value,
    onValueChange,
    placeholder = "Seleccione un área",
    disabled = false,
    id,
}: {
    areas: AreaCatalogo[];
    value: string;
    onValueChange: (areaId: string) => void;
    placeholder?: string;
    disabled?: boolean;
    id?: string;
}) {
    const [abierto, setAbierto] = useState(false);

    const areaSeleccionada = useMemo(
        () => areas.find((area) => area.id === value),
        [areas, value],
    );

    return (
        <Popover open={abierto} onOpenChange={setAbierto} modal>
            <PopoverTrigger asChild>
                <Button
                    id={id}
                    type="button"
                    variant="outline"
                    role="combobox"
                    aria-expanded={abierto}
                    disabled={disabled || areas.length === 0}
                    className={cn(
                        "w-full justify-between font-normal !bg-white !text-slate-900",
                        !areaSeleccionada && "text-muted-foreground",
                    )}
                >
                    <span className="min-w-0 flex-1 overflow-hidden text-left">
                        {areaSeleccionada ? (
                            <EtiquetaAreaConIcono area={areaSeleccionada} />
                        ) : (
                            <span className="truncate">{placeholder}</span>
                        )}
                    </span>
                    <ChevronsUpDown className="ml-2 size-4 shrink-0 opacity-50" />
                </Button>
            </PopoverTrigger>
            <PopoverContent align="start" className={POPOVER_CONTENT_CLASS}>
                <Command className={COMMAND_CLASS} filter={filtrarArea}>
                    <CommandInput placeholder="Buscar área..." className="!text-slate-900" />
                    <CommandList>
                        <CommandEmpty>No se encontraron áreas.</CommandEmpty>
                        <CommandGroup>
                            {areas.map((area) => (
                                <CommandItem
                                    key={area.id}
                                    value={`${area.codigo} ${area.nombre}`}
                                    className={COMMAND_ITEM_CLASS}
                                    onSelect={() => {
                                        onValueChange(area.id);
                                        setAbierto(false);
                                    }}
                                >
                                    <Check
                                        className={cn(
                                            "size-4 shrink-0",
                                            value === area.id ? "opacity-100" : "opacity-0",
                                        )}
                                    />
                                    <EtiquetaAreaConIcono area={area} />
                                </CommandItem>
                            ))}
                        </CommandGroup>
                    </CommandList>
                </Command>
            </PopoverContent>
        </Popover>
    );
}

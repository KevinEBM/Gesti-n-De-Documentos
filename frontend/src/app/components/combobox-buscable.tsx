import { Check, ChevronsUpDown } from "lucide-react";
import { type ReactNode, useMemo, useState } from "react";

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
import { normalizarNombre } from "@/lib/normalizar-nombre";
import { cn } from "@/lib/utils";

const POPOVER_CONTENT_CLASS =
    "w-[var(--radix-popover-trigger-width)] p-0 !bg-white !text-slate-900 border border-slate-200 shadow-md z-[99999]";

const COMMAND_CLASS = "!bg-white !text-slate-900";

const COMMAND_ITEM_CLASS =
    "!text-slate-900 data-[selected=true]:!bg-slate-100 data-[selected=true]:!text-slate-900";

export function filtrarOpcionCombobox(value: string, search: string): number {
    if (!search.trim()) return 1;
    return normalizarNombre(value).includes(normalizarNombre(search)) ? 1 : 0;
}

export function ComboboxBuscable<T>({
    items,
    value,
    onValueChange,
    getItemValue,
    getSearchValue,
    renderItem,
    renderValue,
    placeholder = "Seleccionar…",
    placeholderBusqueda = "Buscar…",
    emptyText = "No se encontraron resultados.",
    disabled = false,
    id,
    className,
    invalid = false,
    describedBy,
    opcionVacia,
}: {
    items: T[];
    value: string;
    onValueChange: (value: string) => void;
    getItemValue: (item: T) => string;
    getSearchValue: (item: T) => string;
    renderItem: (item: T) => ReactNode;
    renderValue?: (item: T) => ReactNode;
    placeholder?: string;
    placeholderBusqueda?: string;
    emptyText?: string;
    disabled?: boolean;
    id?: string;
    className?: string;
    invalid?: boolean;
    describedBy?: string;
    opcionVacia?: { value: string; label: string };
}) {
    const [abierto, setAbierto] = useState(false);

    const seleccionado = useMemo(
        () => items.find((item) => getItemValue(item) === value),
        [items, value, getItemValue],
    );
    const esOpcionVacia = Boolean(opcionVacia && value === opcionVacia.value);

    return (
        <Popover open={abierto} onOpenChange={setAbierto} modal>
            <PopoverTrigger asChild>
                <Button
                    id={id}
                    type="button"
                    variant="outline"
                    role="combobox"
                    aria-expanded={abierto}
                    disabled={disabled}
                    aria-invalid={invalid}
                    aria-describedby={describedBy}
                    className={cn(
                        "w-full min-w-0 justify-between font-normal !bg-white !text-slate-900",
                        !seleccionado && !esOpcionVacia && "text-muted-foreground",
                        invalid && "border-destructive",
                        className,
                    )}
                >
                    <span className="min-w-0 flex-1 overflow-hidden text-left">
                        {seleccionado ? (
                            (renderValue ?? renderItem)(seleccionado)
                        ) : esOpcionVacia ? (
                            <span className="truncate">{opcionVacia?.label}</span>
                        ) : (
                            <span className="truncate">{placeholder}</span>
                        )}
                    </span>
                    <ChevronsUpDown className="ml-2 size-4 shrink-0 opacity-50" />
                </Button>
            </PopoverTrigger>
            <PopoverContent align="start" className={POPOVER_CONTENT_CLASS}>
                <Command className={COMMAND_CLASS} filter={filtrarOpcionCombobox}>
                    <CommandInput
                        placeholder={placeholderBusqueda}
                        className="!text-slate-900"
                    />
                    <CommandList>
                        <CommandEmpty>{emptyText}</CommandEmpty>
                        <CommandGroup>
                            {opcionVacia ? (
                                <CommandItem
                                    value={opcionVacia.label}
                                    className={COMMAND_ITEM_CLASS}
                                    onSelect={() => {
                                        onValueChange(opcionVacia.value);
                                        setAbierto(false);
                                    }}
                                >
                                    <Check
                                        className={cn(
                                            "size-4 shrink-0",
                                            esOpcionVacia ? "opacity-100" : "opacity-0",
                                        )}
                                    />
                                    <span className="truncate">{opcionVacia.label}</span>
                                </CommandItem>
                            ) : null}
                            {items.map((item) => {
                                const itemValue = getItemValue(item);
                                return (
                                    <CommandItem
                                        key={itemValue}
                                        value={getSearchValue(item)}
                                        className={COMMAND_ITEM_CLASS}
                                        onSelect={() => {
                                            onValueChange(itemValue);
                                            setAbierto(false);
                                        }}
                                    >
                                        <Check
                                            className={cn(
                                                "size-4 shrink-0",
                                                value === itemValue ? "opacity-100" : "opacity-0",
                                            )}
                                        />
                                        {renderItem(item)}
                                    </CommandItem>
                                );
                            })}
                        </CommandGroup>
                    </CommandList>
                </Command>
            </PopoverContent>
        </Popover>
    );
}

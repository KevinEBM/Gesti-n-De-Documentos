import type { TipoDocumentoCatalogo } from "@/lib/tipos-documento-api";
import { obtenerIconoFormato } from "@/lib/iconos-formatos";

import { ComboboxBuscable } from "@/components/combobox-buscable";

export function etiquetaTipoDocumentoCatalogo(
    tipo: Pick<TipoDocumentoCatalogo, "codigo" | "nombre">,
): string {
    const codigo = tipo.codigo?.trim();
    return codigo ? `${codigo} - ${tipo.nombre}` : tipo.nombre;
}

function EtiquetaTipoConIcono({
    tipo,
}: {
    tipo: Pick<TipoDocumentoCatalogo, "codigo" | "nombre">;
}) {
    const { icono: Icono, color } = obtenerIconoFormato(tipo.nombre);

    return (
        <span className="flex min-w-0 items-center gap-2">
            <Icono className={`size-4 shrink-0 ${color}`} />
            <span className="truncate">{etiquetaTipoDocumentoCatalogo(tipo)}</span>
        </span>
    );
}

export function SelectorTipoDocumento({
    tipos,
    value,
    onValueChange,
    placeholder = "Seleccione un tipo",
    placeholderBusqueda = "Buscar tipo...",
    disabled = false,
    id,
    className,
    invalid = false,
    describedBy,
    opcionTodos,
}: {
    tipos: TipoDocumentoCatalogo[];
    value: string;
    onValueChange: (tipoId: string) => void;
    placeholder?: string;
    placeholderBusqueda?: string;
    disabled?: boolean;
    id?: string;
    className?: string;
    invalid?: boolean;
    describedBy?: string;
    opcionTodos?: { value: string; label: string };
}) {
    return (
        <ComboboxBuscable
            items={tipos}
            value={value}
            onValueChange={onValueChange}
            getItemValue={(item) => item.id}
            getSearchValue={(item) => `${item.codigo} ${item.nombre}`}
            renderItem={(item) => <EtiquetaTipoConIcono tipo={item} />}
            placeholder={placeholder}
            placeholderBusqueda={placeholderBusqueda}
            emptyText="No se encontraron tipos."
            disabled={disabled}
            id={id}
            className={className}
            invalid={invalid}
            describedBy={describedBy}
            opcionVacia={opcionTodos}
        />
    );
}

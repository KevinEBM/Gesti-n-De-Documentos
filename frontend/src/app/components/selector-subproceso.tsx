import type { SubprogramaCatalogo } from "@/lib/subprogramas-api";
import { obtenerIconoSubProceso } from "@/lib/iconos-subprocesos";

import { ComboboxBuscable } from "@/components/combobox-buscable";

export function etiquetaSubprocesoCatalogo(
    subprograma: Pick<SubprogramaCatalogo, "codigo" | "nombre">,
): string {
    return `${subprograma.codigo} - ${subprograma.nombre}`;
}

function EtiquetaSubprocesoConIcono({
    subprograma,
}: {
    subprograma: Pick<SubprogramaCatalogo, "codigo" | "nombre">;
}) {
    const { icono: Icono, color } = obtenerIconoSubProceso(subprograma.nombre);

    return (
        <span className="flex min-w-0 items-center gap-2">
            <Icono className={`size-4 shrink-0 ${color}`} />
            <span className="truncate">{etiquetaSubprocesoCatalogo(subprograma)}</span>
        </span>
    );
}

export function SelectorSubproceso({
    subprogramas,
    value,
    onValueChange,
    placeholder = "Seleccione un subproceso",
    placeholderBusqueda = "Buscar subproceso...",
    disabled = false,
    id,
    className,
    invalid = false,
    describedBy,
    opcionTodos,
}: {
    subprogramas: SubprogramaCatalogo[];
    value: string;
    onValueChange: (subprogramaId: string) => void;
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
            items={subprogramas}
            value={value}
            onValueChange={onValueChange}
            getItemValue={(item) => item.id}
            getSearchValue={(item) => `${item.codigo} ${item.nombre}`}
            renderItem={(item) => <EtiquetaSubprocesoConIcono subprograma={item} />}
            placeholder={placeholder}
            placeholderBusqueda={placeholderBusqueda}
            emptyText="No se encontraron subprocesos."
            disabled={disabled}
            id={id}
            className={className}
            invalid={invalid}
            describedBy={describedBy}
            opcionVacia={opcionTodos}
        />
    );
}

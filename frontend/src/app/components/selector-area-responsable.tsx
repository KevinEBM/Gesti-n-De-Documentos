import type { AreaCatalogo } from "@/lib/areas-api";
import { obtenerIconoArea } from "@/lib/iconos-areas";

import { ComboboxBuscable } from "@/components/combobox-buscable";

export type FormatoEtiquetaArea = "codigo-nombre" | "nombre";

export function etiquetaAreaCatalogo(
    area: Pick<AreaCatalogo, "codigo" | "nombre">,
    formato: FormatoEtiquetaArea = "codigo-nombre",
): string {
    return formato === "codigo-nombre" ? `${area.codigo} - ${area.nombre}` : area.nombre;
}

function valorBusquedaArea(
    area: Pick<AreaCatalogo, "codigo" | "nombre">,
    formato: FormatoEtiquetaArea,
): string {
    return formato === "codigo-nombre" ? `${area.codigo} ${area.nombre}` : area.nombre;
}

/** @deprecated Usar etiquetaAreaCatalogo */
export function etiquetaAreaResponsable(area: Pick<AreaCatalogo, "codigo" | "nombre">): string {
    return etiquetaAreaCatalogo(area, "codigo-nombre");
}

function EtiquetaAreaConIcono({
    area,
    formato,
}: {
    area: Pick<AreaCatalogo, "codigo" | "nombre">;
    formato: FormatoEtiquetaArea;
}) {
    const { icono: Icono, color } = obtenerIconoArea(area.nombre);

    return (
        <span className="flex min-w-0 items-center gap-2">
            <Icono className={`size-4 shrink-0 ${color}`} />
            <span className="truncate">{etiquetaAreaCatalogo(area, formato)}</span>
        </span>
    );
}

export function SelectorAreaResponsable({
    areas,
    value,
    onValueChange,
    placeholder = "Seleccione un área",
    placeholderBusqueda = "Buscar área...",
    formato = "codigo-nombre",
    disabled = false,
    id,
    className,
    invalid = false,
    describedBy,
    opcionTodos,
}: {
    areas: AreaCatalogo[];
    value: string;
    onValueChange: (areaId: string) => void;
    placeholder?: string;
    placeholderBusqueda?: string;
    formato?: FormatoEtiquetaArea;
    disabled?: boolean;
    id?: string;
    className?: string;
    invalid?: boolean;
    describedBy?: string;
    opcionTodos?: { value: string; label: string };
}) {
    return (
        <ComboboxBuscable
            items={areas}
            value={value}
            onValueChange={onValueChange}
            getItemValue={(area) => area.id}
            getSearchValue={(area) => valorBusquedaArea(area, formato)}
            renderItem={(area) => <EtiquetaAreaConIcono area={area} formato={formato} />}
            placeholder={placeholder}
            placeholderBusqueda={placeholderBusqueda}
            emptyText="No se encontraron áreas."
            disabled={disabled || (areas.length === 0 && !opcionTodos)}
            id={id}
            className={className}
            invalid={invalid}
            describedBy={describedBy}
            opcionVacia={opcionTodos}
        />
    );
}

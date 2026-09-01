import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
} from "@/components/ui/select";
import { obtenerIconoRol } from "@/lib/iconos-roles";

const SELECT_CONTENT_CLASS =
    "!bg-white !text-slate-900 border border-slate-200 shadow-md z-[99999]";

const SELECT_TRIGGER_CLASS = "w-full !bg-white !text-slate-900";

const SELECT_ITEM_CLASS =
    "!text-slate-900 focus:!bg-slate-100 focus:!text-slate-900 data-[highlighted]:!bg-slate-100 data-[highlighted]:!text-slate-900";

function EtiquetaRolConIcono({ etiqueta }: { etiqueta: string }) {
    const { icono: Icono, color } = obtenerIconoRol(etiqueta);

    return (
        <div className="flex min-w-0 items-center gap-2">
            <Icono className={`h-4 w-4 shrink-0 ${color}`} />
            <span className="truncate">{etiqueta}</span>
        </div>
    );
}

export function SelectorRol({
    roles,
    value,
    onValueChange,
    disabled = false,
    placeholder = "Seleccionar…",
}: {
    roles: { id: string; etiqueta: string }[];
    value: string;
    onValueChange: (rolId: string) => void;
    disabled?: boolean;
    placeholder?: string;
}) {
    const rolSeleccionado = roles.find((rol) => rol.id === value);

    return (
        <Select value={value} onValueChange={onValueChange} disabled={disabled}>
            <SelectTrigger
                className={`${SELECT_TRIGGER_CLASS} [&>div]:min-w-0 [&>div]:flex-1`}
            >
                {rolSeleccionado ? (
                    <EtiquetaRolConIcono etiqueta={rolSeleccionado.etiqueta} />
                ) : (
                    <span className="truncate text-muted-foreground">{placeholder}</span>
                )}
            </SelectTrigger>
            <SelectContent className={SELECT_CONTENT_CLASS}>
                {roles.map((rol) => (
                    <SelectItem
                        key={rol.id}
                        value={rol.id}
                        className={SELECT_ITEM_CLASS}
                    >
                        <EtiquetaRolConIcono etiqueta={rol.etiqueta} />
                    </SelectItem>
                ))}
            </SelectContent>
        </Select>
    );
}

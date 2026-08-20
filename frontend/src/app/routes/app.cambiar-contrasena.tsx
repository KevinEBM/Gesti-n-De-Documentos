import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { Eye, EyeOff, Lock } from "lucide-react";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";

import { AppShell } from "@/components/AppShell";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/lib/api";
import { cambiarContrasena } from "@/lib/auth-api";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/app/cambiar-contrasena")({
    head: () => ({
        meta: [
            { title: "Cambiar contraseña — Intranet documental" },
            {
                name: "description",
                content: "Actualiza la contraseña de tu cuenta de acceso a la intranet documental.",
            },
        ],
    }),
    component: CambiarContrasena,
});

type ErroresFormulario = {
    contrasenaActual?: string;
    nuevaContrasena?: string;
    confirmacionContrasena?: string;
};

function validarContrasenaNueva(valor: string): string | undefined {
    if (!valor) return "La nueva contraseña es obligatoria.";
    if (valor.length < 8 || valor.length > 100) {
        return "La contraseña debe tener entre 8 y 100 caracteres.";
    }
    return undefined;
}

function CambiarContrasena() {
    const navigate = useNavigate();
    const [contrasenaActual, setContrasenaActual] = useState("");
    const [nuevaContrasena, setNuevaContrasena] = useState("");
    const [confirmacionContrasena, setConfirmacionContrasena] = useState("");
    const [errores, setErrores] = useState<ErroresFormulario>({});
    const [enviando, setEnviando] = useState(false);

    const limpiarFormulario = () => {
        setContrasenaActual("");
        setNuevaContrasena("");
        setConfirmacionContrasena("");
        setErrores({});
    };

    const validarFormulario = (): ErroresFormulario => {
        const nuevos: ErroresFormulario = {};

        if (!contrasenaActual) {
            nuevos.contrasenaActual = "La contraseña actual es obligatoria.";
        }

        const errorNueva = validarContrasenaNueva(nuevaContrasena);
        if (errorNueva) {
            nuevos.nuevaContrasena = errorNueva;
        }

        if (!confirmacionContrasena) {
            nuevos.confirmacionContrasena = "La confirmación es obligatoria.";
        } else if (nuevaContrasena !== confirmacionContrasena) {
            nuevos.confirmacionContrasena = "Las contraseñas no coinciden.";
        }

        return nuevos;
    };

    const enviar = async (event: FormEvent) => {
        event.preventDefault();

        const nuevosErrores = validarFormulario();
        if (Object.keys(nuevosErrores).length > 0) {
            setErrores(nuevosErrores);
            return;
        }

        setErrores({});
        setEnviando(true);
        try {
            await cambiarContrasena({
                contrasenaActual,
                nuevaContrasena,
                confirmacionContrasena,
            });
            toast.success("Contraseña actualizada correctamente.");
            limpiarFormulario();
        } catch (err) {
            if (err instanceof ApiError) {
                const mensaje = err.message.toLowerCase();
                if (mensaje.includes("contraseña actual")) {
                    setErrores({ contrasenaActual: err.message });
                    return;
                }
                if (mensaje.includes("no coinciden")) {
                    setErrores({ confirmacionContrasena: err.message });
                    return;
                }
                if (err.errores) {
                    setErrores({
                        contrasenaActual: err.errores.contrasenaActual,
                        nuevaContrasena: err.errores.nuevaContrasena,
                        confirmacionContrasena: err.errores.confirmacionContrasena,
                    });
                    return;
                }
                toast.error(err.message);
                return;
            }
            toast.error("No fue posible cambiar la contraseña.");
        } finally {
            setEnviando(false);
        }
    };

    const cancelar = () => {
        limpiarFormulario();
        navigate({ to: "/app/documentos" });
    };

    return (
        <AppShell
            titulo="Cambiar contraseña"
            descripcion="Actualiza la contraseña de tu cuenta. Necesitas conocer la contraseña actual."
        >
            <Card className="max-w-lg">
                <CardContent className="py-6">
                    <form className="space-y-5" onSubmit={(event) => void enviar(event)} noValidate>
                        <CampoContrasena
                            id="contrasena-actual"
                            label="Contraseña actual"
                            value={contrasenaActual}
                            onChange={setContrasenaActual}
                            autoComplete="current-password"
                            error={errores.contrasenaActual}
                            disabled={enviando}
                        />
                        <CampoContrasena
                            id="nueva-contrasena"
                            label="Nueva contraseña"
                            value={nuevaContrasena}
                            onChange={setNuevaContrasena}
                            autoComplete="new-password"
                            error={errores.nuevaContrasena}
                            disabled={enviando}
                        />
                        <CampoContrasena
                            id="confirmacion-contrasena"
                            label="Confirmar nueva contraseña"
                            value={confirmacionContrasena}
                            onChange={setConfirmacionContrasena}
                            autoComplete="new-password"
                            error={errores.confirmacionContrasena}
                            disabled={enviando}
                        />

                        <div className="flex flex-wrap justify-end gap-2 pt-2">
                            <Button
                                type="button"
                                variant="outline"
                                className="bg-white text-black"
                                onClick={cancelar}
                                disabled={enviando}
                            >
                                Cancelar
                            </Button>
                            <Button type="submit" disabled={enviando}>
                                {enviando ? "Actualizando..." : "Cambiar contraseña"}
                            </Button>
                        </div>
                    </form>
                </CardContent>
            </Card>
        </AppShell>
    );
}

function CampoContrasena({
    id,
    label,
    value,
    onChange,
    autoComplete,
    error,
    disabled,
}: {
    id: string;
    label: string;
    value: string;
    onChange: (valor: string) => void;
    autoComplete: "current-password" | "new-password";
    error?: string;
    disabled?: boolean;
}) {
    const [visible, setVisible] = useState(false);

    return (
        <div className="space-y-1.5">
            <Label htmlFor={id}>{label}</Label>
            <div className="relative">
                <Lock className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                    id={id}
                    type={visible ? "text" : "password"}
                    autoComplete={autoComplete}
                    placeholder="••••••••"
                    className={cn("bg-white pl-9 pr-10", error && "border-destructive")}
                    value={value}
                    onChange={(event) => onChange(event.target.value)}
                    aria-invalid={!!error}
                    disabled={disabled}
                />
                <button
                    type="button"
                    className="absolute right-2 top-1/2 -translate-y-1/2 rounded p-1 text-muted-foreground hover:text-foreground"
                    onClick={() => setVisible((prev) => !prev)}
                    aria-label={visible ? "Ocultar contraseña" : "Mostrar contraseña"}
                    tabIndex={-1}
                    disabled={disabled}
                >
                    {visible ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
                </button>
            </div>
            {error ? <p className="text-xs text-destructive">{error}</p> : null}
        </div>
    );
}

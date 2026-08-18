import logoImg from "../resources/portada-logo.png";
import fondoImg from "../resources/fondo-inicio-sesion.png";

import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { Lock, Mail, ShieldCheck } from "lucide-react";
import { useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { useIntranet } from "@/lib/store";

export const Route = createFileRoute("/")({
    head: () => ({
        meta: [
            { title: "Iniciar sesión — Intranet Documental" },
            {
                name: "description",
                content:
                    "Acceso privado para trabajadores autorizados de Grupo Andina a la intranet de documentos internos.",
            },
            { property: "og:title", content: "Iniciar sesión — Intranet documental " },
            {
                property: "og:description",
                content: "Acceso privado para trabajadores autorizados de Grupo Andina a la intranet de documentos internos.",
            },
        ],
    }),
    component: Login,
});

const demos = [
    { correo: "admin@empresa.com", password: "admin123", rol: "Administrador", detalle: "Gestión total de la intranet" },
    {
        correo: "administrativo@empresa.com",
        password: "admin123",
        rol: "Administrativo",
        detalle: "Consulta de documentos vigentes de su área",
    },
    {correo: "jefearea@empresa.com",password:"admin123",rol: "Administrativo", detalle: "Consulta de documentos vigentes e historicos de su área"}
];

function Login() {
    const { iniciarSesion, sesion } = useIntranet();
    const navigate = useNavigate();
    const [correo, setCorreo] = useState("");
    const [password, setPassword] = useState("");
    const [errores, setErrores] = useState<{ correo?: string; password?: string }>({});
    const [error, setError] = useState<string | null>(null);
    const [aviso, setAviso] = useState<string | null>(null);

    useEffect(() => {
        if (sesion) {
            navigate({ to: sesion.rol === "administrador" ? "/app/panel-admin" : "/app/inicio" });
        }
    }, [sesion, navigate]);

    const enviar = (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        setAviso(null);
        const nuevos: { correo?: string; password?: string } = {};
        if (!correo.trim()) nuevos.correo = "El correo institucional es obligatorio.";
        else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo.trim()))
            nuevos.correo = "Ingresa un correo válido, por ejemplo nombre@empresa.com.";
        if (!password) nuevos.password = "La contraseña es obligatoria.";
        else if (password.length < 6) nuevos.password = "La contraseña debe tener al menos 6 caracteres.";
        setErrores(nuevos);
        if (Object.keys(nuevos).length > 0) return;

        const res = iniciarSesion(correo, password);
        if (!res.ok) setError(res.error ?? "No fue posible iniciar sesión.");
    };

    const usarDemo = (d: (typeof demos)[number]) => {
        setCorreo(d.correo);
        setPassword(d.password);
        setErrores({});
        setError(null);
    };

    return (
        /*
          COLOR PRINCIPAL DE TODO LO DEMÁS: #f2eee2
        */
        <div className="grid min-h-screen lg:grid-cols-[0.6fr_1fr]" style={{ backgroundColor: "#f2eee2" }}>

            {/* SECCIÓN IZQUIERDA*/}
            <div
                className="relative hidden flex-col justify-between p-8 text-sidebar-foreground lg:flex transition-colors duration-200 border-r-10 border-[#b99a79]"
                style={{
                    backgroundColor: "#e5e8d3",
                    color: "#112639"
                }}
            >
                <div className="flex items-center gap-3">

                    <div className="flex h-60 w-auto items-center justify-center overflow-hidden rounded-md p-1">
                        <img
                            src={logoImg}
                            alt="Logo Plantar"
                            className="size-full object-contain"
                        />
                    </div>
                </div>

                <div className="max-w-md space-y-5">
                    <h2 className="text-4xl font-semibold leading-tight">
                        Un solo lugar para la documentación interna de la empresa.
                    </h2>
                    <p className="text-sm leading-relaxed opacity-90">
                        Consulta manuales, políticas, protocolos y procedimientos vigentes de su área, con control de versiones.
                    </p>
                    <ul className="space-y-2 text-sm opacity-90">
                        <li>· Documentos organizados por área, categoría y tipo</li>
                        <li>· Historial de versiones siempre disponible</li>
                        <li>· Acceso restringido a personal autorizado</li>
                    </ul>
                </div>

                {/*<p className="flex items-center gap-2 text-xs opacity-70">
                    <ShieldCheck className="size-4" />
                    Prototipo de demostración con datos ficticios.
                </p> */}
            </div>

            {/* SECCIÓN DERECHA: Formulario de Login */}
            <div className="relative flex items-center justify-center px-5 py-12 overflow-hidden">

                {/* IMAGEN DE FONDO INFERIOR DERECHA */}
                <div className="absolute bottom-100% right-0 pointer-events-none hidden lg:block max-h-[100%] w-auto z-0">
                    <img
                        src={fondoImg}
                        alt="Ilustración decorativa inferior"
                        className="h-full w-auto object-contain object-bottom"
                    />
                </div>

                {/* CONTENEDOR OPACO DEL FORMULARIO */}
                <div className="w-full max-w-sm relative z-10 bg-white/95 backdrop-blur-sm p-8 rounded-2xl shadow-xl border border-border/50">
                    {/* SECCIÓN MÓVIL */}
                    <div className="mb-8 flex items-center gap-3 lg:hidden">
                        <div className="flex h-10 w-auto items-center justify-center overflow-hidden rounded-md p-1">
                            <img
                                src={logoImg}
                                alt="Logo Grupo Andina"
                                className="size-full object-contain"
                            />
                        </div>
                    </div>

                    <h1 className="text-2xl font-semibold">Iniciar sesión</h1>
                    <p className="mt-1.5 text-sm text-muted-foreground">
                        Usa tu correo institucional. Las cuentas son creadas por el administrador.
                    </p>

                    <form onSubmit={enviar} noValidate className="mt-7 space-y-4">
                        <div className="space-y-1.5">
                            <Label htmlFor="correo">Correo institucional</Label>
                            <div className="relative">
                                <Mail className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                                <Input
                                    id="correo"
                                    type="email"
                                    autoComplete="email"
                                    placeholder="nombre@empresa.com"
                                    className="pl-9 bg-white"
                                    value={correo}
                                    onChange={(e) => setCorreo(e.target.value)}
                                    aria-invalid={!!errores.correo}
                                />
                            </div>
                            {errores.correo && <p className="text-xs text-destructive">{errores.correo}</p>}
                        </div>

                        <div className="space-y-1.5">
                            <Label htmlFor="password">Contraseña</Label>
                            <div className="relative">
                                <Lock className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                                <Input
                                    id="password"
                                    type="password"
                                    autoComplete="current-password"
                                    placeholder="••••••••"
                                    className="pl-9 bg-white"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    aria-invalid={!!errores.password}
                                />
                            </div>
                            {errores.password && <p className="text-xs text-destructive">{errores.password}</p>}
                        </div>

                        {error && (
                            <Alert variant="destructive">
                                <AlertDescription>{error}</AlertDescription>
                            </Alert>
                        )}
                        {aviso && (
                            <Alert>
                                <AlertDescription>{aviso}</AlertDescription>
                            </Alert>
                        )}

                        {/* Botón de Iniciar Sesión */}
                        <Button
                            type="submit"
                            className="w-full text-white font-medium transition-opacity hover:opacity-90"
                            style={{ backgroundColor: "#289248" }}
                        >
                            Iniciar sesión
                        </Button>
                    </form>

                    <div className="mt-8 rounded-lg border border-border bg-white p-4 shadow-sm">
                        <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                            Usuarios de demostración
                        </p>
                        <div className="mt-3 space-y-2">
                            {demos.map((d) => (
                                <button
                                    key={d.correo}
                                    type="button"
                                    onClick={() => usarDemo(d)}
                                    className="flex w-full items-center justify-between rounded-md border border-border px-3 py-2 text-left transition-colors hover:border-primary/40 hover:bg-muted/50"
                                >
                                    <span className="min-w-0">
                                        <span className="block truncate text-sm font-medium">{d.correo}</span>
                                        <span className="block truncate text-xs text-muted-foreground">
                                          {d.rol} · {d.detalle}
                                        </span>
                                    </span>
                                    <span className="ml-3 shrink-0 text-xs font-medium" style={{ color: "#289248" }}>Usar</span>
                                </button>
                            ))}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
import { createContext, useContext, useMemo, useState, type ReactNode } from "react";
import { apiFetch, ApiError, type LoginResponseDto } from "./api";
import {
    clearSession,
    getSession,
    loginResponseToUsuario,
    saveSession,
    type AuthSession,
} from "./auth-storage";
import { type Usuario } from "./data";

export interface Permisos {
    gestionarUsuarios: boolean;
    gestionarParametros: boolean;
    publicarDocumentos: boolean;
    actualizarDocumentos: boolean;
    administrarEstados: boolean;
    verHistorialGlobal: boolean;
}

interface IntranetContextValue {
    sesion: Usuario | null;
    permisos: Permisos;
    iniciarSesion: (correo: string, password: string) => Promise<{ ok: boolean; error?: string }>;
    cerrarSesion: () => void;
}

const IntranetContext = createContext<IntranetContextValue | null>(null);

const sinPermisos: Permisos = {
    gestionarUsuarios: false,
    gestionarParametros: false,
    publicarDocumentos: false,
    actualizarDocumentos: false,
    administrarEstados: false,
    verHistorialGlobal: false,
};

export function permisosDe(usuario: Usuario | null): Permisos {
    if (!usuario) return sinPermisos;
    if (usuario.rol === "administrador") {
        return {
            gestionarUsuarios: true,
            gestionarParametros: true,
            publicarDocumentos: true,
            actualizarDocumentos: true,
            administrarEstados: true,
            verHistorialGlobal: true,
        };
    }
    if (usuario.rol === "jefe_area") {
        return {
            gestionarUsuarios: false,
            gestionarParametros: false,
            publicarDocumentos: false,
            actualizarDocumentos: false,
            administrarEstados: false,
            verHistorialGlobal: true,
        };
    }
    return sinPermisos;
}

export function IntranetProvider({ children }: { children: ReactNode }) {
    const [sesion, setSesion] = useState<Usuario | null>(
        () => getSession()?.usuario ?? null,
    );

    const value = useMemo<IntranetContextValue>(() => {
        const permisos = permisosDe(sesion);

        return {
            sesion,
            permisos,
            iniciarSesion: async (correo, password) => {
                try {
                    const datos = await apiFetch<LoginResponseDto>("/api/auth/login", {
                        method: "POST",
                        body: JSON.stringify({ correo, contrasena: password }),
                    });
                    const usuario = loginResponseToUsuario(datos);
                    const session: AuthSession = {
                        token: datos.token,
                        tipo: datos.tipo,
                        usuario,
                    };
                    saveSession(session);
                    setSesion(usuario);
                    return { ok: true };
                } catch (err) {
                    if (err instanceof ApiError) {
                        return { ok: false, error: err.message };
                    }
                    return { ok: false, error: "No fue posible iniciar sesión" };
                }
            },
            cerrarSesion: () => {
                clearSession();
                setSesion(null);
            },
        };
    }, [sesion]);

    return <IntranetContext.Provider value={value}>{children}</IntranetContext.Provider>;
}

export function useIntranet() {
    const ctx = useContext(IntranetContext);
    if (!ctx) throw new Error("useIntranet debe usarse dentro de IntranetProvider");
    return ctx;
}

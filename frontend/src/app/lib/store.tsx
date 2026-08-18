import { createContext, useContext, useMemo, useState, type ReactNode } from "react";
import {
    actividadInicial,
    areasIniciales,
    subProcesosIniciales,
    documentosIniciales,
    //notificacionesIniciales,
    tiposIniciales,
    usuariosIniciales,
    type Actividad,
    type Area,
    type SubProceso,
    type Documento,
    type Estado,
    type Notificacion,
    type TipoDocumento,
    type Usuario,
    type Version,
} from "./data";

type Parametro = Area | SubProceso | TipoDocumento;

export interface Permisos {
    gestionarUsuarios: boolean;
    gestionarParametros: boolean;
    publicarDocumentos: boolean;
    actualizarDocumentos: boolean;
    administrarEstados: boolean;
    verHistorialGlobal: boolean;
    verUsarEmojiIcon: boolean;
}

interface IntranetContextValue {
    sesion: Usuario | null;
    permisos: Permisos;
    areas: Area[];
    sub_proceso: SubProceso[];
    tipos: TipoDocumento[];
    usuarios: Usuario[];
    documentos: Documento[];
    //notificaciones: Notificacion[];
    actividad: Actividad[];
    iniciarSesion: (correo: string, password: string) => { ok: boolean; error?: string };
    cerrarSesion: () => void;
    nombreArea: (id: string) => string;
    nombreSub_Proceso: (id: string) => string;
    nombreTipo: (id: string) => string;
    /** Documentos que el usuario en sesión puede consultar. */
    documentosVisibles: Documento[];
    /** Notificaciones correspondientes a documentos visibles para el usuario. */
//    notificacionesVisibles: Notificacion[];
    /** ¿Puede el usuario consultar el historial de versiones de este documento? */
    puedeVerHistorial: (doc: Documento) => boolean;
    /*marcarLeida: (id: string) => void;
    marcarTodasLeidas: () => void;*/
    verUsarEmojiIcon: boolean;
    crearDocumento: (doc: Omit<Documento, "id" | "consultas" | "versiones">) => void;
    actualizarDocumento: (id: string, cambios: Partial<Documento>) => void;
    nuevaVersion: (
        id: string,
        version: Version,
        opciones?: { visibleTodas?: boolean; areasAutorizadas?: string[] },
    ) => void;
    guardarUsuario: (usuario: Omit<Usuario, "id"> & { id?: string }) => void;
    alternarUsuario: (id: string) => void;
    guardarParametro: (
        tipo: "areas" | "sub_proceso" | "tipos",
        valor: Omit<Parametro, "id"> & { id?: string },
    ) => void;
    alternarParametro: (tipo: "areas" | "sub_proceso" | "tipos", id: string) => void;
}

const IntranetContext = createContext<IntranetContextValue | null>(null);

const CREDENCIALES: Record<string, string> = {
    "admin@empresa.com": "admin123",
    "administrativo@empresa.com": "admin123",
    "jefearea@empresa.com":"admin123",
};

const nuevoId = (prefijo: string) => `${prefijo}${Math.random().toString(36).slice(2, 8)}`;

const sinPermisos: Permisos = {
    gestionarUsuarios: false,
    gestionarParametros: false,
    publicarDocumentos: false,
    actualizarDocumentos: false,
    administrarEstados: false,
    verHistorialGlobal: false,
    verUsarEmojiIcon: false,
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
            verUsarEmojiIcon: true,
        };
    } else if (usuario.rol === "jefe_area") {
        return {
            gestionarUsuarios: false,
            gestionarParametros: false,
            publicarDocumentos: false,
            actualizarDocumentos: false,
            administrarEstados: false,
            verHistorialGlobal: true,
            verUsarEmojiIcon: true,
        };
    }
    // Jefe de área y administrativo solo consultan.
    return sinPermisos;
}

/** Sugiere la siguiente versión menor a partir de la vigente (1.0 → 1.1). */
export function sugerirVersion(actual: string) {
    const [mayor, menor] = actual.split(".");
    const m = Number.parseInt(mayor ?? "1", 10);
    const n = Number.parseInt(menor ?? "0", 10);
    if (Number.isNaN(m)) return "1.0";
    return `${m}.${Number.isNaN(n) ? 1 : n + 1}`;
}

export function IntranetProvider({ children }: { children: ReactNode }) {
    const [sesion, setSesion] = useState<Usuario | null>(null);
    const [areas, setAreas] = useState<Area[]>(areasIniciales);
    const [sub_proceso, setSub_Procesos] = useState<SubProceso[]>(subProcesosIniciales);
    const [tipos, setTipos] = useState<TipoDocumento[]>(tiposIniciales);
    const [usuarios, setUsuarios] = useState<Usuario[]>(usuariosIniciales);
    const [documentos, setDocumentos] = useState<Documento[]>(documentosIniciales);
//    const [notificaciones, setNotificaciones] = useState<Notificacion[]>(notificacionesIniciales);
    const [actividad, setActividad] = useState<Actividad[]>(actividadInicial);

    const value = useMemo<IntranetContextValue>(() => {
        const permisos = permisosDe(sesion);

        const registrar = (accion: string, detalle: string) =>
            setActividad((prev) => [
                {
                    id: nuevoId("ac"),
                    usuario: sesion?.nombre ?? "Sistema",
                    accion,
                    detalle,
                    fecha: new Date().toISOString().slice(0, 16).replace("T", " "),
                },
                ...prev,
            ]);

        /* const notificar = (tipo: Notificacion["tipo"], titulo: string, mensaje: string, documentoId: string) =>
             setNotificaciones((prev) => [
                 {
                     id: nuevoId("n"),
                     tipo,
                     titulo,
                     mensaje,
                     fecha: new Date().toISOString().slice(0, 16).replace("T", " "),
                     leida: false,
                     documentoId,
                 },
                 ...prev,
             ]);
 */
        const autorizado = (doc: Documento, areaId: string) =>
            doc.visibleTodas || doc.areaId === areaId || doc.areasAutorizadas.includes(areaId);

        const documentosVisibles =
            !sesion
                ? []
                : sesion.rol === "administrador"
                    ? documentos
                    : documentos.filter((d) => d.estado === "publicado" && autorizado(d, sesion.areaId));

        /*const notificacionesVisibles = notificaciones.filter((n) => {
            if (!sesion) return false;
            if (sesion.rol === "administrador") return true;
            const doc = documentos.find((d) => d.id === n.documentoId);
            return !!doc && doc.estado === "publicado" && autorizado(doc, sesion.areaId);
        });*/

        const puedeVerHistorial = (doc: Documento): boolean => {
            if (!sesion) return false;

            if (sesion.rol === "administrador") return true;

            // El jefe de área puede ver el historial de los documentos de su área
            if (sesion.rol === "jefe_area") {
                return autorizado(doc, sesion.areaId);
            }

            // El administrativo NO puede
            if (sesion.rol === "administrativo") {
                return false;
            }

            return false;
        };

        const setter = { areas: setAreas, sub_proceso: setSub_Procesos, tipos: setTipos } as const;

        return {
            sesion,
            permisos,
            areas,
            sub_proceso,
            tipos,
            usuarios,
            documentos,
            // notificaciones,
            actividad,
            documentosVisibles,
            //  notificacionesVisibles,
            puedeVerHistorial,
            verUsarEmojiIcon: permisos.verUsarEmojiIcon,
            iniciarSesion: (correo, password) => {
                const usuario = usuarios.find((u) => u.correo.toLowerCase() === correo.trim().toLowerCase());
                if (!usuario) return { ok: false, error: "No existe una cuenta con ese correo institucional." };
                if (!usuario.activo) return { ok: false, error: "La cuenta está inactiva. Contacta al administrador." };
                if (CREDENCIALES[usuario.correo] && CREDENCIALES[usuario.correo] !== password) {
                    return { ok: false, error: "La contraseña no es correcta." };
                }
                setSesion(usuario);
                return { ok: true };
            },
            cerrarSesion: () => setSesion(null),
            nombreArea: (id) => areas.find((a) => a.id === id)?.nombre ?? "—",
            nombreSub_Proceso: (id) => sub_proceso.find((c) => c.id === id)?.nombre ?? "—",
            nombreTipo: (id) => tipos.find((t) => t.id === id)?.nombre ?? "—",
            marcarLeida: (id) =>
            {/*}   setNotificaciones((prev) => prev.map((n) => (n.id === id ? { ...n, leida: true } : n))),
            marcarTodasLeidas: () => setNotificaciones((prev) => prev.map((n) => ({ ...n, leida: true }))),*/},
            crearDocumento: (doc) => {
                const id = nuevoId("d");
                setDocumentos((prev) => [
                    {
                        ...doc,
                        id,
                        consultas: 0,
                        versiones: [
                            {
                                numero: doc.version,
                                fecha: doc.fechaPublicacion,
                                autor: doc.publicadoPor,
                                notas: "Versión inicial.",
                                archivo: doc.archivo,
                            },
                        ],
                    },
                    ...prev,
                ]);/*
                registrar(doc.estado === "publicado" ? "Publicó documento" : "Guardó borrador", doc.nombre);
                if (doc.estado === "publicado") {
                    notificar("nuevo", "Nuevo documento publicado", `Se publicó un nuevo documento: ${doc.nombre}.`, id);
                }*/
            },
            actualizarDocumento: (id, cambios) => {
                setDocumentos((prev) => prev.map((d) => (d.id === id ? { ...d, ...cambios } : d)));
                const doc = documentos.find((d) => d.id === id);
                registrar("Actualizó documento", doc?.nombre ?? id);
            },
            nuevaVersion: (id, version, opciones) => {
                setDocumentos((prev) =>
                    prev.map((d) =>
                        d.id === id
                            ? {
                                ...d,
                                version: version.numero,
                                archivo: version.archivo ?? d.archivo,
                                fechaPublicacion: version.fecha,
                                estado: "publicado" as Estado,
                                visibleTodas: opciones?.visibleTodas ?? d.visibleTodas,
                                areasAutorizadas: opciones?.areasAutorizadas ?? d.areasAutorizadas,
                                // Nunca se sobrescriben las versiones anteriores.
                                versiones: [version, ...d.versiones],
                            }
                            : d,
                    ),
                );
                const doc = documentos.find((d) => d.id === id);
                registrar("Publicó nueva versión", `${doc?.nombre ?? id} v${version.numero}`);
                /* notificar(
                     "version",
                     "Documento actualizado",
                     `El documento ${doc?.nombre ?? "seleccionado"} fue actualizado a la versión ${version.numero}.`,
                     id,
                 );*/
            },
            guardarUsuario: (usuario) => {
                if (usuario.id) {
                    setUsuarios((prev) => prev.map((u) => (u.id === usuario.id ? ({ ...u, ...usuario } as Usuario) : u)));
                    registrar("Editó usuario", usuario.correo);
                } else {
                    setUsuarios((prev) => [...prev, { ...usuario, id: nuevoId("u") } as Usuario]);
                    registrar("Creó usuario", usuario.correo);
                }
            },
            alternarUsuario: (id) => {
                setUsuarios((prev) => prev.map((u) => (u.id === id ? { ...u, activo: !u.activo } : u)));
                const u = usuarios.find((x) => x.id === id);
                registrar("Cambió estado de usuario", `${u?.correo ?? id} → ${u?.activo ? "inactivo" : "activo"}`);
            },
            guardarParametro: (tipo, valor) => {
                const update = setter[tipo] as React.Dispatch<React.SetStateAction<Parametro[]>>;
                if (valor.id) {
                    update((prev) => prev.map((p) => (p.id === valor.id ? ({ ...p, ...valor } as Parametro) : p)));
                    registrar("Editó parámetro", valor.nombre);
                } else {
                    update((prev) => [...prev, { ...valor, id: nuevoId(tipo[0]) } as Parametro]);
                    registrar("Creó parámetro", valor.nombre);
                }
            },
            alternarParametro: (tipo, id) => {
                const update = setter[tipo] as React.Dispatch<React.SetStateAction<Parametro[]>>;
                update((prev) => prev.map((p) => (p.id === id ? { ...p, activo: !p.activo } : p)));
                registrar("Cambió estado de parámetro", id);
            },
        };
    }, [sesion, areas, sub_proceso, tipos, usuarios, documentos/*, notificaciones*/, actividad]);

    return <IntranetContext.Provider value={value}>{children}</IntranetContext.Provider>;
}

export function useIntranet() {
    const ctx = useContext(IntranetContext);
    if (!ctx) throw new Error("useIntranet debe usarse dentro de IntranetProvider");
    return ctx;
}
import React, { useState, useMemo } from 'react';
// Importación Global: Traemos TODO el contenido de las librerías
import * as LucideIcons from "lucide-react";
import * as TablerIcons from "@tabler/icons-react";

// Definimos la estructura de un icono dinámico
interface DynamicIcon {
    name: string;
    component: React.ElementType;
}

// Definimos los posibles valores que devolverá el evento onSelect
export type IconSelection =
    | { type: 'emoji'; value: string; component?: never }
    | { type: 'lucide' | 'tabler'; value: string; component: React.ElementType };

// Props que recibe el componente
export interface IconPickerProps {
    onSelect: (selection: IconSelection) => void;
    onClose: () => void;
}

// ------------------------------------------------------------------
// 2. EXTRACCIÓN DINÁMICA
// ------------------------------------------------------------------

// Extraemos los componentes asegurando a TypeScript (con 'as any')
// que podemos iterar sobre las exportaciones del módulo.
const allLucideIcons: DynamicIcon[] = Object.keys(LucideIcons)
    .filter(key => /^[A-Z]/.test(key))
    .map(key => ({
        name: key,
        component: (LucideIcons as any)[key] as React.ElementType
    }));

const allTablerIcons: DynamicIcon[] = Object.keys(TablerIcons)
    .filter(key => /^[A-Z]/.test(key))
    .map(key => ({
        name: key.replace(/^Icon/, ''),
        component: (TablerIcons as any)[key] as React.ElementType
    }));

const emojis: string[] = [
    '😂', '😊', '🤣', '❤️', '😍', '🙄',
    '👌', '😘', '💕', '😁', '👍', '🙌',
    '✨', '🔥', '🎉', '💡', '✅', '📦'
];

// ------------------------------------------------------------------
// 3. COMPONENTE PRINCIPAL
// ------------------------------------------------------------------

export default function IconPicker({ onSelect, onClose }: IconPickerProps) {
    const [activeTab, setActiveTab] = useState<'emoji' | 'lucide' | 'tabler'>('emoji');
    const [searchTerm, setSearchTerm] = useState<string>('');

    // Optimización con useMemo para no re-calcular en cada renderizado
    const displayedLucide = useMemo(() => {
        let filtered = allLucideIcons;
        if (searchTerm) {
            filtered = allLucideIcons.filter(icon =>
                icon.name.toLowerCase().includes(searchTerm.toLowerCase())
            );
        }
        return filtered.slice(0, 100);
    }, [searchTerm]);

    const displayedTabler = useMemo(() => {
        let filtered = allTablerIcons;
        if (searchTerm) {
            filtered = allTablerIcons.filter(icon =>
                icon.name.toLowerCase().includes(searchTerm.toLowerCase())
            );
        }
        return filtered.slice(0, 100);
    }, [searchTerm]);

    return (
        <div className="flex flex-col w-[350px] h-[450px] bg-white border border-gray-200 rounded-lg shadow-xl overflow-hidden font-sans">

            <div className="flex justify-between items-center p-3 border-b border-gray-100">
                <span className="text-sm font-semibold text-gray-700">Iconos y más</span>
                <button
                    onClick={onClose}
                    className="p-1 hover:bg-gray-100 rounded-md text-gray-500 transition-colors"
                >
                    ✕
                </button>
            </div>

            <div className="flex gap-2 p-2 border-b border-gray-100 px-4">
                <button
                    onClick={() => setActiveTab('emoji')}
                    className={`p-2 rounded-md transition-colors ${activeTab === 'emoji' ? 'bg-gray-100 border-b-2 border-black' : 'hover:bg-gray-50'}`}
                    title="Emojis"
                >
                    😀
                </button>
                <button
                    onClick={() => setActiveTab('lucide')}
                    className={`p-2 rounded-md transition-colors ${activeTab === 'lucide' ? 'bg-gray-100 border-b-2 border-black' : 'hover:bg-gray-50'}`}
                    title="Lucide Icons"
                >
                    <LucideIcons.ShieldCheck size={20} className="text-gray-700" />
                </button>
                <button
                    onClick={() => setActiveTab('tabler')}
                    className={`p-2 rounded-md transition-colors ${activeTab === 'tabler' ? 'bg-gray-100 border-b-2 border-black' : 'hover:bg-gray-50'}`}
                    title="Tabler Icons"
                >
                    <TablerIcons.IconBuildingWarehouse size={20} className="text-gray-700" />
                </button>
            </div>

            <div className="p-3">
                <div className="relative">
                    <span className="absolute left-3 top-2.5 text-gray-400 text-sm">🔍</span>
                    <input
                        type="text"
                        placeholder={`Buscar en ${activeTab === 'lucide' ? allLucideIcons.length : activeTab === 'tabler' ? allTablerIcons.length : 'emojis'} iconos...`}
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="w-full pl-9 pr-3 py-2 bg-gray-50 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-blue-500"
                    />
                </div>
            </div>

            <div className="flex-1 overflow-y-auto p-4 custom-scrollbar">

                {activeTab === 'emoji' && (
                    <div>
                        <div className="text-xs text-gray-500 mb-3 font-medium uppercase tracking-wider">
                            Emojis
                        </div>
                        <div className="grid grid-cols-6 gap-2">
                            {emojis.map((emoji, index) => (
                                <button
                                    key={index}
                                    onClick={() => onSelect({ type: 'emoji', value: emoji })}
                                    className="text-2xl h-10 w-10 flex items-center justify-center hover:bg-gray-100 rounded-md transition-colors"
                                >
                                    {emoji}
                                </button>
                            ))}
                        </div>
                    </div>
                )}

                {activeTab === 'lucide' && (
                    <div>
                        <div className="flex justify-between items-end mb-3">
                            <span className="text-xs text-gray-500 font-medium uppercase tracking-wider">
                                Lucide Icons
                            </span>
                            <span className="text-[10px] text-gray-400">
                                Mostrando {displayedLucide.length} de {allLucideIcons.length}
                            </span>
                        </div>
                        <div className="grid grid-cols-5 gap-3">
                            {displayedLucide.map((iconObj, index) => {
                                const IconComponent = iconObj.component;
                                return (
                                    <button
                                        key={index}
                                        onClick={() => onSelect({ type: 'lucide', value: iconObj.name, component: IconComponent })}
                                        className="h-12 w-12 flex items-center justify-center text-gray-700 hover:bg-gray-100 hover:text-black rounded-md transition-colors"
                                        title={iconObj.name}
                                    >
                                        <IconComponent size={24} strokeWidth={1.5} />
                                    </button>
                                );
                            })}
                        </div>
                        {displayedLucide.length === 0 && (
                            <p className="text-sm text-gray-400 text-center mt-4">No se encontraron iconos</p>
                        )}
                    </div>
                )}

                {activeTab === 'tabler' && (
                    <div>
                        <div className="flex justify-between items-end mb-3">
                            <span className="text-xs text-gray-500 font-medium uppercase tracking-wider">
                                Tabler Icons
                            </span>
                            <span className="text-[10px] text-gray-400">
                                Mostrando {displayedTabler.length} de {allTablerIcons.length}
                            </span>
                        </div>
                        <div className="grid grid-cols-5 gap-3">
                            {displayedTabler.map((iconObj, index) => {
                                const IconComponent = iconObj.component;
                                return (
                                    <button
                                        key={index}
                                        onClick={() => onSelect({ type: 'tabler', value: iconObj.name, component: IconComponent })}
                                        className="h-12 w-12 flex items-center justify-center text-gray-700 hover:bg-gray-100 hover:text-black rounded-md transition-colors"
                                        title={iconObj.name}
                                    >
                                        <IconComponent size={26} stroke={1.5} />
                                    </button>
                                );
                            })}
                        </div>
                        {displayedTabler.length === 0 && (
                            <p className="text-sm text-gray-400 text-center mt-4">No se encontraron iconos</p>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}
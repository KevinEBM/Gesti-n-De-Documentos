import { useCallback } from "react";
import { useDropzone } from "react-dropzone";
import { File, UploadCloud, X } from "lucide-react";
import { Button } from "@/components/ui/button";

interface DropzoneAreaProps {
    onFileSelect: (file: File | null) => void;
    selectedFile: File | null;
    accept?: Record<string, string[]>;
    maxSize?: number; // En bytes
}

export function DropzoneArea({ onFileSelect, selectedFile, accept, maxSize = 10485760 }: DropzoneAreaProps) {
    const onDrop = useCallback(
        (acceptedFiles: File[]) => {
            if (acceptedFiles && acceptedFiles.length > 0) {
                onFileSelect(acceptedFiles[0]);
            }
        },
        [onFileSelect]
    );

    const { getRootProps, getInputProps, isDragActive, isDragReject } = useDropzone({
        onDrop,
        accept,
        maxSize,
        maxFiles: 1,
    });

    const formatBytes = (bytes: number, decimals = 2) => {
        if (!+bytes) return "0 Bytes";
        const k = 1024;
        const dm = decimals < 0 ? 0 : decimals;
        const sizes = ["Bytes", "KB", "MB", "GB", "TB"];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`;
    };

    const handleRemoveFile = (e: React.MouseEvent) => {
        e.stopPropagation(); // Evita que se abra el selector de archivos al hacer clic en la 'X'
        onFileSelect(null);
    };

    return (
        <div className="w-full">
            {!selectedFile ? (
                <div
                    {...getRootProps()}
                    className={`flex flex-col items-center justify-center w-full h-48 border-2 border-dashed rounded-lg cursor-pointer transition-colors ${
                        isDragActive
                            ? "border-primary bg-primary/5"
                            : isDragReject
                                ? "border-destructive bg-destructive/5"
                                : "border-muted-foreground/25 hover:bg-muted/50"
                    }`}
                >
                    <input {...getInputProps()} />
                    <UploadCloud
                        className={`w-10 h-10 mb-3 ${
                            isDragActive ? "text-primary" : "text-muted-foreground"
                        }`}
                    />
                    <div className="text-center px-4">
                        <p className="mb-2 text-sm font-semibold">
                            {isDragActive
                                ? "Suelta el archivo aquí..."
                                : "Haz clic para subir o arrastra y suelta"}
                        </p>
                        <p className="text-xs text-muted-foreground">
                            PDF, DOCX, XLSX (Máx. {formatBytes(maxSize)})
                        </p>
                    </div>
                </div>
            ) : (
                <div className="flex items-center justify-between p-4 border rounded-lg bg-muted/30">
                    <div className="flex items-center space-x-3 overflow-hidden">
                        <div className="p-2 bg-primary/10 rounded-lg shrink-0">
                            <File className="w-6 h-6 text-primary" />
                        </div>
                        <div className="overflow-hidden">
                            <p className="text-sm font-medium truncate">{selectedFile.name}</p>
                            <p className="text-xs text-muted-foreground">
                                {formatBytes(selectedFile.size)}
                            </p>
                        </div>
                    </div>
                    <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        onClick={handleRemoveFile}
                        className="text-muted-foreground hover:text-destructive shrink-0"
                    >
                        <X className="w-5 h-5" />
                    </Button>
                </div>
            )}
        </div>
    );
}
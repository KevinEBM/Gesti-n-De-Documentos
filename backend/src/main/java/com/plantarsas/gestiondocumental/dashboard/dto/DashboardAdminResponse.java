package com.plantarsas.gestiondocumental.dashboard.dto;

/**
 * Métricas generales que ve el administrador: cuántos documentos
 * están publicados, cuántos usuarios están activos y cuántas áreas
 * hay registradas.
 */
public record DashboardAdminResponse(
        long documentosPublicados,
        long usuariosActivos,
        long areasRegistradas
) {
}

package com.plantarsas.gestiondocumental.dashboard.service;

import com.plantarsas.gestiondocumental.dashboard.dto.ActividadDocumentalResponse;
import com.plantarsas.gestiondocumental.dashboard.dto.DashboardAdminResponse;

import java.util.List;

/**
 * Contrato de las operaciones del panel administrativo: obtener las
 * métricas generales y la actividad reciente del sistema.
 */
public interface DashboardService {

    DashboardAdminResponse obtenerDashboard();

    List<ActividadDocumentalResponse> obtenerActividadReciente();
}

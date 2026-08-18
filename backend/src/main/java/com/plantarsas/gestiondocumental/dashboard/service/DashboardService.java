package com.plantarsas.gestiondocumental.dashboard.service;

import com.plantarsas.gestiondocumental.dashboard.dto.ActividadDocumentalResponse;
import com.plantarsas.gestiondocumental.dashboard.dto.DashboardAdminResponse;

import java.util.List;

public interface DashboardService {

    DashboardAdminResponse obtenerDashboard();

    List<ActividadDocumentalResponse> obtenerActividadReciente();
}

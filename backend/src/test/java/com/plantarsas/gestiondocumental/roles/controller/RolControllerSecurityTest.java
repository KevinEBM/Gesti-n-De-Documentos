package com.plantarsas.gestiondocumental.roles.controller;

import com.plantarsas.gestiondocumental.config.SecurityTestConfiguration;
import com.plantarsas.gestiondocumental.exception.GlobalExceptionHandler;
import com.plantarsas.gestiondocumental.roles.dto.RolResponse;
import com.plantarsas.gestiondocumental.roles.service.RolService;
import com.plantarsas.gestiondocumental.security.AuthenticatedUser;
import com.plantarsas.gestiondocumental.security.JwtAccessDeniedHandler;
import com.plantarsas.gestiondocumental.security.JwtAuthenticationEntryPoint;
import com.plantarsas.gestiondocumental.security.JwtAuthenticationFilter;
import com.plantarsas.gestiondocumental.security.JwtService;
import com.plantarsas.gestiondocumental.shared.enums.RolEnum;
import com.plantarsas.gestiondocumental.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RolControllerSecurityTest.ConfiguracionSeguridadRolesTest.class)
@WebAppConfiguration
class RolControllerSecurityTest {

    private static final String URL_LISTAR = "/api/roles";
    private static final String URL_OBTENER = "/api/roles/1";

    @MockitoBean
    private RolService rolService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void limpiarMocks() {
        reset(rolService, jwtService, usuarioRepository);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor administradorAutenticado() {
        AuthenticatedUser administrador = new AuthenticatedUser(1L, "admin@plantarsas.com", RolEnum.ADMINISTRADOR);
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                administrador, null, List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
        );
        return SecurityMockMvcRequestPostProcessors.authentication(token);
    }

    private RolResponse rolDePrueba() {
        return new RolResponse(1L, RolEnum.ADMINISTRADOR, "Rol administrador", true);
    }

    @Test
    void listar_sinAutenticacion_debeResponder401() throws Exception {
        mockMvc.perform(get(URL_LISTAR))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(rolService);
    }

    @Test
    void listar_conAdministrador_debeResponder200() throws Exception {
        when(rolService.listar()).thenReturn(List.of(rolDePrueba()));

        mockMvc.perform(get(URL_LISTAR).with(administradorAutenticado()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "JEFE_AREA")
    void listar_conJefeArea_debeResponder403() throws Exception {
        mockMvc.perform(get(URL_LISTAR))
                .andExpect(status().isForbidden());

        verifyNoInteractions(rolService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void listar_conAdministrativo_debeResponder403() throws Exception {
        mockMvc.perform(get(URL_LISTAR))
                .andExpect(status().isForbidden());

        verifyNoInteractions(rolService);
    }

    @Test
    void obtenerPorId_sinAutenticacion_debeResponder401() throws Exception {
        mockMvc.perform(get(URL_OBTENER))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(rolService);
    }

    @Test
    void obtenerPorId_conAdministrador_debeResponder200() throws Exception {
        when(rolService.obtenerPorId(1L)).thenReturn(rolDePrueba());

        mockMvc.perform(get(URL_OBTENER).with(administradorAutenticado()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "JEFE_AREA")
    void obtenerPorId_conJefeArea_debeResponder403() throws Exception {
        mockMvc.perform(get(URL_OBTENER))
                .andExpect(status().isForbidden());

        verifyNoInteractions(rolService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void obtenerPorId_conAdministrativo_debeResponder403() throws Exception {
        mockMvc.perform(get(URL_OBTENER))
                .andExpect(status().isForbidden());

        verifyNoInteractions(rolService);
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({
            SecurityTestConfiguration.class,
            GlobalExceptionHandler.class,
            RolController.class
    })
    static class ConfiguracionSeguridadRolesTest {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}

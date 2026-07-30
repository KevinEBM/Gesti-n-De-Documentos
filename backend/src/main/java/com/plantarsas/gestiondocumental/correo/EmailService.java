package com.plantarsas.gestiondocumental.correo;

import java.util.Collection;

public interface EmailService {

    void enviar(String destinatario, String asunto, String cuerpo);

    void enviar(Collection<String> destinatarios, String asunto, String cuerpo);
}


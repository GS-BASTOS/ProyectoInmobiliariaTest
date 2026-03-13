package com.inmobiliaria.app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.contact.email}")
    private String contactEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void enviarConsultaInmueble(String propertyCode,
                                       String nombre,
                                       String telefono,
                                       String email,
                                       String mensaje) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(contactEmail);
            mail.setSubject("Nueva consulta · Inmueble " + propertyCode);
            mail.setText(
                "═══════════════════════════════════\n" +
                "  NUEVA CONSULTA — SOLVIA STORE BILBAO\n" +
                "═══════════════════════════════════\n\n" +
                "Inmueble:   " + propertyCode + "\n\n" +
                "Nombre:     " + nombre + "\n" +
                "Teléfono:   " + telefono + "\n" +
                "Email:      " + (email != null && !email.isBlank() ? email : "No indicado") + "\n\n" +
                "Mensaje:\n"   + (mensaje != null && !mensaje.isBlank() ? mensaje : "Sin mensaje") + "\n\n" +
                "───────────────────────────────────\n" +
                "Enviado desde solviastorebilbao.com"
            );
            mailSender.send(mail);
        } catch (Exception e) {
            System.err.println("[EmailService] Error enviando consulta inmueble: " + e.getMessage());
        }
    }

    @Async
    public void enviarConsultaGeneral(String nombre,
                                      String telefono,
                                      String email,
                                      String motivo,
                                      String mensaje) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(contactEmail);
            mail.setSubject("Nueva consulta general – Solvia Store Bilbao");
            mail.setText(
                "═══════════════════════════════════\n" +
                "  CONSULTA GENERAL — SOLVIA STORE BILBAO\n" +
                "═══════════════════════════════════\n\n" +
                "Nombre:    " + nombre + "\n" +
                "Teléfono:  " + telefono + "\n" +
                "Email:     " + (email != null && !email.isBlank() ? email : "No indicado") + "\n" +
                "Motivo:    " + (motivo != null && !motivo.isBlank() ? motivo : "No indicado") + "\n\n" +
                "Mensaje:\n" + (mensaje != null && !mensaje.isBlank() ? mensaje : "Sin mensaje") + "\n\n" +
                "───────────────────────────────────\n" +
                "Enviado desde solviastorebilbao.com"
            );
            mailSender.send(mail);
        } catch (Exception e) {
            System.err.println("[EmailService] Error enviando consulta general: " + e.getMessage());
        }
    }
}

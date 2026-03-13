package com.inmobiliaria.app.web;

import com.inmobiliaria.app.domain.Property;
import com.inmobiliaria.app.domain.PropertyMedia;
import com.inmobiliaria.app.repo.PropertyMediaRepository;
import com.inmobiliaria.app.repo.PropertyRepository;
import com.inmobiliaria.app.service.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
public class PublicController {

    private final PropertyRepository      propertyRepository;
    private final PropertyMediaRepository mediaRepository;
    private final EmailService            emailService;

    public PublicController(PropertyRepository propertyRepository,
                            PropertyMediaRepository mediaRepository,
                            EmailService emailService) {
        this.propertyRepository = propertyRepository;
        this.mediaRepository    = mediaRepository;
        this.emailService       = emailService;
    }

    // ── GET / → Landing page ─────────────────────────────
    @GetMapping("/")
    public String home(Model model) {
        List<Property> destacados = propertyRepository
                .findByPublicadoTrueAndSoldFalseOrderByIdDesc()
                .stream().limit(3).toList();

        asignarPortadas(destacados);
        model.addAttribute("destacados", destacados);
        return "public/home";
    }

    // ── GET /catalogo ─────────────────────────────────────
    @GetMapping("/catalogo")
    public String catalogo(@RequestParam(required = false) String tipo,
                           Model model) {

        List<Property> propiedades = (tipo != null && !tipo.isBlank())
                ? propertyRepository
                    .findByPublicadoTrueAndSoldFalseAndPropertyTypeOrderByIdDesc(tipo)
                : propertyRepository
                    .findByPublicadoTrueAndSoldFalseOrderByIdDesc();

        asignarPortadas(propiedades);

        List<String> tipos = propertyRepository.findTiposPublicados();

        model.addAttribute("propiedades", propiedades);
        model.addAttribute("tipos",       tipos);
        model.addAttribute("tipoActivo",  tipo);
        return "public/catalogo";
    }

    // ── GET /catalogo/{id} ────────────────────────────────
    @GetMapping("/catalogo/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!property.isPublicado() || property.isSold()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        List<PropertyMedia> media = mediaRepository.findByPropertyIdOrderByIdAsc(id);

        PropertyMedia firstImage = media.stream()
                .filter(m -> "IMAGE".equals(m.getMediaType()))
                .findFirst()
                .orElse(null);

        model.addAttribute("property",   property);
        model.addAttribute("mediaList",  media);
        model.addAttribute("firstImage", firstImage);
        return "public/catalogo_detalle";
    }

    // ── POST /catalogo/{id}/contacto ──────────────────────
    @PostMapping("/catalogo/{id}/contacto")
    public String contacto(@PathVariable Long id,
                           @RequestParam String nombre,
                           @RequestParam String telefono,
                           @RequestParam(required = false) String email,
                           @RequestParam(required = false) String mensaje) {

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!property.isPublicado() || property.isSold()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        emailService.enviarConsultaInmueble(
            property.getPropertyCode(),
            nombre, telefono, email, mensaje
        );
        return "redirect:/catalogo/" + id + "?enviado";
    }

    // ── POST /contacto (formulario general home) ──────────
    @PostMapping("/contacto")
    public String contactoGeneral(
            @RequestParam String nombre,
            @RequestParam String telefono,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String motivo,
            @RequestParam(required = false) String mensaje) {

        emailService.enviarConsultaGeneral(nombre, telefono, email, motivo, mensaje);
        return "redirect:/?enviado#contacto";
    }

    // ── Helper ────────────────────────────────────────────
    private void asignarPortadas(List<Property> propiedades) {
        propiedades.forEach(p ->
            mediaRepository.findByPropertyIdOrderByIdAsc(p.getId())
                .stream()
                .filter(m -> "IMAGE".equals(m.getMediaType()))
                .findFirst()
                .ifPresent(m -> p.setPortadaUrl(m.getCloudinaryUrl()))
        );
    }
}

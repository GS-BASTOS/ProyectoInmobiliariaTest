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
import java.util.stream.Collectors;

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
    public String catalogo(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Integer precioMin,
            @RequestParam(required = false) Integer precioMax,
            @RequestParam(required = false) String municipio,
            @RequestParam(required = false) String provincia,
            @RequestParam(required = false) String estado,   // "disponible" | "prevendido"
            @RequestParam(required = false) String orden,    // "precio_asc" | "precio_desc" | "reciente"
            Model model) {

        // 1. Carga base: solo publicados y no vendidos definitivamente
        List<Property> propiedades = propertyRepository
                .findByPublicadoTrueAndSoldFalseOrderByIdDesc();

        // 2. Filtros en memoria (evita N queries extra)
        propiedades = propiedades.stream()

            // Tipo de inmueble
            .filter(p -> tipo == null || tipo.isBlank()
                    || tipo.equalsIgnoreCase(p.getPropertyType()))

            // Precio mínimo
            .filter(p -> precioMin == null
                    || (p.getPrecio() != null && p.getPrecio() >= precioMin))

            // Precio máximo
            .filter(p -> precioMax == null
                    || (p.getPrecio() != null && p.getPrecio() <= precioMax))

            // Municipio / ciudad (contains, case-insensitive)
            .filter(p -> municipio == null || municipio.isBlank()
                    || (p.getMunicipality() != null
                        && p.getMunicipality().toLowerCase()
                           .contains(municipio.toLowerCase().trim())))

            // Provincia
            .filter(p -> provincia == null || provincia.isBlank()
                    || (p.getProvince() != null
                        && p.getProvince().toLowerCase()
                           .contains(provincia.toLowerCase().trim())))

            // Estado: prevendido o disponible
            .filter(p -> {
                if ("prevendido".equalsIgnoreCase(estado)) return p.isPreVendido();
                if ("disponible".equalsIgnoreCase(estado)) return !p.isPreVendido();
                return true; // sin filtro = todos
            })

            .collect(Collectors.toList());

        // 3. Ordenación
        if ("precio_asc".equals(orden)) {
            propiedades.sort((a, b) -> {
                if (a.getPrecio() == null) return 1;
                if (b.getPrecio() == null) return -1;
                return a.getPrecio().compareTo(b.getPrecio());
            });
        } else if ("precio_desc".equals(orden)) {
            propiedades.sort((a, b) -> {
                if (a.getPrecio() == null) return 1;
                if (b.getPrecio() == null) return -1;
                return b.getPrecio().compareTo(a.getPrecio());
            });
        }
        // "reciente" ya viene ordenado por id desc del repositorio

        // 4. Portadas
        asignarPortadas(propiedades);

        // 5. Listas para los selects del filtro
        List<String> tipos      = propertyRepository.findTiposPublicados();
        List<String> municipios = propiedades.stream()
                .map(Property::getMunicipality)
                .filter(m -> m != null && !m.isBlank())
                .distinct().sorted().collect(Collectors.toList());
        List<String> provincias = propiedades.stream()
                .map(Property::getProvince)
                .filter(p -> p != null && !p.isBlank())
                .distinct().sorted().collect(Collectors.toList());

        // Precio máximo disponible para el slider
        int maxPrecioDisponible = propertyRepository
                .findByPublicadoTrueAndSoldFalseOrderByIdDesc()
                .stream()
                .filter(p -> p.getPrecio() != null)
                .mapToInt(Property::getPrecio)
                .max().orElse(1_000_000);

        model.addAttribute("propiedades",          propiedades);
        model.addAttribute("tipos",                tipos);
        model.addAttribute("municipios",           municipios);
        model.addAttribute("provincias",           provincias);
        model.addAttribute("maxPrecioDisponible",  maxPrecioDisponible);

        // Devolver los filtros activos para repintar el formulario
        model.addAttribute("tipoActivo",    tipo);
        model.addAttribute("precioMin",     precioMin);
        model.addAttribute("precioMax",     precioMax);
        model.addAttribute("municipioActivo", municipio);
        model.addAttribute("provinciaActiva", provincia);
        model.addAttribute("estadoActivo",  estado);
        model.addAttribute("ordenActivo",   orden);

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
                .findFirst().orElse(null);

        PropertyMedia firstVideo = media.stream()
                .filter(m -> "VIDEO".equals(m.getMediaType()))
                .findFirst().orElse(null);

        model.addAttribute("property",   property);
        model.addAttribute("mediaList",  media);
        model.addAttribute("firstImage", firstImage);
        model.addAttribute("firstVideo", firstVideo);
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
            property.getPropertyCode(), nombre, telefono, email, mensaje);
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
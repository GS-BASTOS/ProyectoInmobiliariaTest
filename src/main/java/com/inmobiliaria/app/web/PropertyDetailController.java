package com.inmobiliaria.app.web;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.inmobiliaria.app.domain.Property;
import com.inmobiliaria.app.domain.PropertyMedia;
import com.inmobiliaria.app.repo.ClientPropertyInteractionRepository;
import com.inmobiliaria.app.repo.PropertyMediaRepository;
import com.inmobiliaria.app.repo.PropertyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
public class PropertyDetailController {

    private final PropertyRepository propertyRepository;
    private final PropertyMediaRepository mediaRepository;
    private final ClientPropertyInteractionRepository interactionRepository;
    private final Cloudinary cloudinary;

    public PropertyDetailController(PropertyRepository propertyRepository,
                                    PropertyMediaRepository mediaRepository,
                                    ClientPropertyInteractionRepository interactionRepository,
                                    Cloudinary cloudinary) {
        this.propertyRepository    = propertyRepository;
        this.mediaRepository       = mediaRepository;
        this.interactionRepository = interactionRepository;
        this.cloudinary            = cloudinary;
    }

    // ── GET /inmuebles/{id} ──────────────────────────────────
    @GetMapping("/inmuebles/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<PropertyMedia> media = mediaRepository.findByPropertyIdOrderByIdAsc(id);
        long interestedCount = interactionRepository.countByPropertyId(id);

        model.addAttribute("property",        property);
        model.addAttribute("mediaList",       media);
        model.addAttribute("interestedCount", interestedCount);
        return "property_detail";
    }

    // ── POST /inmuebles/{id}/actualizar ──────────────────────
    @PostMapping("/inmuebles/{id}/actualizar")
    public String update(@PathVariable Long id,
                         @RequestParam(required = false) String province,
                         @RequestParam(required = false) String address,
                         @RequestParam(required = false) String municipality,
                         @RequestParam(required = false) String propertyType,
                         @RequestParam(required = false) String description,
                         @RequestParam(defaultValue = "false") boolean occupied,
                         @RequestParam(defaultValue = "false") boolean hasAlarm,
                         @RequestParam(required = false) String alarmCode,
                         @RequestParam(required = false) String notes) {

        Property p = propertyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        p.setProvince(t(province));
        p.setAddress(t(address));
        p.setMunicipality(t(municipality));
        p.setPropertyType(t(propertyType));
        p.setDescription(t(description));
        p.setOccupied(occupied);
        p.setHasAlarm(hasAlarm);
        p.setAlarmCode(hasAlarm ? t(alarmCode) : "");
        p.setNotes(t(notes));
        propertyRepository.save(p);

        return "redirect:/inmuebles/" + id;
    }

    // ── POST /inmuebles/{id}/media ───────────────────────────
    @PostMapping("/inmuebles/{id}/media")
    public String uploadMedia(@PathVariable Long id,
                              @RequestParam("files") List<MultipartFile> files) throws IOException {

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String contentType  = file.getContentType() != null ? file.getContentType() : "";
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";

            // ── Determinar resource_type para Cloudinary ──
            String resourceType;
            if (contentType.startsWith("video/")) {
                resourceType = "video";
            } else if (contentType.equals("application/pdf")
                    || contentType.startsWith("application/msword")
                    || contentType.startsWith("application/vnd")) {
                resourceType = "raw";
            } else {
                resourceType = "image";
            }

            // ── Determinar mediaType interno ──
            String mediaType;
            if (resourceType.equals("video"))               mediaType = "VIDEO";
            else if (contentType.equals("application/pdf")) mediaType = "PDF";
            else if (resourceType.equals("raw"))            mediaType = "DOCUMENT";
            else                                            mediaType = "IMAGE";

            // ── Parámetros de subida ──
            Map uploadParams;
            if (resourceType.equals("raw")) {
                String extension = "";
                int dotIndex = originalName.lastIndexOf('.');
                if (dotIndex > 0) {
                    extension = originalName.substring(dotIndex); // ej: ".pdf"
                }
                uploadParams = ObjectUtils.asMap(
                    "resource_type", "raw",
                    "public_id",     "inmobiliaria/" + id + "/" + System.currentTimeMillis() + extension,
                    "use_filename",  false,
                    "access_mode",   "public"
                );
            } else {
                uploadParams = ObjectUtils.asMap(
                    "resource_type", resourceType,
                    "folder",        "inmobiliaria/" + id
                );
            }

            // ── Subir a Cloudinary ──
            Map result = cloudinary.uploader().upload(file.getBytes(), uploadParams);

            PropertyMedia media = new PropertyMedia();
            media.setProperty(property);
            media.setOriginalName(originalName);
            media.setMediaType(mediaType);
            media.setCloudinaryUrl((String) result.get("secure_url"));
            media.setCloudinaryId((String) result.get("public_id"));
            mediaRepository.save(media);
        }

        return "redirect:/inmuebles/" + id;
    }

    // ── POST /inmuebles/{id}/media/{mediaId}/eliminar ────────
    // Solo borra el registro de BD — el archivo en Cloudinary se conserva
    @PostMapping("/inmuebles/{id}/media/{mediaId}/eliminar")
    public String deleteMedia(@PathVariable Long id,
                              @PathVariable Long mediaId) {
        PropertyMedia media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        mediaRepository.delete(media);
        return "redirect:/inmuebles/" + id;
    }

    // ── Helpers ─────────────────────────────────────────────
    private static String t(String s) { return s == null ? "" : s.trim(); }
}

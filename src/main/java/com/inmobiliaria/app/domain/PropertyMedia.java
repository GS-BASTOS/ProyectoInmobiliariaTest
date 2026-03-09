package com.inmobiliaria.app.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "property_media")
public class PropertyMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "original_name", length = 255)
    private String originalName;

    @Column(name = "media_type", length = 20)
    private String mediaType; // "IMAGE" | "VIDEO" | "PDF" | "DOCUMENT"

    @Column(name = "cloudinary_url", length = 512)
    private String cloudinaryUrl;

    @Column(name = "cloudinary_id", length = 255)
    private String cloudinaryId;

    // ── Getters & Setters ──────────────────────────────────

    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }

    public Property getProperty()              { return property; }
    public void setProperty(Property p)        { this.property = p; }

    public String getOriginalName()            { return originalName; }
    public void setOriginalName(String v)      { this.originalName = v; }

    public String getMediaType()               { return mediaType; }
    public void setMediaType(String v)         { this.mediaType = v; }

    public String getCloudinaryUrl()           { return cloudinaryUrl; }
    public void setCloudinaryUrl(String v)     { this.cloudinaryUrl = v; }

    public String getCloudinaryId()            { return cloudinaryId; }
    public void setCloudinaryId(String v)      { this.cloudinaryId = v; }

    // ── URL de descarga con nombre original ───────────────
    // Inserta fl_attachment:<nombre> en la URL de Cloudinary
    // para que el navegador descargue el archivo con su nombre real.
    @Transient
    public String getDownloadUrl() {
        if (cloudinaryUrl == null) return "#";
        String name = (originalName != null ? originalName : "archivo");

        // 1. Eliminar acentos
        name = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
                                   .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // 2. Limpiar caracteres problemáticos
        name = name.replace(" ",  "_")
                   .replace(",",  "")
                   .replace(";",  "")
                   .replace("(",  "")
                   .replace(")",  "")
                   .replace("&",  "")
                   .replace("?",  "")
                   .replace("#",  "")
                   .replace("[",  "")
                   .replace("]",  "")
                   .replace("'",  "")
                   .replace("\"", "")
                   .replace("/",  "-");

        // 3. ── CLAVE: quitar la extensión del nombre para fl_attachment ──
        //    Cloudinary confunde "archivo.pdf" con una transformación
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }

        return cloudinaryUrl.replace("/upload/", "/upload/fl_attachment:" + name + "/");
    }


}

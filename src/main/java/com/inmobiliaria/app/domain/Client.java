package com.inmobiliaria.app.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClientType clientType = ClientType.PARTICULAR;

    @NotBlank
    @Size(max = 140)
    @Column(nullable = false, length = 140)
    private String fullName;

    @Size(max = 120)
    @Column(length = 120)
    private String companyName;

    @Size(max = 500)
    @Column(length = 500)
    private String generalNotes;

    @Size(max = 60)
    @Column(length = 60)
    private String solviaCode;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean posibleOcupa = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean compradorFinal = false;

    // ── NUEVO ──────────────────────────────────────────────
    @Size(max = 500)
    @Column(length = 500)
    private String motivoContacto;
    // ───────────────────────────────────────────────────────

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC, id ASC")
    private List<ClientPhone> phones = new ArrayList<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC, id ASC")
    private List<ClientEmail> emails = new ArrayList<>();

    // ── Getters y setters ──────────────────────────────────

    public Long getId() { return id; }
    public ClientType getClientType() { return clientType; }
    public void setClientType(ClientType clientType) { this.clientType = clientType; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getGeneralNotes() { return generalNotes; }
    public void setGeneralNotes(String generalNotes) { this.generalNotes = generalNotes; }
    public String getSolviaCode() { return solviaCode; }
    public void setSolviaCode(String solviaCode) { this.solviaCode = solviaCode; }
    public boolean isPosibleOcupa() { return posibleOcupa; }
    public void setPosibleOcupa(boolean posibleOcupa) { this.posibleOcupa = posibleOcupa; }
    public boolean isCompradorFinal() { return compradorFinal; }
    public void setCompradorFinal(boolean compradorFinal) { this.compradorFinal = compradorFinal; }
    public String getMotivoContacto() { return motivoContacto; }
    public void setMotivoContacto(String motivoContacto) { this.motivoContacto = motivoContacto; }
    public List<ClientPhone> getPhones() { return phones; }
    public void setPhones(List<ClientPhone> phones) { this.phones = phones; }
    public List<ClientEmail> getEmails() { return emails; }
    public void setEmails(List<ClientEmail> emails) { this.emails = emails; }
}

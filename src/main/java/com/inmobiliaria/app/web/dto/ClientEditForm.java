package com.inmobiliaria.app.web.dto;

import com.inmobiliaria.app.domain.ClientType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class ClientEditForm {

    @NotNull
    private Long id;

    @NotNull
    private ClientType clientType;

    @NotBlank
    @Size(max = 140)
    private String fullName;

    @Size(max = 120)
    private String companyName;

    @Size(max = 60)
    private String solviaCode;

    @NotBlank
    @Size(max = 30)
    private String phone1;

    @Size(max = 30)
    private String phone2;

    @Size(max = 30)
    private String phone3;

    @Email
    @Size(max = 140)
    private String email1;

    @Email
    @Size(max = 140)
    private String email2;

    @Size(max = 500)
    private String generalNotes;

    private boolean posibleOcupa;
    private boolean compradorFinal;
    private boolean preVenda;

    // ── Lista de inmuebles comprados ──
    private List<Long> purchasedPropertyIds = new ArrayList<>();

    // ── Lista de inmuebles pre-vendidos ──
    private List<Long> preVendaPropertyIds = new ArrayList<>();

    // ── Getters / Setters ────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ClientType getClientType() { return clientType; }
    public void setClientType(ClientType clientType) { this.clientType = clientType; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getSolviaCode() { return solviaCode; }
    public void setSolviaCode(String solviaCode) { this.solviaCode = solviaCode; }

    public String getPhone1() { return phone1; }
    public void setPhone1(String phone1) { this.phone1 = phone1; }

    public String getPhone2() { return phone2; }
    public void setPhone2(String phone2) { this.phone2 = phone2; }

    public String getPhone3() { return phone3; }
    public void setPhone3(String phone3) { this.phone3 = phone3; }

    public String getEmail1() { return email1; }
    public void setEmail1(String email1) { this.email1 = email1; }

    public String getEmail2() { return email2; }
    public void setEmail2(String email2) { this.email2 = email2; }

    public String getGeneralNotes() { return generalNotes; }
    public void setGeneralNotes(String generalNotes) { this.generalNotes = generalNotes; }

    public boolean isPosibleOcupa() { return posibleOcupa; }
    public void setPosibleOcupa(boolean posibleOcupa) { this.posibleOcupa = posibleOcupa; }

    public boolean isCompradorFinal() { return compradorFinal; }
    public void setCompradorFinal(boolean compradorFinal) { this.compradorFinal = compradorFinal; }

    public boolean isPreVenda() { return preVenda; }
    public void setPreVenda(boolean preVenda) { this.preVenda = preVenda; }

    public List<Long> getPurchasedPropertyIds() { return purchasedPropertyIds; }
    public void setPurchasedPropertyIds(List<Long> purchasedPropertyIds) {
        this.purchasedPropertyIds = purchasedPropertyIds != null ? purchasedPropertyIds : new ArrayList<>();
    }

    public List<Long> getPreVendaPropertyIds() { return preVendaPropertyIds; }
    public void setPreVendaPropertyIds(List<Long> preVendaPropertyIds) {
        this.preVendaPropertyIds = preVendaPropertyIds != null ? preVendaPropertyIds : new ArrayList<>();
    }
    
    @Size(max = 500)
    private String motivoContacto;

    public String getMotivoContacto() { return motivoContacto; }
    public void setMotivoContacto(String motivoContacto) { this.motivoContacto = motivoContacto; }

}

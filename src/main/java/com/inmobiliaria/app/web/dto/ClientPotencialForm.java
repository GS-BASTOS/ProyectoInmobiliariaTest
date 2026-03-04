package com.inmobiliaria.app.web.dto;

import com.inmobiliaria.app.domain.ClientType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ClientPotencialForm {

    @NotNull
    private ClientType clientType = ClientType.PARTICULAR;

    @NotBlank
    @Size(max = 140)
    private String fullName;

    @Size(max = 120)
    private String companyName;

    @Size(max = 60)
    private String solviaCode;

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
    private String motivoContacto;

    // ── Getters / Setters ────────────────────────────────

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

    public String getMotivoContacto() { return motivoContacto; }
    public void setMotivoContacto(String motivoContacto) { this.motivoContacto = motivoContacto; }
}

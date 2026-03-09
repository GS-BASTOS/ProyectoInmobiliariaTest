package com.inmobiliaria.app.web.dto;

import com.inmobiliaria.app.domain.ClientType;
import com.inmobiliaria.app.domain.ContactChannel;
import com.inmobiliaria.app.domain.InterestStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class AddClientInterestForm {

    @NotNull
    private ClientType clientType;

    @NotNull
    private LocalDate contactDate;

    @NotNull
    private ContactChannel channel;

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

    @NotNull
    private InterestStatus status;

    @Size(max = 140)
    private String municipality;

    @NotBlank
    @Size(max = 60)
    private String propertyCode;

    @Size(max = 60)
    private String propertyType;

    @Size(max = 140)
    private String address;

    @Size(max = 500)
    private String comments;

    // ── NO MOLESTAR ────────────────────────────────────────
    private boolean noMolestar;
    // ───────────────────────────────────────────────────────

    public AddClientInterestForm() {}

    public ClientType getClientType() { return clientType; }
    public void setClientType(ClientType clientType) { this.clientType = clientType; }

    public LocalDate getContactDate() { return contactDate; }
    public void setContactDate(LocalDate contactDate) { this.contactDate = contactDate; }

    public ContactChannel getChannel() { return channel; }
    public void setChannel(ContactChannel channel) { this.channel = channel; }

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

    public InterestStatus getStatus() { return status; }
    public void setStatus(InterestStatus status) { this.status = status; }

    public String getMunicipality() { return municipality; }
    public void setMunicipality(String municipality) { this.municipality = municipality; }

    public String getPropertyCode() { return propertyCode; }
    public void setPropertyCode(String propertyCode) { this.propertyCode = propertyCode; }

    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public boolean isNoMolestar() { return noMolestar; }
    public void setNoMolestar(boolean noMolestar) { this.noMolestar = noMolestar; }
}

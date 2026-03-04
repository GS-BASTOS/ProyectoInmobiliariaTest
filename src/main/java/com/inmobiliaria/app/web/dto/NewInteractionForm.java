package com.inmobiliaria.app.web.dto;

import com.inmobiliaria.app.domain.ContactChannel;
import com.inmobiliaria.app.domain.InterestStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class NewInteractionForm {

    @NotNull
    private LocalDate contactDate = LocalDate.now();

    @Size(max = 60)
    private String solviaCode;

    @NotNull
    private ContactChannel channel = ContactChannel.OTRO;

    @NotNull
    private InterestStatus status = InterestStatus.VERDE_PENSANDO;

    @Size(max = 1200)
    private String comments;

    @NotBlank
    @Size(max = 60)
    private String propertyCode;

    @Size(max = 80)
    private String propertyType;

    @Size(max = 160)
    private String address;

    @Size(max = 80)
    private String municipality;

    @Size(max = 30)
    private String phone;

    @Size(max = 140)
    private String email;

    public LocalDate getContactDate() { return contactDate; }
    public void setContactDate(LocalDate contactDate) { this.contactDate = contactDate; }
    
    public String getSolviaCode() { return solviaCode; }
    public void setSolviaCode(String solviaCode) { this.solviaCode = solviaCode; }
    
    public ContactChannel getChannel() { return channel; }
    public void setChannel(ContactChannel channel) { this.channel = channel; }
    
    public InterestStatus getStatus() { return status; }
    public void setStatus(InterestStatus status) { this.status = status; }
    
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    
    public String getPropertyCode() { return propertyCode; }
    public void setPropertyCode(String propertyCode) { this.propertyCode = propertyCode; }

    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getMunicipality() { return municipality; }
    public void setMunicipality(String municipality) { this.municipality = municipality; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

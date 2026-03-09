package com.inmobiliaria.app.web;

import com.inmobiliaria.app.domain.*;
import com.inmobiliaria.app.repo.ClientPhoneRepository;
import com.inmobiliaria.app.repo.ClientPropertyInteractionRepository;
import com.inmobiliaria.app.repo.ClientRepository;
import com.inmobiliaria.app.repo.PropertyRepository;
import com.inmobiliaria.app.web.dto.AddClientInterestForm;
import com.inmobiliaria.app.web.dto.PropertyCatalogDto;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class AddController {

    private final ClientRepository clientRepository;
    private final ClientPhoneRepository clientPhoneRepository;
    private final PropertyRepository propertyRepository;
    private final ClientPropertyInteractionRepository interactionRepository;

    public AddController(ClientRepository clientRepository,
                          ClientPhoneRepository clientPhoneRepository,
                          PropertyRepository propertyRepository,
                          ClientPropertyInteractionRepository interactionRepository) {
        this.clientRepository    = clientRepository;
        this.clientPhoneRepository = clientPhoneRepository;
        this.propertyRepository  = propertyRepository;
        this.interactionRepository = interactionRepository;
    }

    @GetMapping("/agregar")
    public String addForm(Model model) {
        model.addAttribute("form", new AddClientInterestForm());
        reloadCombos(model);
        model.addAttribute("existingClientId", null);
        return "add";
    }

    @PostMapping("/agregar")
    @Transactional
    public String add(@Valid @ModelAttribute("form") AddClientInterestForm form,
                      BindingResult br,
                      Model model) {

        String p1 = normalizePhone(form.getPhone1());
        String p2 = normalizePhone(form.getPhone2());
        String p3 = normalizePhone(form.getPhone3());

        if (!p1.isBlank() && !p2.isBlank() && p1.equals(p2)) br.rejectValue("phone2", "dup", "Teléfono repetido.");
        if (!p1.isBlank() && !p3.isBlank() && p1.equals(p3)) br.rejectValue("phone3", "dup", "Teléfono repetido.");
        if (!p2.isBlank() && !p3.isBlank() && p2.equals(p3)) br.rejectValue("phone3", "dup", "Teléfono repetido.");

        if (br.hasErrors()) {
            reloadCombos(model);
            model.addAttribute("existingClientId", null);
            return "add";
        }

        Long existingClientId = null;

        Long owner1 = checkPhoneUniqueForCreate(br, "phone1", p1);
        if (owner1 != null) existingClientId = owner1;

        if (!p2.isBlank()) {
            Long owner2 = checkPhoneUniqueForCreate(br, "phone2", p2);
            if (existingClientId == null && owner2 != null) existingClientId = owner2;
        }

        if (!p3.isBlank()) {
            Long owner3 = checkPhoneUniqueForCreate(br, "phone3", p3);
            if (existingClientId == null && owner3 != null) existingClientId = owner3;
        }

        if (br.hasErrors()) {
            reloadCombos(model);
            model.addAttribute("existingClientId", existingClientId);
            return "add";
        }

        Client client = new Client();
        client.setClientType(form.getClientType());
        client.setFullName(t(form.getFullName()));
        client.setCompanyName(t(form.getCompanyName()));
        client.setGeneralNotes("");
        client.setSolviaCode(t(form.getSolviaCode()));
        client.setNoMolestar(form.isNoMolestar());                   // ← NO MOLESTAR

        if (!p1.isBlank()) addPhone(client, p1, 1);
        if (!p2.isBlank()) addPhone(client, p2, 2);
        if (!p3.isBlank()) addPhone(client, p3, 3);

        String e1 = t(form.getEmail1());
        String e2 = t(form.getEmail2());
        if (!e1.isBlank()) addEmail(client, e1, 1);
        if (!e2.isBlank()) addEmail(client, e2, 2);

        Client savedClient;
        try {
            savedClient = clientRepository.save(client);
        } catch (DataIntegrityViolationException ex) {
            br.reject("dbUnique", "No se pudo guardar: hay un teléfono repetido ya asignado en el sistema.");
            reloadCombos(model);
            model.addAttribute("existingClientId", null);
            return "add";
        }

        String code = t(form.getPropertyCode());
        Property property = propertyRepository.findByPropertyCode(code)
                .orElseGet(() -> {
                    Property p = new Property();
                    p.setPropertyCode(code);
                    p.setPropertyType(t(form.getPropertyType()));
                    p.setAddress(t(form.getAddress()));
                    p.setMunicipality(t(form.getMunicipality()));
                    p.setNotes("");
                    return propertyRepository.save(p);
                });

        ClientPropertyInteraction interaction = new ClientPropertyInteraction();
        interaction.setClient(savedClient);
        interaction.setProperty(property);
        interaction.setContactDate(form.getContactDate());
        interaction.setChannel(form.getChannel());
        interaction.setStatus(form.getStatus());
        interaction.setComments(t(form.getComments()));
        interactionRepository.save(interaction);

        return "redirect:/clientes/" + savedClient.getId();
    }

    private void reloadCombos(Model model) {
        model.addAttribute("clientTypes", ClientType.values());
        model.addAttribute("channels", ContactChannel.values());
        model.addAttribute("statuses", InterestStatus.values());

        List<PropertyCatalogDto> catalog = propertyRepository
                .findAllByOrderByPropertyCodeAsc()
                .stream()
                .filter(p -> !p.isSold())
                .map(p -> new PropertyCatalogDto(
                        p.getId(), p.getPropertyCode(), p.getPropertyType(),
                        p.getAddress(), p.getMunicipality(),
                        p.isPreVendido(), p.isSold()))
                .collect(Collectors.toList());

        model.addAttribute("catalogProperties", catalog);
    }

    private Long checkPhoneUniqueForCreate(BindingResult br, String fieldName, String phone) {
        if (phone == null || phone.isBlank()) return null;
        return clientPhoneRepository.findFirstByPhoneNumber(phone)
                .map(found -> {
                    Long ownerId = found.getClient().getId();
                    br.rejectValue(fieldName, "phoneExists",
                            "Ese teléfono ya está asignado a otro cliente (ID " + ownerId + ").");
                    return ownerId;
                })
                .orElse(null);
    }

    private static String normalizePhone(String s) {
        if (s == null) return "";
        String trimmed = s.trim();
        if (trimmed.isEmpty()) return "";
        boolean hasDdi = trimmed.startsWith("+");
        String digits = trimmed.replaceAll("[^0-9]", "");
        return hasDdi ? "+" + digits : digits;
    }

    private static String t(String s) { return s == null ? "" : s.trim(); }

    private void addPhone(Client client, String number, int position) {
        ClientPhone p = new ClientPhone();
        p.setClient(client);
        p.setPhoneNumber(number);
        p.setPosition(position);
        client.getPhones().add(p);
    }

    private void addEmail(Client client, String email, int position) {
        ClientEmail e = new ClientEmail();
        e.setClient(client);
        e.setEmail(email);
        e.setPosition(position);
        client.getEmails().add(e);
    }
}

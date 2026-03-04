package com.inmobiliaria.app.web;

import com.inmobiliaria.app.domain.*;
import com.inmobiliaria.app.repo.ClientPhoneRepository;
import com.inmobiliaria.app.repo.ClientPropertyInteractionRepository;
import com.inmobiliaria.app.repo.ClientRepository;
import com.inmobiliaria.app.repo.PropertyRepository;
import com.inmobiliaria.app.repo.VisitRepository;
import com.inmobiliaria.app.web.dto.ClientEditForm;
import com.inmobiliaria.app.web.dto.NewInteractionForm;
import com.inmobiliaria.app.web.dto.PropertyCatalogDto;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ClientDetailController {

    private final ClientRepository clientRepository;
    private final ClientPhoneRepository clientPhoneRepository;
    private final ClientPropertyInteractionRepository interactionRepository;
    private final VisitRepository visitRepository;
    private final PropertyRepository propertyRepository;

    public ClientDetailController(ClientRepository clientRepository,
                                  ClientPhoneRepository clientPhoneRepository,
                                  ClientPropertyInteractionRepository interactionRepository,
                                  VisitRepository visitRepository,
                                  PropertyRepository propertyRepository) {
        this.clientRepository    = clientRepository;
        this.clientPhoneRepository = clientPhoneRepository;
        this.interactionRepository = interactionRepository;
        this.visitRepository     = visitRepository;
        this.propertyRepository  = propertyRepository;
    }

    // ── GET /clientes/{id} ───────────────────────────────────
    @GetMapping("/clientes/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Client client = clientRepository.findWithPhonesById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        clientRepository.findWithEmailsById(id).ifPresent(c2 -> client.setEmails(c2.getEmails()));
        List<ClientPropertyInteraction> interactions =
                interactionRepository.findByClientIdWithPropertyOrderByContactDateDesc(client.getId());
        ClientEditForm form = buildEditForm(client, interactions);
        NewInteractionForm ni = buildPrefilledInteractionForm(client, interactions);
        model.addAttribute("client",       client);
        model.addAttribute("form",         form);
        model.addAttribute("clientTypes",  ClientType.values());
        model.addAttribute("newInteraction", ni);
        model.addAttribute("channels",     ContactChannel.values());
        model.addAttribute("statuses",     InterestStatus.values());
        model.addAttribute("interactions", interactions);
        model.addAttribute("visits",
                visitRepository.findByClient_IdOrderByVisitAtDescIdDesc(client.getId()));
        model.addAttribute("catalogProperties", buildCatalog());
        // ── NUEVO ──────────────────────────────────────────
        model.addAttribute("motivoContacto", client.getMotivoContacto());
        // ───────────────────────────────────────────────────
        return "client_detail";
    }

    // ── POST NDA ─────────────────────────────────────────────
    @PostMapping("/clientes/{clientId}/interacciones/{interactionId}/nda")
    @ResponseBody
    public void toggleNda(@PathVariable Long clientId,
                          @PathVariable Long interactionId,
                          @RequestParam("ndaRequested") boolean ndaRequested) {
        ClientPropertyInteraction it = interactionRepository.findById(interactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (it.getClient() == null || !it.getClient().getId().equals(clientId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        it.setNdaRequested(ndaRequested);
        interactionRepository.save(it);
    }

    // ── POST STATUS ──────────────────────────────────────────
    @PostMapping("/clientes/{clientId}/interacciones/{interactionId}/status")
    @ResponseBody
    public void updateInteractionStatus(@PathVariable Long clientId,
                                        @PathVariable Long interactionId,
                                        @RequestParam("status") InterestStatus status) {
        ClientPropertyInteraction it = interactionRepository.findById(interactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (it.getClient() == null || !it.getClient().getId().equals(clientId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        it.setStatus(status);
        interactionRepository.save(it);
    }

    // ── POST COMMENTS ────────────────────────────────────────
    @PostMapping("/clientes/{clientId}/interacciones/{interactionId}/comments")
    @ResponseBody
    public void updateInteractionComments(@PathVariable Long clientId,
                                          @PathVariable Long interactionId,
                                          @RequestParam("comments") String comments) {
        ClientPropertyInteraction it = interactionRepository.findById(interactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (it.getClient() == null || !it.getClient().getId().equals(clientId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        String sanitized = (comments == null) ? "" : comments;
        it.setComments(sanitized.isBlank() ? "" : sanitized);
        interactionRepository.save(it);
    }

    // ── POST TICKET ──────────────────────────────────────────
    @PostMapping("/clientes/{clientId}/interacciones/{interactionId}/ticket")
    @ResponseBody
    public void updateTicket(@PathVariable Long clientId,
                             @PathVariable Long interactionId,
                             @RequestParam("ticketCode") String ticketCode) {
        ClientPropertyInteraction it = interactionRepository.findById(interactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (it.getClient() == null || !it.getClient().getId().equals(clientId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        String code = ticketCode == null ? "" : ticketCode.trim();
        it.setTicketCode(code.isBlank() ? null : code);
        interactionRepository.save(it);
    }

    // ── POST EDITAR ──────────────────────────────────────────
    @PostMapping("/clientes/{id}/editar")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") ClientEditForm form,
                         BindingResult br,
                         Model model) {
        Client client = clientRepository.findWithPhonesById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        clientRepository.findWithEmailsById(id).ifPresent(c2 -> client.setEmails(c2.getEmails()));

        String p1 = normalizePhone(form.getPhone1());
        String p2 = normalizePhone(form.getPhone2());
        String p3 = normalizePhone(form.getPhone3());

        if (!p2.isBlank() && p2.equals(p1)) br.rejectValue("phone2", "dup", "Teléfono repetido.");
        if (!p3.isBlank() && (p3.equals(p1) || p3.equals(p2))) br.rejectValue("phone3", "dup", "Teléfono repetido.");
        checkPhoneUnique(br, "phone1", p1, client.getId());
        if (!p2.isBlank()) checkPhoneUnique(br, "phone2", p2, client.getId());
        if (!p3.isBlank()) checkPhoneUnique(br, "phone3", p3, client.getId());

        if (br.hasErrors()) {
            List<ClientPropertyInteraction> interactions =
                    interactionRepository.findByClientIdWithPropertyOrderByContactDateDesc(client.getId());
            repopulateDetailModel(model, client, form,
                    buildPrefilledInteractionForm(client, interactions));
            return "client_detail";
        }

        boolean eraCompradorAntes = client.isCompradorFinal();
        boolean eraOcupaAntes     = client.isPosibleOcupa();

        client.setClientType(form.getClientType());
        client.setFullName(form.getFullName());
        client.setCompanyName(t(form.getCompanyName()));
        client.setGeneralNotes(form.getGeneralNotes());
        client.setSolviaCode(t(form.getSolviaCode()));
        client.setPosibleOcupa(form.isPosibleOcupa());
        client.setCompradorFinal(form.isCompradorFinal());
        // ── NUEVO ──────────────────────────────────────────
        client.setMotivoContacto(t(form.getMotivoContacto()));
        // ───────────────────────────────────────────────────

        List<ClientPropertyInteraction> interactions =
                interactionRepository.findByClientIdWithPropertyOrderByContactDateDesc(client.getId());

        // CASO 1: se desmarca comprador → liberar inmuebles
        if (eraCompradorAntes && !form.isCompradorFinal()) {
            interactions.stream()
                    .map(ClientPropertyInteraction::getProperty)
                    .distinct()
                    .filter(Property::isSold)
                    .forEach(p -> {
                        p.setSold(false);
                        p.setSoldClient(null);
                        propertyRepository.save(p);
                    });
        }

        // CASO 2: se marca pre venda
        if (form.isPreVenda()) {
            List<Long> preVendaIds = form.getPreVendaPropertyIds();
            if (preVendaIds != null && !preVendaIds.isEmpty()) {
                preVendaIds.forEach(propId ->
                        propertyRepository.findById(propId).ifPresent(p -> {
                            if (!p.isSold()) {
                                p.setPreVendido(true);
                                p.setPreVendidoClient(client);
                                propertyRepository.save(p);
                            }
                        })
                );
            }
        }

        // CASO 3: se marca comprador
        if (form.isCompradorFinal()) {
            List<Long> boughtIds = form.getPurchasedPropertyIds();
            if (boughtIds != null && !boughtIds.isEmpty()) {
                boughtIds.forEach(propId ->
                        propertyRepository.findById(propId).ifPresent(p -> {
                            p.setSold(true);
                            p.setSoldClient(client);
                            p.setPreVendido(false);
                            p.setPreVendidoClient(null);
                            propertyRepository.save(p);
                        })
                );
                interactions.forEach(it -> {
                    if (!boughtIds.contains(it.getProperty().getId())) {
                        it.setStatus(InterestStatus.ROSA_DESCARTA);
                        interactionRepository.save(it);
                    }
                });
            }
        }

        // CASO 4: se marca okupa → todas a DESCARTA
        if (!eraOcupaAntes && form.isPosibleOcupa()) {
            interactions.forEach(it -> {
                it.setStatus(InterestStatus.ROSA_DESCARTA);
                interactionRepository.save(it);
            });
        }

        upsertPhone(client, 1, p1);
        upsertPhone(client, 2, p2);
        upsertPhone(client, 3, p3);
        upsertEmail(client, 1, t(form.getEmail1()));
        upsertEmail(client, 2, t(form.getEmail2()));

        try {
            clientRepository.save(client);
        } catch (DataIntegrityViolationException ex) {
            br.reject("dbUnique", "No se pudo guardar: hay un teléfono repetido.");
            repopulateDetailModel(model, client, form,
                    buildPrefilledInteractionForm(client, interactions));
            return "client_detail";
        }

        return "redirect:/clientes/" + id;
    }

    // ── POST NUEVA INTERACCIÓN ───────────────────────────────
    @PostMapping("/clientes/{id}/interacciones")
    public String addInteraction(@PathVariable Long id,
                                 @Valid @ModelAttribute("newInteraction") NewInteractionForm form,
                                 BindingResult br,
                                 Model model) {
        Client client = clientRepository.findWithPhonesById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        clientRepository.findWithEmailsById(id).ifPresent(c2 -> client.setEmails(c2.getEmails()));
        List<ClientPropertyInteraction> interactions =
                interactionRepository.findByClientIdWithPropertyOrderByContactDateDesc(client.getId());

        if (br.hasErrors()) {
            repopulateDetailModel(model, client, buildEditForm(client, interactions), form);
            model.addAttribute("interactions", interactions);
            return "client_detail";
        }

        String code = t(form.getPropertyCode());
        Property property = propertyRepository.findByPropertyCode(code)
                .map(existing -> {
                    String pt   = t(form.getPropertyType());
                    String addr = t(form.getAddress());
                    String mun  = t(form.getMunicipality());
                    if (!pt.isBlank())   existing.setPropertyType(pt);
                    if (!addr.isBlank()) existing.setAddress(addr);
                    if (!mun.isBlank())  existing.setMunicipality(mun);
                    return propertyRepository.save(existing);
                })
                .orElseGet(() -> {
                    Property p = new Property();
                    p.setPropertyCode(code);
                    p.setPropertyType(t(form.getPropertyType()));
                    p.setAddress(t(form.getAddress()));
                    p.setMunicipality(t(form.getMunicipality()));
                    return propertyRepository.save(p);
                });

        ClientPropertyInteraction interaction = new ClientPropertyInteraction();
        interaction.setClient(client);
        interaction.setProperty(property);
        interaction.setContactDate(form.getContactDate());
        interaction.setChannel(form.getChannel());
        interaction.setStatus(form.getStatus());
        interaction.setSolviaCode(t(form.getSolviaCode()));
        interaction.setComments(t(form.getComments()));
        interaction.setNdaRequested(false);
        interactionRepository.save(interaction);

        return "redirect:/clientes/" + id;
    }

    // ── HELPERS ──────────────────────────────────────────────

    private List<PropertyCatalogDto> buildCatalog() {
        return propertyRepository.findAllByOrderByPropertyCodeAsc()
                .stream()
                .filter(p -> !p.isSold())
                .map(p -> new PropertyCatalogDto(
                        p.getId(), p.getPropertyCode(), p.getPropertyType(),
                        p.getAddress(), p.getMunicipality(),
                        p.isPreVendido(), p.isSold()))
                .collect(Collectors.toList());
    }

    private ClientEditForm buildEditForm(Client client,
                                          List<ClientPropertyInteraction> interactions) {
        ClientEditForm form = new ClientEditForm();
        form.setId(client.getId());
        form.setClientType(client.getClientType());
        form.setFullName(client.getFullName());
        form.setCompanyName(client.getCompanyName());
        form.setGeneralNotes(client.getGeneralNotes());
        form.setSolviaCode(client.getSolviaCode());
        form.setPosibleOcupa(client.isPosibleOcupa());
        form.setCompradorFinal(client.isCompradorFinal());
        // ── NUEVO ──────────────────────────────────────────
        form.setMotivoContacto(client.getMotivoContacto());
        // ───────────────────────────────────────────────────
        form.setPhone1(client.getPhones().stream()
                .filter(p -> p.getPosition() == 1)
                .map(ClientPhone::getPhoneNumber).findFirst().orElse(""));
        form.setPhone2(client.getPhones().stream()
                .filter(p -> p.getPosition() == 2)
                .map(ClientPhone::getPhoneNumber).findFirst().orElse(""));
        form.setPhone3(client.getPhones().stream()
                .filter(p -> p.getPosition() == 3)
                .map(ClientPhone::getPhoneNumber).findFirst().orElse(""));
        form.setEmail1(client.getEmails().stream()
                .filter(e -> e.getPosition() == 1)
                .map(ClientEmail::getEmail).findFirst().orElse(""));
        form.setEmail2(client.getEmails().stream()
                .filter(e -> e.getPosition() == 2)
                .map(ClientEmail::getEmail).findFirst().orElse(""));

        if (interactions != null) {
            List<Long> preVendidos = interactions.stream()
                    .map(ClientPropertyInteraction::getProperty)
                    .filter(p -> p.isPreVendido() && !p.isSold())
                    .map(Property::getId)
                    .distinct().toList();
            form.setPreVendaPropertyIds(preVendidos);
            if (!preVendidos.isEmpty()) form.setPreVenda(true);

            if (client.isCompradorFinal()) {
                List<Long> vendidos = interactions.stream()
                        .map(ClientPropertyInteraction::getProperty)
                        .filter(Property::isSold)
                        .map(Property::getId)
                        .distinct().toList();
                form.setPurchasedPropertyIds(vendidos);
            }
        }
        return form;
    }

    private NewInteractionForm buildPrefilledInteractionForm(
            Client client, List<ClientPropertyInteraction> interactions) {
        NewInteractionForm ni = new NewInteractionForm();
        ni.setPhone(client.getPhones().stream()
                .filter(p -> p.getPosition() != null && p.getPosition() == 1)
                .map(ClientPhone::getPhoneNumber).findFirst().orElse(""));
        ni.setEmail(client.getEmails().stream()
                .filter(e -> e.getPosition() != null && e.getPosition() == 1)
                .map(ClientEmail::getEmail).findFirst().orElse(""));
        ni.setSolviaCode(t(client.getSolviaCode()));
        if (interactions != null && !interactions.isEmpty()) {
            ClientPropertyInteraction last = interactions.get(0);
            if (last.getChannel() != null) ni.setChannel(last.getChannel());
            if (last.getStatus()  != null) ni.setStatus(last.getStatus());
            if (last.getProperty() != null) {
                Property p = last.getProperty();
                if (!t(p.getPropertyCode()).isBlank()) ni.setPropertyCode(p.getPropertyCode());
                if (!t(p.getPropertyType()).isBlank()) ni.setPropertyType(p.getPropertyType());
                if (!t(p.getAddress()).isBlank())      ni.setAddress(p.getAddress());
                if (!t(p.getMunicipality()).isBlank()) ni.setMunicipality(p.getMunicipality());
            }
        }
        return ni;
    }

    private void repopulateDetailModel(Model model, Client client,
                                        ClientEditForm editForm,
                                        NewInteractionForm newInteractionForm) {
        model.addAttribute("client",       client);
        model.addAttribute("form",         editForm);
        model.addAttribute("clientTypes",  ClientType.values());
        model.addAttribute("newInteraction", newInteractionForm);
        model.addAttribute("channels",     ContactChannel.values());
        model.addAttribute("statuses",     InterestStatus.values());
        model.addAttribute("interactions",
                interactionRepository.findByClientIdWithPropertyOrderByContactDateDesc(client.getId()));
        model.addAttribute("visits",
                visitRepository.findByClient_IdOrderByVisitAtDescIdDesc(client.getId()));
        model.addAttribute("catalogProperties", buildCatalog());
        // ── NUEVO ──────────────────────────────────────────
        model.addAttribute("motivoContacto", client.getMotivoContacto());
        // ───────────────────────────────────────────────────
    }

    private void upsertPhone(Client client, int position, String number) {
        String n = t(number);
        ClientPhone existing = client.getPhones().stream()
                .filter(p -> p.getPosition() != null && p.getPosition() == position)
                .findFirst().orElse(null);
        if (n.isBlank()) { if (existing != null) client.getPhones().remove(existing); return; }
        if (existing == null) {
            ClientPhone p = new ClientPhone();
            p.setClient(client); p.setPosition(position); p.setPhoneNumber(n);
            client.getPhones().add(p);
        } else { existing.setPhoneNumber(n); }
    }

    private void upsertEmail(Client client, int position, String email) {
        String e = t(email);
        ClientEmail existing = client.getEmails().stream()
                .filter(x -> x.getPosition() != null && x.getPosition() == position)
                .findFirst().orElse(null);
        if (e.isBlank()) { if (existing != null) client.getEmails().remove(existing); return; }
        if (existing == null) {
            ClientEmail ce = new ClientEmail();
            ce.setClient(client); ce.setPosition(position); ce.setEmail(e);
            client.getEmails().add(ce);
        } else { existing.setEmail(e); }
    }

    private void checkPhoneUnique(BindingResult br, String fieldName,
                                   String phone, Long editingClientId) {
        if (phone == null || phone.isBlank()) return;
        clientPhoneRepository.findFirstByPhoneNumber(phone).ifPresent(found -> {
            if (!found.getClient().getId().equals(editingClientId))
                br.rejectValue(fieldName, "phoneExists",
                        "Ese teléfono ya está asignado a otro cliente (ID "
                                + found.getClient().getId() + ").");
        });
    }

    private static String normalizePhone(String s) {
        if (s == null) return "";
        String trimmed = s.trim();
        if (trimmed.isEmpty()) return "";
        boolean hasDdi = trimmed.startsWith("+");
        String digits  = trimmed.replaceAll("[^0-9]", "");
        return hasDdi ? "+" + digits : digits;
    }

    private static String t(String s) { return s == null ? "" : s.trim(); }
}

package com.inmobiliaria.app.web;

import com.inmobiliaria.app.domain.*;
import com.inmobiliaria.app.repo.*;
import com.inmobiliaria.app.web.dto.ClientPotencialForm;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/clientes")
public class ClientesController {

    private final ClientRepository                    clientRepo;
    private final ClientPhoneRepository               phoneRepo;
    private final ClientEmailRepository               emailRepo;
    private final ClientPropertyInteractionRepository interactionRepo;

    public ClientesController(ClientRepository clientRepo,
                               ClientPhoneRepository phoneRepo,
                               ClientEmailRepository emailRepo,
                               ClientPropertyInteractionRepository interactionRepo) {
        this.clientRepo      = clientRepo;
        this.phoneRepo       = phoneRepo;
        this.emailRepo       = emailRepo;
        this.interactionRepo = interactionRepo;
    }

    /* ══════════════════════════════════════════
       LISTA DE CLIENTES
    ══════════════════════════════════════════ */
    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) String tipo,
                       @RequestParam(required = false) String telefono,
                       @RequestParam(required = false) String email,
                       Model model) {

        List<Client> clients = clientRepo.searchAll(q == null ? "" : q);

        // ── Filtro por teléfono ───────────────────
        if (telefono != null && !telefono.isBlank()) {
            String tel = telefono.trim().toLowerCase();
            List<Long> idsConTel = phoneRepo.findAll().stream()
                    .filter(p -> p.getPhoneNumber() != null &&
                                 p.getPhoneNumber().toLowerCase().contains(tel))
                    .map(p -> p.getClient().getId())
                    .distinct()
                    .collect(Collectors.toList());
            clients = clients.stream()
                    .filter(c -> idsConTel.contains(c.getId()))
                    .collect(Collectors.toList());
        }

        // ── Filtro por email ──────────────────────
        if (email != null && !email.isBlank()) {
            String em = email.trim().toLowerCase();
            List<Long> idsConEmail = emailRepo.findAll().stream()
                    .filter(e -> e.getEmail() != null &&
                                 e.getEmail().toLowerCase().contains(em))
                    .map(e -> e.getClient().getId())
                    .distinct()
                    .collect(Collectors.toList());
            clients = clients.stream()
                    .filter(c -> idsConEmail.contains(c.getId()))
                    .collect(Collectors.toList());
        }

        // ── Filtro por tipo (potencial / interesado) ──
        List<Long> idsConInteracciones = interactionRepo
                .findAllWithClientAndPropertyOrderByContactDateDesc()
                .stream()
                .map(i -> i.getClient().getId())
                .distinct()
                .collect(Collectors.toList());

        if ("potencial".equals(tipo)) {
            clients = clients.stream()
                    .filter(c -> !idsConInteracciones.contains(c.getId()))
                    .collect(Collectors.toList());
        } else if ("interesado".equals(tipo)) {
            clients = clients.stream()
                    .filter(c -> idsConInteracciones.contains(c.getId()))
                    .collect(Collectors.toList());
        }

        List<Long> ids = clients.stream().map(Client::getId).collect(Collectors.toList());

        Map<Long, List<ClientPhone>> phones = ids.isEmpty() ? Map.of() :
                phoneRepo.findByClient_IdInOrderByClient_IdAscPositionAsc(ids)
                        .stream()
                        .collect(Collectors.groupingBy(p -> p.getClient().getId()));

        Map<Long, List<ClientEmail>> emails = ids.isEmpty() ? Map.of() :
                emailRepo.findByClient_IdInOrderByClient_IdAscPositionAsc(ids)
                        .stream()
                        .collect(Collectors.groupingBy(e -> e.getClient().getId()));

        Map<Long, Long> interactionCount = new HashMap<>();
        for (Long cid : ids) {
            interactionCount.put(cid,
                    (long) interactionRepo
                            .findByClientIdWithPropertyOrderByContactDateDesc(cid)
                            .size());
        }

        model.addAttribute("clients",                   clients);
        model.addAttribute("phonesByClientId",          phones);
        model.addAttribute("emailsByClientId",          emails);
        model.addAttribute("interactionCount",          interactionCount);
        model.addAttribute("clientIdsWithInteractions", idsConInteracciones);
        model.addAttribute("q",           q);
        model.addAttribute("tipo",        tipo);
        model.addAttribute("telefono",    telefono);
        model.addAttribute("email",       email);
        model.addAttribute("clientTypes", ClientType.values());

        return "clientes";
    }

    /* ══════════════════════════════════════════
       FORMULARIO NUEVO CLIENTE POTENCIAL
    ══════════════════════════════════════════ */
    @GetMapping("/nuevo")
    public String nuevoForm(Model model) {
        model.addAttribute("form",        new ClientPotencialForm());
        model.addAttribute("clientTypes", ClientType.values());
        return "agregar-cliente";
    }

    /* ══════════════════════════════════════════
       GUARDAR NUEVO CLIENTE POTENCIAL
    ══════════════════════════════════════════ */
    @PostMapping("/nuevo")
    public String nuevoSave(@Valid @ModelAttribute("form") ClientPotencialForm form,
                             BindingResult result,
                             Model model) {

        // ── Normalizar teléfonos ──────────────────
        String p1 = normalizePhone(form.getPhone1());
        String p2 = normalizePhone(form.getPhone2());
        String p3 = normalizePhone(form.getPhone3());

        // ── Validar duplicados entre sí ───────────
        if (!p1.isBlank() && !p2.isBlank() && p1.equals(p2))
            result.rejectValue("phone2", "dup", "Teléfono repetido.");
        if (!p1.isBlank() && !p3.isBlank() && p1.equals(p3))
            result.rejectValue("phone3", "dup", "Teléfono repetido.");
        if (!p2.isBlank() && !p3.isBlank() && p2.equals(p3))
            result.rejectValue("phone3", "dup", "Teléfono repetido.");

        // ── Validar que no existan en la BD ───────
        Long existingClientId = null;
        if (!p1.isBlank()) existingClientId = checkPhoneUnique(result, "phone1", p1, existingClientId);
        if (!p2.isBlank()) existingClientId = checkPhoneUnique(result, "phone2", p2, existingClientId);
        if (!p3.isBlank()) existingClientId = checkPhoneUnique(result, "phone3", p3, existingClientId);

        if (result.hasErrors()) {
            model.addAttribute("clientTypes",      ClientType.values());
            model.addAttribute("existingClientId", existingClientId);
            return "agregar-cliente";
        }

        // ── Crear y guardar cliente ───────────────
        Client client = new Client();
        client.setClientType(form.getClientType());
        client.setFullName(form.getFullName());
        client.setCompanyName(nvl(form.getCompanyName()));
        client.setSolviaCode(nvl(form.getSolviaCode()));
        client.setMotivoContacto(nvl(form.getMotivoContacto()));

        try {
            client = clientRepo.save(client);
        } catch (DataIntegrityViolationException ex) {
            result.reject("dbUnique", "No se pudo guardar: error de integridad en la base de datos.");
            model.addAttribute("clientTypes", ClientType.values());
            return "agregar-cliente";
        }

        // ── Guardar teléfonos ─────────────────────
        int pos = 1;
        for (String num : List.of(p1, p2, p3)) {
            if (!num.isBlank()) {
                ClientPhone p = new ClientPhone();
                p.setClient(client);
                p.setPhoneNumber(num);
                p.setPosition(pos++);
                try {
                    phoneRepo.save(p);
                } catch (DataIntegrityViolationException ex) {
                    result.reject("dbUnique", "El teléfono " + num + " ya está registrado en otro cliente.");
                    clientRepo.deleteById(client.getId());
                    model.addAttribute("clientTypes", ClientType.values());
                    return "agregar-cliente";
                }
            }
        }

        // ── Guardar emails ────────────────────────
        int epos = 1;
        for (String mail : List.of(nvl(form.getEmail1()), nvl(form.getEmail2()))) {
            if (!mail.isBlank()) {
                ClientEmail e = new ClientEmail();
                e.setClient(client);
                e.setEmail(mail.trim());
                e.setPosition(epos++);
                emailRepo.save(e);
            }
        }

        return "redirect:/clientes/" + client.getId();
    }

    /* ── Helpers ──────────────────────────────── */

    private Long checkPhoneUnique(BindingResult result, String field,
                                   String phone, Long existingId) {
        return phoneRepo.findFirstByPhoneNumber(phone)
                .map(found -> {
                    Long ownerId = found.getClient().getId();
                    result.rejectValue(field, "phoneExists",
                            "Ese teléfono ya está asignado a otro cliente (ID " + ownerId + ").");
                    return ownerId;
                })
                .orElse(existingId);
    }

    private static String normalizePhone(String s) {
        if (s == null) return "";
        String trimmed = s.trim();
        if (trimmed.isEmpty()) return "";
        boolean hasDdi = trimmed.startsWith("+");
        String digits  = trimmed.replaceAll("[^0-9]", "");
        return hasDdi ? "+" + digits : digits;
    }

    private String nvl(String s) { return s == null ? "" : s.trim(); }
}

package com.retiro.jovenes.controller;

import com.retiro.jovenes.model.Persona;
import com.retiro.jovenes.model.Registro;
import com.retiro.jovenes.service.PersonaService;
import com.retiro.jovenes.service.RegistroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class RegistroController {

    @Autowired
    private RegistroService registroService;

    @Autowired
    private PersonaService personaService;

    // ============ VIEWS ============

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/registro")
    public String registroForm(Model model) {
        model.addAttribute("modalidades", Registro.Modalidad.values());
        model.addAttribute("mediosPago", Registro.MedioPago.values());
        return "registro";
    }

    @PostMapping("/registro")
    public String registrar(@RequestParam Long personaId,
            @RequestParam Registro.Modalidad modalidad,
            @RequestParam Registro.MedioPago medioPago,
            @RequestParam(defaultValue = "TOTAL") String tipoPago,
            @RequestParam(required = false) Integer montoAdelanto,
            RedirectAttributes redirectAttributes) {
        if (registroService.yaRegistrado(personaId)) {
            redirectAttributes.addFlashAttribute("error", "Esta persona ya está registrada.");
            return "redirect:/registro";
        }

        Optional<Persona> persona = personaService.findById(personaId);
        if (persona.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Persona no encontrada.");
            return "redirect:/registro";
        }

        Registro registro = new Registro();
        registro.setPersona(persona.get());
        registro.setModalidad(modalidad);
        registro.setMedioPago(medioPago);

        int precioTotal = modalidad.getPrecio();
        if ("ADELANTO".equalsIgnoreCase(tipoPago) && montoAdelanto != null && montoAdelanto > 0) {
            registro.setMontoPagado(Math.min(montoAdelanto, precioTotal));
        } else {
            registro.setMontoPagado(precioTotal);
        }

        registroService.save(registro);

        String detalle = registro.isPagoCompleto() ? " (Pago Completo)"
                : " (Adelanto S/ " + registro.getMontoPagado() + ", Saldo: S/ " + registro.getSaldoPendiente() + ")";
        redirectAttributes.addFlashAttribute("success",
                "¡" + persona.get().getNombreCompleto() + " registrado exitosamente" + detalle + "!");
        return "redirect:/crud";
    }

    @GetMapping("/crud")
    public String crud(Model model) {
        java.util.List<Registro> registros = registroService.findAll();
        int totalRecaudado = registros.stream().mapToInt(Registro::getMontoPagado).sum();
        int totalSaldoPendiente = registros.stream().mapToInt(Registro::getSaldoPendiente).sum();
        long cantCompletos = registros.stream().filter(Registro::isPagoCompleto).count();
        long cantAdelantos = registros.stream().filter(r -> !r.isPagoCompleto()).count();

        long cantJuevesSabado = registros.stream().filter(r -> r.getModalidad() == Registro.Modalidad.JUEVES_SABADO)
                .count();
        long cantMiercolesSabado = registros.stream()
                .filter(r -> r.getModalidad() == Registro.Modalidad.MIERCOLES_SABADO).count();
        long cantSoloViernes = registros.stream().filter(r -> r.getModalidad() == Registro.Modalidad.SOLO_VIERNES)
                .count();

        int totalYapePlin = registros.stream().filter(r -> r.getMedioPago() == Registro.MedioPago.YAPE_PLIN)
                .mapToInt(Registro::getMontoPagado).sum();
        int totalEfectivo = registros.stream().filter(r -> r.getMedioPago() == Registro.MedioPago.EFECTIVO)
                .mapToInt(Registro::getMontoPagado).sum();
        long cantYapePlin = registros.stream().filter(r -> r.getMedioPago() == Registro.MedioPago.YAPE_PLIN).count();
        long cantEfectivo = registros.stream().filter(r -> r.getMedioPago() == Registro.MedioPago.EFECTIVO).count();

        model.addAttribute("registros", registros);
        model.addAttribute("totalRecaudado", totalRecaudado);
        model.addAttribute("totalSaldoPendiente", totalSaldoPendiente);
        model.addAttribute("cantCompletos", cantCompletos);
        model.addAttribute("cantAdelantos", cantAdelantos);
        model.addAttribute("cantJuevesSabado", cantJuevesSabado);
        model.addAttribute("cantMiercolesSabado", cantMiercolesSabado);
        model.addAttribute("cantSoloViernes", cantSoloViernes);
        model.addAttribute("totalYapePlin", totalYapePlin);
        model.addAttribute("totalEfectivo", totalEfectivo);
        model.addAttribute("cantYapePlin", cantYapePlin);
        model.addAttribute("cantEfectivo", cantEfectivo);
        model.addAttribute("mediosPago", Registro.MedioPago.values());
        return "crud";
    }

    @PostMapping("/crud/completar-pago/{id}")
    public String completarPago(@PathVariable Long id,
            @RequestParam(required = false) Registro.MedioPago medioPago,
            RedirectAttributes redirectAttributes) {
        Optional<Registro> registroOpt = registroService.findById(id);
        if (registroOpt.isPresent()) {
            Registro r = registroOpt.get();
            registroService.completarPago(id, medioPago != null ? medioPago : r.getMedioPago());
            redirectAttributes.addFlashAttribute("success", "¡Pago de " + r.getPersona().getNombreCompleto()
                    + " actualizado a PAGO COMPLETO (S/ " + r.getModalidad().getPrecio() + ")!");
        }
        return "redirect:/crud";
    }

    @GetMapping("/crud/editar/{id}")
    public String editarForm(@PathVariable Long id, Model model) {
        Optional<Registro> registro = registroService.findById(id);
        if (registro.isEmpty())
            return "redirect:/crud";
        model.addAttribute("registro", registro.get());
        model.addAttribute("modalidades", Registro.Modalidad.values());
        model.addAttribute("mediosPago", Registro.MedioPago.values());
        model.addAttribute("estadosPago", Registro.EstadoPago.values());
        return "editar";
    }

    @PostMapping("/crud/editar/{id}")
    public String editar(@PathVariable Long id,
            @RequestParam Registro.Modalidad modalidad,
            @RequestParam Registro.MedioPago medioPago,
            @RequestParam(defaultValue = "TOTAL") String tipoPago,
            @RequestParam(required = false) Integer montoPagado,
            RedirectAttributes redirectAttributes) {
        Optional<Registro> registroOpt = registroService.findById(id);
        if (registroOpt.isEmpty())
            return "redirect:/crud";

        Registro registro = registroOpt.get();
        registro.setModalidad(modalidad);
        registro.setMedioPago(medioPago);

        int precioTotal = modalidad.getPrecio();
        if ("TOTAL".equalsIgnoreCase(tipoPago)) {
            registro.setMontoPagado(precioTotal);
        } else if (montoPagado != null && montoPagado > 0) {
            registro.setMontoPagado(Math.min(montoPagado, precioTotal));
        }

        registroService.save(registro);

        redirectAttributes.addFlashAttribute("success",
                "Registro de " + registro.getPersona().getNombreCompleto() + " actualizado correctamente.");
        return "redirect:/crud";
    }

    @PostMapping("/crud/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        registroService.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Registro eliminado.");
        return "redirect:/crud";
    }
}

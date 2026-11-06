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

import java.time.LocalDateTime;
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
        model.addAttribute("iglesias", Registro.Iglesia.values());
        model.addAttribute("modalidades", Registro.Modalidad.values());
        model.addAttribute("mediosPago", Registro.MedioPago.values());
        return "registro";
    }

    @PostMapping("/registro")
    public String registrar(@RequestParam(defaultValue = "CANTO_GRANDE") Registro.Iglesia iglesia,
                            @RequestParam Long personaId,
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
        registro.setIglesia(iglesia);
        registro.setModalidad(modalidad);
        registro.setMedioPago(medioPago);

        int precioTotal = modalidad.getPrecio();
        int inicial = ("ADELANTO".equalsIgnoreCase(tipoPago) && montoAdelanto != null && montoAdelanto > 0)
                ? Math.min(montoAdelanto, precioTotal)
                : precioTotal;

        LocalDateTime ahora = LocalDateTime.now();
        registro.setMontoPago1(inicial);
        registro.setMedioPago1(medioPago);
        registro.setFechaPago1(ahora);
        registro.setMontoPagado(inicial);
        registro.recalcularMontoYEstado();

        registroService.save(registro);

        String detalle = registro.isPagoCompleto()
                ? " (Pago Completo)"
                : " (1° Pago / Adelanto S/ " + registro.getMontoPagado() + ", Saldo: S/ " + registro.getSaldoPendiente() + ")";
        redirectAttributes.addFlashAttribute("success",
                "¡" + persona.get().getNombreCompleto() + " registrado exitosamente en " + iglesia.getDisplayName() + detalle + "!");
        return "redirect:/crud";
    }

    @GetMapping("/crud")
    public String crud(Model model) {
        java.util.List<Registro> registros = registroService.findAll();
        int totalRecaudado = registros.stream().mapToInt(Registro::getMontoPagado).sum();
        int totalSaldoPendiente = registros.stream().mapToInt(Registro::getSaldoPendiente).sum();
        long cantCompletos = registros.stream().filter(Registro::isPagoCompleto).count();
        long cantAdelantos = registros.stream().filter(r -> !r.isPagoCompleto()).count();

        long cantJuevesSabado = registros.stream().filter(r -> r.getModalidad() == Registro.Modalidad.JUEVES_SABADO).count();
        long cantMiercolesSabado = registros.stream().filter(r -> r.getModalidad() == Registro.Modalidad.MIERCOLES_SABADO).count();
        long cantSoloViernes = registros.stream().filter(r -> r.getModalidad() == Registro.Modalidad.SOLO_VIERNES).count();

        int totalYapePlin = registros.stream().filter(r -> r.getMedioPago() == Registro.MedioPago.YAPE_PLIN).mapToInt(Registro::getMontoPagado).sum();
        int totalEfectivo = registros.stream().filter(r -> r.getMedioPago() == Registro.MedioPago.EFECTIVO).mapToInt(Registro::getMontoPagado).sum();
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
        model.addAttribute("iglesias", Registro.Iglesia.values());
        return "crud";
    }

    @PostMapping("/crud/abonar/{id}")
    public String registrarAbono(@PathVariable Long id,
                                 @RequestParam int montoAbono,
                                 @RequestParam Registro.MedioPago medioPago,
                                 RedirectAttributes redirectAttributes) {
        Optional<Registro> registroOpt = registroService.findById(id);
        if (registroOpt.isPresent()) {
            Registro r = registroOpt.get();
            int sigPago = r.getSiguienteNumeroPago();
            if (sigPago == 0) {
                redirectAttributes.addFlashAttribute("error", "Ya se registraron los 3 pagos permitidos para este asistente.");
                return "redirect:/crud";
            }
            if (montoAbono <= 0) {
                redirectAttributes.addFlashAttribute("error", "El monto del abono debe ser mayor a 0.");
                return "redirect:/crud";
            }
            registroService.registrarAbono(id, montoAbono, medioPago);
            redirectAttributes.addFlashAttribute("success",
                    "¡" + sigPago + "° Pago de S/ " + montoAbono + " (" + medioPago.getDisplayName() + ") registrado correctamente para " + r.getPersona().getNombreCompleto() + "!");
        }
        return "redirect:/crud";
    }

    @PostMapping("/crud/completar-pago/{id}")
    public String completarPago(@PathVariable Long id,
                                @RequestParam(required = false) Registro.MedioPago medioPago,
                                RedirectAttributes redirectAttributes) {
        Optional<Registro> registroOpt = registroService.findById(id);
        if (registroOpt.isPresent()) {
            Registro r = registroOpt.get();
            int saldo = r.getSaldoPendiente();
            registroService.completarPago(id, medioPago != null ? medioPago : r.getMedioPago());
            redirectAttributes.addFlashAttribute("success", "¡Pago de " + r.getPersona().getNombreCompleto()
                    + " actualizado a PAGO COMPLETO (S/ " + saldo + " cancelados)! Total: S/ " + r.getModalidad().getPrecio());
        }
        return "redirect:/crud";
    }

    @GetMapping("/crud/editar/{id}")
    public String editarForm(@PathVariable Long id, Model model) {
        Optional<Registro> registro = registroService.findById(id);
        if (registro.isEmpty())
            return "redirect:/crud";
        model.addAttribute("registro", registro.get());
        model.addAttribute("iglesias", Registro.Iglesia.values());
        model.addAttribute("modalidades", Registro.Modalidad.values());
        model.addAttribute("mediosPago", Registro.MedioPago.values());
        model.addAttribute("estadosPago", Registro.EstadoPago.values());
        return "editar";
    }

    @PostMapping("/crud/editar/{id}")
    public String editar(@PathVariable Long id,
                         @RequestParam(defaultValue = "CANTO_GRANDE") Registro.Iglesia iglesia,
                         @RequestParam Registro.Modalidad modalidad,
                         @RequestParam(required = false) Integer montoPago1,
                         @RequestParam(required = false) Registro.MedioPago medioPago1,
                         @RequestParam(required = false) Integer montoPago2,
                         @RequestParam(required = false) Registro.MedioPago medioPago2,
                         @RequestParam(required = false) Integer montoPago3,
                         @RequestParam(required = false) Registro.MedioPago medioPago3,
                         RedirectAttributes redirectAttributes) {
        Optional<Registro> registroOpt = registroService.findById(id);
        if (registroOpt.isEmpty())
            return "redirect:/crud";

        Registro registro = registroOpt.get();
        registro.setIglesia(iglesia);
        registro.setModalidad(modalidad);

        if (montoPago1 != null && montoPago1 > 0) {
            registro.setMontoPago1(montoPago1);
            if (medioPago1 != null) registro.setMedioPago1(medioPago1);
        }
        registro.setMontoPago2(montoPago2 != null && montoPago2 > 0 ? montoPago2 : null);
        registro.setMedioPago2(montoPago2 != null && montoPago2 > 0 ? medioPago2 : null);

        registro.setMontoPago3(montoPago3 != null && montoPago3 > 0 ? montoPago3 : null);
        registro.setMedioPago3(montoPago3 != null && montoPago3 > 0 ? medioPago3 : null);

        registro.recalcularMontoYEstado();
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

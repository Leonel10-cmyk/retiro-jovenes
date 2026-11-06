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

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/personas")
public class PersonaWebController {

    @Autowired
    private PersonaService personaService;

    @Autowired
    private RegistroService registroService;

    @GetMapping
    public String listar(Model model) {
        List<Persona> lista = personaService.findAll();

        long totalMiembros = lista.stream().filter(p -> p.getRol() == Persona.Rol.MIEMBRO).count();
        long totalAspirantes = lista.stream().filter(p -> p.getRol() == Persona.Rol.ASPIRANTE).count();
        long totalLideres = lista.stream().filter(p -> p.getRol() == Persona.Rol.LIDER_CAPITAN 
                || p.getRol() == Persona.Rol.LIDER_ALTERNO 
                || p.getRol() == Persona.Rol.DIRECTIVO).count();

        model.addAttribute("personas", lista);
        model.addAttribute("roles", Persona.Rol.values());
        model.addAttribute("iglesias", Registro.Iglesia.values());
        model.addAttribute("totalPersonas", lista.size());
        model.addAttribute("totalMiembros", totalMiembros);
        model.addAttribute("totalAspirantes", totalAspirantes);
        model.addAttribute("totalLideres", totalLideres);
        return "personas";
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam(required = false) Long id,
                          @RequestParam String nombres,
                          @RequestParam(required = false, defaultValue = "") String apellidos,
                          @RequestParam(defaultValue = "CANTO_GRANDE") Registro.Iglesia iglesia,
                          @RequestParam Persona.Rol rol,
                          @RequestParam Integer numeroGrupo,
                          RedirectAttributes redirectAttributes) {
        Persona persona;
        if (id != null) {
            Optional<Persona> opt = personaService.findById(id);
            if (opt.isPresent()) {
                persona = opt.get();
            } else {
                persona = new Persona();
            }
        } else {
            persona = new Persona();
        }

        persona.setNombres(nombres.trim());
        persona.setApellidos(apellidos != null ? apellidos.trim() : "");
        persona.setIglesia(iglesia != null ? iglesia : Registro.Iglesia.CANTO_GRANDE);
        persona.setRol(rol);
        persona.setNumeroGrupo(numeroGrupo);
        personaService.save(persona);

        redirectAttributes.addFlashAttribute("success", "Datos de " + persona.getNombreCompleto() + " (" + persona.getIglesia().getDisplayName() + ") guardados correctamente.");
        return "redirect:/personas";
    }

    @GetMapping("/editar/{id}")
    public String editarForm(@PathVariable Long id, Model model) {
        Optional<Persona> persona = personaService.findById(id);
        if (persona.isEmpty()) {
            return "redirect:/personas";
        }
        model.addAttribute("persona", persona.get());
        model.addAttribute("roles", Persona.Rol.values());
        model.addAttribute("iglesias", Registro.Iglesia.values());
        return "persona-editar";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (registroService.yaRegistrado(id)) {
            redirectAttributes.addFlashAttribute("error", "No se puede eliminar a este joven porque ya está registrado en el retiro. Primero elimina su registro en la sección 'Ver Registros'.");
            return "redirect:/personas";
        }

        Optional<Persona> persona = personaService.findById(id);
        String nombre = persona.map(Persona::getNombreCompleto).orElse("el joven");
        personaService.deleteById(id);
        redirectAttributes.addFlashAttribute("success", nombre + " ha sido eliminado del padrón.");
        return "redirect:/personas";
    }
}


package com.retiro.jovenes.controller;

import com.retiro.jovenes.model.Persona;
import com.retiro.jovenes.service.PersonaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/personas")
public class PersonaController {

    @Autowired
    private PersonaService personaService;

    private Map<String, Object> personaToMap(Persona p) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", p.getId());
        map.put("nombres", p.getNombres());
        map.put("apellidos", p.getApellidos());
        map.put("nombreCompleto", p.getNombreCompleto());
        map.put("rol", p.getRol().name());
        map.put("rolDisplay", p.getRol().getDisplayName());
        map.put("numeroGrupo", p.getNumeroGrupo());
        return map;
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<Map<String, Object>>> buscar(@RequestParam String q) {
        List<Persona> personas = personaService.buscar(q);
        List<Map<String, Object>> result = personas.stream()
                .map(this::personaToMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        return personaService.findById(id)
                .map(p -> ResponseEntity.ok(personaToMap(p)))
                .orElse(ResponseEntity.notFound().build());
    }
}

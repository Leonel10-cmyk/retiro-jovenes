package com.retiro.jovenes.service;

import com.retiro.jovenes.model.Persona;
import com.retiro.jovenes.repository.PersonaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PersonaService {

    @Autowired
    private PersonaRepository personaRepository;

    public List<Persona> buscar(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return personaRepository.buscarPorNombreOApellido(query.trim());
    }

    public Optional<Persona> findById(Long id) {
        return personaRepository.findById(id);
    }

    public List<Persona> findAll() {
        return personaRepository.findAllByOrderByNumeroGrupoAscNombresAsc();
    }

    public Persona save(Persona persona) {
        return personaRepository.save(persona);
    }

    public void deleteById(Long id) {
        personaRepository.deleteById(id);
    }
}

package com.retiro.jovenes.service;

import com.retiro.jovenes.model.Registro;
import com.retiro.jovenes.repository.RegistroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RegistroService {

    @Autowired
    private RegistroRepository registroRepository;

    public List<Registro> findAll() {
        return registroRepository.findAllWithPersona();
    }

    public Optional<Registro> findById(Long id) {
        return registroRepository.findById(id);
    }

    public Registro save(Registro registro) {
        int precioTotal = registro.getModalidad().getPrecio();
        if (registro.getMontoPagado() == null || registro.getMontoPagado() >= precioTotal) {
            registro.setMontoPagado(precioTotal);
            registro.setEstadoPago(Registro.EstadoPago.COMPLETO);
        } else {
            registro.setEstadoPago(Registro.EstadoPago.ADELANTO);
        }
        return registroRepository.save(registro);
    }

    public void completarPago(Long id, Registro.MedioPago nuevoMedioPago) {
        findById(id).ifPresent(r -> {
            r.setMontoPagado(r.getModalidad().getPrecio());
            r.setEstadoPago(Registro.EstadoPago.COMPLETO);
            if (nuevoMedioPago != null) {
                r.setMedioPago(nuevoMedioPago);
            }
            registroRepository.save(r);
        });
    }

    public void deleteById(Long id) {
        registroRepository.deleteById(id);
    }

    public boolean yaRegistrado(Long personaId) {
        return registroRepository.existsByPersonaId(personaId);
    }
}

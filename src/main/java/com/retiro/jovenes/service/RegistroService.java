package com.retiro.jovenes.service;

import com.retiro.jovenes.model.Registro;
import com.retiro.jovenes.repository.RegistroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
        // Asegurar consistencia del primer pago
        if (registro.getMontoPago1() == null && registro.getMontoPagado() != null) {
            registro.setMontoPago1(registro.getMontoPagado());
            registro.setMedioPago1(registro.getMedioPago());
            registro.setFechaPago1(registro.getFechaRegistro() != null ? registro.getFechaRegistro() : LocalDateTime.now());
        }
        registro.recalcularMontoYEstado();
        return registroRepository.save(registro);
    }

    public void registrarAbono(Long id, int montoAbono, Registro.MedioPago medioPago) {
        findById(id).ifPresent(r -> {
            int saldo = r.getSaldoPendiente();
            if (saldo <= 0) return;

            int montoReal = Math.min(montoAbono, saldo);
            LocalDateTime ahora = LocalDateTime.now();
            Registro.MedioPago mp = (medioPago != null) ? medioPago : r.getMedioPago();

            if (r.getMontoPago2() == null || r.getMontoPago2() <= 0) {
                r.setMontoPago2(montoReal);
                r.setMedioPago2(mp);
                r.setFechaPago2(ahora);
            } else if (r.getMontoPago3() == null || r.getMontoPago3() <= 0) {
                r.setMontoPago3(montoReal);
                r.setMedioPago3(mp);
                r.setFechaPago3(ahora);
            } else {
                // Si ya tenía 3 abonos registrados pero quedaba saldo residual
                r.setMontoPago3(r.getMontoPago3() + montoReal);
                r.setFechaPago3(ahora);
            }

            // Actualizar último medio de pago usado
            r.setMedioPago(mp);
            r.recalcularMontoYEstado();
            registroRepository.save(r);
        });
    }

    public void completarPago(Long id, Registro.MedioPago nuevoMedioPago) {
        findById(id).ifPresent(r -> {
            int saldo = r.getSaldoPendiente();
            if (saldo <= 0) return;

            LocalDateTime ahora = LocalDateTime.now();
            Registro.MedioPago mp = (nuevoMedioPago != null) ? nuevoMedioPago : r.getMedioPago();

            if (r.getMontoPago2() == null || r.getMontoPago2() <= 0) {
                r.setMontoPago2(saldo);
                r.setMedioPago2(mp);
                r.setFechaPago2(ahora);
            } else if (r.getMontoPago3() == null || r.getMontoPago3() <= 0) {
                r.setMontoPago3(saldo);
                r.setMedioPago3(mp);
                r.setFechaPago3(ahora);
            } else {
                r.setMontoPago3((r.getMontoPago3() != null ? r.getMontoPago3() : 0) + saldo);
                r.setFechaPago3(ahora);
            }

            r.setMedioPago(mp);
            r.recalcularMontoYEstado();
            registroRepository.save(r);
        });
    }

    public void deleteById(Long id) {
        // Soft Delete: Protección contra eliminación accidental o intencional
        findById(id).ifPresent(r -> {
            r.setEliminado(true);
            r.setFechaEliminacion(LocalDateTime.now());
            registroRepository.save(r);
        });
    }

    public void restaurar(Long id) {
        findById(id).ifPresent(r -> {
            r.setEliminado(false);
            r.setFechaEliminacion(null);
            registroRepository.save(r);
        });
    }

    public List<Registro> findEliminados() {
        return registroRepository.findEliminadosWithPersona();
    }

    public boolean yaRegistrado(Long personaId) {
        return registroRepository.existsByPersonaIdAndEliminadoFalse(personaId);
    }
}

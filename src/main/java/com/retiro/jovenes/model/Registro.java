package com.retiro.jovenes.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "registro")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Registro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad", nullable = false)
    private Modalidad modalidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", nullable = false)
    private MedioPago medioPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false)
    private EstadoPago estadoPago;

    @Column(name = "monto_pagado", nullable = false)
    private Integer montoPagado;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @PrePersist
    protected void onCreate() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
        if (estadoPago == null) {
            estadoPago = (montoPagado != null && modalidad != null && montoPagado >= modalidad.getPrecio())
                    ? EstadoPago.COMPLETO : EstadoPago.ADELANTO;
        }
    }

    public int getMontoTotal() {
        return modalidad != null ? modalidad.getPrecio() : 0;
    }

    public int getSaldoPendiente() {
        int pagado = (montoPagado != null) ? montoPagado : 0;
        return Math.max(0, getMontoTotal() - pagado);
    }

    public boolean isPagoCompleto() {
        return estadoPago == EstadoPago.COMPLETO || getSaldoPendiente() == 0;
    }

    public enum Modalidad {
        JUEVES_SABADO("Jueves a Sábado", 70, "Incluye: Bus, estadía y comida"),
        MIERCOLES_SABADO("Miércoles a Sábado", 80, "Incluye: Bus, estadía y comida"),
        SOLO_VIERNES("Solo Viernes", 50, "Incluye: Estadía, comida y movilidad de regreso");

        private final String displayName;
        private final int precio;
        private final String descripcion;

        Modalidad(String displayName, int precio, String descripcion) {
            this.displayName = displayName;
            this.precio = precio;
            this.descripcion = descripcion;
        }

        public String getDisplayName() { return displayName; }
        public int getPrecio() { return precio; }
        public String getDescripcion() { return descripcion; }
    }

    public enum MedioPago {
        YAPE_PLIN("Yape / Plin", "📱"),
        EFECTIVO("Efectivo", "💵");

        private final String displayName;
        private final String icono;

        MedioPago(String displayName, String icono) {
            this.displayName = displayName;
            this.icono = icono;
        }

        public String getDisplayName() { return displayName; }
        public String getIcono() { return icono; }
    }

    public enum EstadoPago {
        COMPLETO("Pago Completo", "✅"),
        ADELANTO("Adelanto", "⏳");

        private final String displayName;
        private final String icono;

        EstadoPago(String displayName, String icono) {
            this.displayName = displayName;
            this.icono = icono;
        }

        public String getDisplayName() { return displayName; }
        public String getIcono() { return icono; }
    }
}

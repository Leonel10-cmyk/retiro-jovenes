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
    @Column(name = "iglesia", nullable = false)
    private Iglesia iglesia = Iglesia.CANTO_GRANDE;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad", nullable = false)
    private Modalidad modalidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", nullable = false)
    private MedioPago medioPago = MedioPago.YAPE_PLIN;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false)
    private EstadoPago estadoPago = EstadoPago.ADELANTO;

    @Column(name = "monto_pagado", nullable = false)
    private Integer montoPagado = 0;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    // ============ HISTORIAL DE HASTA 3 PAGOS ============
    @Column(name = "monto_pago1")
    private Integer montoPago1;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago1")
    private MedioPago medioPago1;

    @Column(name = "fecha_pago1")
    private LocalDateTime fechaPago1;

    @Column(name = "monto_pago2")
    private Integer montoPago2;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago2")
    private MedioPago medioPago2;

    @Column(name = "fecha_pago2")
    private LocalDateTime fechaPago2;

    @Column(name = "monto_pago3")
    private Integer montoPago3;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago3")
    private MedioPago medioPago3;

    @Column(name = "fecha_pago3")
    private LocalDateTime fechaPago3;

    @PrePersist
    protected void onCreate() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
        if (fechaPago1 == null && montoPago1 != null && montoPago1 > 0) {
            fechaPago1 = fechaRegistro;
        }
        if (medioPago1 == null) {
            medioPago1 = medioPago;
        }
        recalcularMontoYEstado();
    }

    @PreUpdate
    protected void onUpdate() {
        recalcularMontoYEstado();
    }

    public int calcularMontoPagado() {
        int total = 0;
        if (montoPago1 != null && montoPago1 > 0) total += montoPago1;
        if (montoPago2 != null && montoPago2 > 0) total += montoPago2;
        if (montoPago3 != null && montoPago3 > 0) total += montoPago3;

        // Si los campos individuales están vacíos pero montoPagado tiene valor previo
        if (total == 0 && montoPagado != null && montoPagado > 0) {
            return montoPagado;
        }
        return total;
    }

    public void recalcularMontoYEstado() {
        this.montoPagado = calcularMontoPagado();
        int precioTotal = getMontoTotal();
        if (precioTotal > 0 && this.montoPagado >= precioTotal) {
            this.estadoPago = EstadoPago.COMPLETO;
        } else {
            this.estadoPago = EstadoPago.ADELANTO;
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

    public int getNumeroPagosRealizados() {
        int count = 0;
        if (montoPago1 != null && montoPago1 > 0) count++;
        if (montoPago2 != null && montoPago2 > 0) count++;
        if (montoPago3 != null && montoPago3 > 0) count++;
        return (count == 0 && montoPagado != null && montoPagado > 0) ? 1 : count;
    }

    public boolean puedeAgregarPago() {
        return !isPagoCompleto() && (montoPago3 == null || montoPago3 <= 0);
    }

    public int getSiguienteNumeroPago() {
        if (montoPago1 == null || montoPago1 <= 0) return 1;
        if (montoPago2 == null || montoPago2 <= 0) return 2;
        if (montoPago3 == null || montoPago3 <= 0) return 3;
        return 0; // Ya se completaron los 3 abonos
    }

    // ============ ENUMS ============

    public enum Iglesia {
        CANTO_GRANDE("Iglesia Canto Grande"),
        CANTO_REY("Iglesia Canto Rey"),
        CANTO_CHICO("Iglesia Canto Chico"),
        LAS_CASUARINAS("Iglesia Las Casuarinas"),
        BELEN("Iglesia Belén"),
        AYACUCHO("Iglesia Ayacucho"),
        SAN_FERNANDO("Iglesia San Fernando");

        private final String displayName;

        Iglesia(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }
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

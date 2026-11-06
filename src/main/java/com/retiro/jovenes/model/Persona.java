package com.retiro.jovenes.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "persona")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombres;

    @Column(nullable = false)
    private String apellidos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Column(name = "numero_grupo", nullable = false)
    private Integer numeroGrupo;

    @Enumerated(EnumType.STRING)
    @Column(name = "iglesia", nullable = false, columnDefinition = "varchar(50) default 'CANTO_GRANDE'")
    private Registro.Iglesia iglesia = Registro.Iglesia.CANTO_GRANDE;

    public enum Rol {
        LIDER_CAPITAN("Líder Capitán"),
        LIDER_ALTERNO("Líder Alterno"),
        DIRECTIVO("Directivo"),
        MIEMBRO("Miembro"),
        ASPIRANTE("Aspirante");

        private final String displayName;

        Rol(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public String getNombreCompleto() {
        if (apellidos == null || apellidos.trim().isEmpty()) {
            return nombres != null ? nombres.trim() : "";
        }
        return nombres.trim() + " " + apellidos.trim();
    }

    public String getIniciales() {
        String ini = "";
        if (nombres != null && !nombres.trim().isEmpty()) {
            ini += nombres.trim().charAt(0);
        }
        if (apellidos != null && !apellidos.trim().isEmpty()) {
            ini += apellidos.trim().charAt(0);
        }
        return ini.toUpperCase();
    }
}

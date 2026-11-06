package com.retiro.jovenes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.retiro.jovenes.model.Persona;
import com.retiro.jovenes.model.Registro;
import com.retiro.jovenes.repository.PersonaRepository;
import com.retiro.jovenes.repository.RegistroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/admin/backup")
public class BackupController {

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private RegistroRepository registroRepository;

    private final ObjectMapper objectMapper;

    public BackupController() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Descarga inmediata de un archivo JSON con el respaldo total del padrón y registros.
     */
    @GetMapping("/descargar")
    public ResponseEntity<byte[]> descargarBackup() {
        try {
            List<Persona> personas = personaRepository.findAll();
            List<Registro> registros = registroRepository.findAll();

            Map<String, Object> backupData = new LinkedHashMap<>();
            backupData.put("sistema", "Retiro Presbiterial de Jóvenes 2026");
            backupData.put("fechaExportacion", LocalDateTime.now().toString());
            backupData.put("totalPersonas", personas.size());
            backupData.put("totalRegistros", registros.size());
            backupData.put("personas", personas);

            // Mapear registros con referencia al personaId
            List<Map<String, Object>> registrosMapeados = new ArrayList<>();
            for (Registro r : registros) {
                Map<String, Object> rm = new LinkedHashMap<>();
                rm.put("id", r.getId());
                rm.put("personaId", r.getPersona() != null ? r.getPersona().getId() : null);
                rm.put("iglesia", r.getIglesia() != null ? r.getIglesia().name() : "CANTO_GRANDE");
                rm.put("modalidad", r.getModalidad() != null ? r.getModalidad().name() : null);
                rm.put("medioPago", r.getMedioPago() != null ? r.getMedioPago().name() : null);
                rm.put("estadoPago", r.getEstadoPago() != null ? r.getEstadoPago().name() : null);
                rm.put("montoPagado", r.getMontoPagado());
                rm.put("fechaRegistro", r.getFechaRegistro() != null ? r.getFechaRegistro().toString() : null);

                rm.put("montoPago1", r.getMontoPago1());
                rm.put("medioPago1", r.getMedioPago1() != null ? r.getMedioPago1().name() : null);
                rm.put("fechaPago1", r.getFechaPago1() != null ? r.getFechaPago1().toString() : null);

                rm.put("montoPago2", r.getMontoPago2());
                rm.put("medioPago2", r.getMedioPago2() != null ? r.getMedioPago2().name() : null);
                rm.put("fechaPago2", r.getFechaPago2() != null ? r.getFechaPago2().toString() : null);

                rm.put("montoPago3", r.getMontoPago3());
                rm.put("medioPago3", r.getMedioPago3() != null ? r.getMedioPago3().name() : null);
                rm.put("fechaPago3", r.getFechaPago3() != null ? r.getFechaPago3().toString() : null);

                rm.put("eliminado", r.isEliminado());
                rm.put("fechaEliminacion", r.getFechaEliminacion() != null ? r.getFechaEliminacion().toString() : null);

                registrosMapeados.add(rm);
            }
            backupData.put("registros", registrosMapeados);

            byte[] jsonBytes = objectMapper.writeValueAsBytes(backupData);
            String fechaStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
            String filename = "respaldo_retiro_2026_" + fechaStr + ".json";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(jsonBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Restauración a partir de un archivo JSON subido por el administrador.
     */
    @PostMapping("/restaurar")
    public String restaurarBackup(@RequestParam("archivo") MultipartFile file, RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Por favor selecciona un archivo JSON válido de respaldo.");
            return "redirect:/crud";
        }

        try (InputStream is = file.getInputStream()) {
            Map<?, ?> data = objectMapper.readValue(is, Map.class);
            List<?> personasList = (List<?>) data.get("personas");
            List<?> registrosList = (List<?>) data.get("registros");

            int personasRestauradas = 0;
            int registrosRestaurados = 0;

            // 1. Restaurar personas
            if (personasList != null) {
                for (Object item : personasList) {
                    Map<?, ?> pm = (Map<?, ?>) item;
                    Long id = pm.get("id") != null ? Long.valueOf(pm.get("id").toString()) : null;
                    if (id != null) {
                        Persona p = personaRepository.findById(id).orElse(new Persona());
                        p.setId(id);
                        p.setNombres((String) pm.get("nombres"));
                        p.setApellidos((String) pm.get("apellidos"));
                        if (pm.get("numeroGrupo") != null) {
                            p.setNumeroGrupo(Integer.valueOf(pm.get("numeroGrupo").toString()));
                        }
                        if (pm.get("rol") != null) {
                            p.setRol(Persona.Rol.valueOf((String) pm.get("rol")));
                        }
                        if (pm.get("iglesia") != null) {
                            p.setIglesia(Registro.Iglesia.valueOf((String) pm.get("iglesia")));
                        } else {
                            p.setIglesia(Registro.Iglesia.CANTO_GRANDE);
                        }
                        personaRepository.save(p);
                        personasRestauradas++;
                    }
                }
            }

            // 2. Restaurar registros
            if (registrosList != null) {
                for (Object item : registrosList) {
                    Map<?, ?> rm = (Map<?, ?>) item;
                    Long id = rm.get("id") != null ? Long.valueOf(rm.get("id").toString()) : null;
                    Long personaId = rm.get("personaId") != null ? Long.valueOf(rm.get("personaId").toString()) : null;

                    if (personaId != null) {
                        Optional<Persona> pOpt = personaRepository.findById(personaId);
                        if (pOpt.isPresent()) {
                            Registro r = (id != null) ? registroRepository.findById(id).orElse(new Registro()) : new Registro();
                            if (id != null) r.setId(id);
                            r.setPersona(pOpt.get());

                            if (rm.get("iglesia") != null) {
                                r.setIglesia(Registro.Iglesia.valueOf((String) rm.get("iglesia")));
                            }
                            if (rm.get("modalidad") != null) {
                                r.setModalidad(Registro.Modalidad.valueOf((String) rm.get("modalidad")));
                            }
                            if (rm.get("medioPago") != null) {
                                r.setMedioPago(Registro.MedioPago.valueOf((String) rm.get("medioPago")));
                            }
                            if (rm.get("estadoPago") != null) {
                                r.setEstadoPago(Registro.EstadoPago.valueOf((String) rm.get("estadoPago")));
                            }
                            if (rm.get("montoPagado") != null) {
                                r.setMontoPagado(Integer.valueOf(rm.get("montoPagado").toString()));
                            }
                            if (rm.get("fechaRegistro") != null) {
                                r.setFechaRegistro(LocalDateTime.parse((String) rm.get("fechaRegistro")));
                            }

                            if (rm.get("montoPago1") != null) r.setMontoPago1(Integer.valueOf(rm.get("montoPago1").toString()));
                            if (rm.get("medioPago1") != null) r.setMedioPago1(Registro.MedioPago.valueOf((String) rm.get("medioPago1")));
                            if (rm.get("fechaPago1") != null) r.setFechaPago1(LocalDateTime.parse((String) rm.get("fechaPago1")));

                            if (rm.get("montoPago2") != null) r.setMontoPago2(Integer.valueOf(rm.get("montoPago2").toString()));
                            if (rm.get("medioPago2") != null) r.setMedioPago2(Registro.MedioPago.valueOf((String) rm.get("medioPago2")));
                            if (rm.get("fechaPago2") != null) r.setFechaPago2(LocalDateTime.parse((String) rm.get("fechaPago2")));

                            if (rm.get("montoPago3") != null) r.setMontoPago3(Integer.valueOf(rm.get("montoPago3").toString()));
                            if (rm.get("medioPago3") != null) r.setMedioPago3(Registro.MedioPago.valueOf((String) rm.get("medioPago3")));
                            if (rm.get("fechaPago3") != null) r.setFechaPago3(LocalDateTime.parse((String) rm.get("fechaPago3")));

                            if (rm.get("eliminado") != null) {
                                r.setEliminado(Boolean.parseBoolean(rm.get("eliminado").toString()));
                            }
                            if (rm.get("fechaEliminacion") != null) {
                                r.setFechaEliminacion(LocalDateTime.parse((String) rm.get("fechaEliminacion")));
                            }

                            r.recalcularMontoYEstado();
                            registroRepository.save(r);
                            registrosRestaurados++;
                        }
                    }
                }
            }

            redirectAttributes.addFlashAttribute("success",
                    "¡Copia de seguridad restaurada exitosamente! (" + personasRestauradas + " jóvenes y " + registrosRestaurados + " inscripciones actualizadas/recuperadas).");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al procesar el archivo de respaldo: " + e.getMessage());
        }

        return "redirect:/crud";
    }
}


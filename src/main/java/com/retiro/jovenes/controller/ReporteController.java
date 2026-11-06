package com.retiro.jovenes.controller;

import com.retiro.jovenes.model.Persona;
import com.retiro.jovenes.model.Registro;
import com.retiro.jovenes.repository.PersonaRepository;
import com.retiro.jovenes.repository.RegistroRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/reportes")
public class ReporteController {

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private RegistroRepository registroRepository;

    @GetMapping
    public String verReporte(@RequestParam(required = false) Integer grupo,
                             @RequestParam(required = false) Registro.Iglesia iglesia,
                             Model model) {
        List<Persona> todasPersonas = personaRepository.findAllByOrderByNumeroGrupoAscNombresAsc();
        List<Registro> todosRegistros = registroRepository.findAllWithPersona();

        // Lista de números de grupo disponibles
        List<Integer> grupos = todasPersonas.stream()
                .map(Persona::getNumeroGrupo)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // Filtrar SOLO personas que se han registrado por grupo y/o iglesia
        List<Registro> registrosFiltrados = todosRegistros.stream()
                .filter(r -> (grupo == null || grupo <= 0 || r.getPersona().getNumeroGrupo().equals(grupo)))
                .filter(r -> (iglesia == null || r.getIglesia() == iglesia))
                .collect(Collectors.toList());

        // Estadísticas sobre los registrados
        long totalRegistrados = registrosFiltrados.size();
        int totalRecaudado = registrosFiltrados.stream().mapToInt(Registro::getMontoPagado).sum();
        int totalSaldoPendiente = registrosFiltrados.stream().mapToInt(Registro::getSaldoPendiente).sum();
        long cantCompletos = registrosFiltrados.stream().filter(Registro::isPagoCompleto).count();
        long cantAdelantos = registrosFiltrados.stream().filter(r -> !r.isPagoCompleto()).count();

        long cantYapePlin = registrosFiltrados.stream().filter(r -> r.getMedioPago() == Registro.MedioPago.YAPE_PLIN).count();
        int totalYapePlin = registrosFiltrados.stream().filter(r -> r.getMedioPago() == Registro.MedioPago.YAPE_PLIN).mapToInt(Registro::getMontoPagado).sum();

        long cantEfectivo = registrosFiltrados.stream().filter(r -> r.getMedioPago() == Registro.MedioPago.EFECTIVO).count();
        int totalEfectivo = registrosFiltrados.stream().filter(r -> r.getMedioPago() == Registro.MedioPago.EFECTIVO).mapToInt(Registro::getMontoPagado).sum();

        long cantMiercolesSabado = registrosFiltrados.stream().filter(r -> r.getModalidad() == Registro.Modalidad.MIERCOLES_SABADO).count();
        long cantJuevesSabado = registrosFiltrados.stream().filter(r -> r.getModalidad() == Registro.Modalidad.JUEVES_SABADO).count();
        long cantSoloViernes = registrosFiltrados.stream().filter(r -> r.getModalidad() == Registro.Modalidad.SOLO_VIERNES).count();

        model.addAttribute("grupos", grupos);
        model.addAttribute("iglesias", Registro.Iglesia.values());
        model.addAttribute("grupoSeleccionado", grupo != null ? grupo : 0);
        model.addAttribute("iglesiaSeleccionada", iglesia);
        model.addAttribute("registros", registrosFiltrados);
        model.addAttribute("totalRegistrados", totalRegistrados);
        model.addAttribute("totalRecaudado", totalRecaudado);
        model.addAttribute("totalSaldoPendiente", totalSaldoPendiente);
        model.addAttribute("cantCompletos", cantCompletos);
        model.addAttribute("cantAdelantos", cantAdelantos);
        model.addAttribute("cantYapePlin", cantYapePlin);
        model.addAttribute("totalYapePlin", totalYapePlin);
        model.addAttribute("cantEfectivo", cantEfectivo);
        model.addAttribute("totalEfectivo", totalEfectivo);
        model.addAttribute("cantMiercolesSabado", cantMiercolesSabado);
        model.addAttribute("cantJuevesSabado", cantJuevesSabado);
        model.addAttribute("cantSoloViernes", cantSoloViernes);

        return "reportes";
    }

    @GetMapping("/exportar-csv")
    public void exportarCsv(@RequestParam(required = false) Integer grupo,
                            @RequestParam(required = false) Registro.Iglesia iglesia,
                            HttpServletResponse response) throws IOException {
        List<Registro> todosRegistros = registroRepository.findAllWithPersona();

        List<Registro> registrosFiltrados = todosRegistros.stream()
                .filter(r -> (grupo == null || grupo <= 0 || r.getPersona().getNumeroGrupo().equals(grupo)))
                .filter(r -> (iglesia == null || r.getIglesia() == iglesia))
                .collect(Collectors.toList());

        String nombreArchivo = "reporte_inscritos_retiro_2026.csv";
        if (grupo != null && grupo > 0) {
            nombreArchivo = "reporte_inscritos_grupo_" + grupo + ".csv";
        } else if (iglesia != null) {
            nombreArchivo = "reporte_inscritos_" + iglesia.name().toLowerCase() + ".csv";
        }

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"");

        PrintWriter writer = response.getWriter();
        // BOM for Excel UTF-8 compatibility
        writer.write('\uFEFF');
        writer.println("N°,Iglesia,Grupo,Nombres,Apellidos,Rol,Modalidad,1° Pago (S/),2° Pago (S/),3° Pago (S/),Total Pagado (S/),Saldo Pendiente (S/),Estado de Pago,Fecha de Registro");

        int index = 1;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (Registro r : registrosFiltrados) {
            Persona p = r.getPersona();
            String estadoPago = r.isPagoCompleto() ? "PAGO COMPLETO" : "ADELANTO (Resta S/ " + r.getSaldoPendiente() + ")";
            String modalidad = r.getModalidad().getDisplayName();
            String iglesiaNombre = (r.getIglesia() != null) ? r.getIglesia().getDisplayName() : "Canto Grande";
            int p1 = (r.getMontoPago1() != null) ? r.getMontoPago1() : (r.getMontoPagado() != null ? r.getMontoPagado() : 0);
            String p2 = (r.getMontoPago2() != null && r.getMontoPago2() > 0) ? String.valueOf(r.getMontoPago2()) : "-";
            String p3 = (r.getMontoPago3() != null && r.getMontoPago3() > 0) ? String.valueOf(r.getMontoPago3()) : "-";
            int total = r.getMontoPagado();
            int saldo = r.getSaldoPendiente();
            String fecha = (r.getFechaRegistro() != null) ? r.getFechaRegistro().format(dtf) : "-";

            writer.printf("%d,\"%s\",%d,\"%s\",\"%s\",\"%s\",\"%s\",%d,%s,%s,%d,%d,\"%s\",\"%s\"%n",
                    index++,
                    iglesiaNombre,
                    p.getNumeroGrupo(),
                    p.getNombres().replace("\"", "\"\""),
                    p.getApellidos().replace("\"", "\"\""),
                    p.getRol().getDisplayName(),
                    modalidad,
                    p1,
                    p2,
                    p3,
                    total,
                    saldo,
                    estadoPago,
                    fecha
            );
        }
        writer.flush();
    }
}

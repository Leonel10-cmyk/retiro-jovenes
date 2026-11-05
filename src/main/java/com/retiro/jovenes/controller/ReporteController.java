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
    public String verReporte(@RequestParam(required = false) Integer grupo, Model model) {
        List<Persona> todasPersonas = personaRepository.findAllByOrderByNumeroGrupoAscNombresAsc();
        List<Registro> todosRegistros = registroRepository.findAllWithPersona();

        // Lista de números de grupo disponibles
        List<Integer> grupos = todasPersonas.stream()
                .map(Persona::getNumeroGrupo)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // Filtrar SOLO personas que se han registrado (con pago completo o adelanto)
        List<Registro> registrosFiltrados = (grupo != null && grupo > 0)
                ? todosRegistros.stream().filter(r -> r.getPersona().getNumeroGrupo().equals(grupo)).collect(Collectors.toList())
                : todosRegistros;

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
        model.addAttribute("grupoSeleccionado", grupo != null ? grupo : 0);
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
    public void exportarCsv(@RequestParam(required = false) Integer grupo, HttpServletResponse response) throws IOException {
        List<Registro> todosRegistros = registroRepository.findAllWithPersona();

        List<Registro> registrosFiltrados = (grupo != null && grupo > 0)
                ? todosRegistros.stream().filter(r -> r.getPersona().getNumeroGrupo().equals(grupo)).collect(Collectors.toList())
                : todosRegistros;

        String nombreArchivo = (grupo != null && grupo > 0) ? "reporte_inscritos_grupo_" + grupo + ".csv" : "reporte_inscritos_general_retiro.csv";

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"");

        PrintWriter writer = response.getWriter();
        // BOM for Excel UTF-8 compatibility
        writer.write('\uFEFF');
        writer.println("N°,Grupo,Nombres,Apellidos,Rol,Modalidad,Medio de Pago,Monto Pagado (S/),Saldo Pendiente (S/),Estado de Pago,Fecha de Registro");

        int index = 1;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (Registro r : registrosFiltrados) {
            Persona p = r.getPersona();
            String estadoPago = r.isPagoCompleto() ? "PAGO COMPLETO" : "ADELANTO (Resta S/ " + r.getSaldoPendiente() + ")";
            String modalidad = r.getModalidad().getDisplayName();
            String medioPago = r.getMedioPago().getDisplayName();
            int monto = r.getMontoPagado();
            int saldo = r.getSaldoPendiente();
            String fecha = (r.getFechaRegistro() != null) ? r.getFechaRegistro().format(dtf) : "-";

            writer.printf("%d,%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d,%d,\"%s\",\"%s\"%n",
                    index++,
                    p.getNumeroGrupo(),
                    p.getNombres().replace("\"", "\"\""),
                    p.getApellidos().replace("\"", "\"\""),
                    p.getRol().getDisplayName(),
                    modalidad,
                    medioPago,
                    monto,
                    saldo,
                    estadoPago,
                    fecha
            );
        }
        writer.flush();
    }
}

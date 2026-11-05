package com.retiro.jovenes.repository;

import com.retiro.jovenes.model.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, Long> {

    @Query("SELECT p FROM Persona p WHERE " +
           "LOWER(p.nombres) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.apellidos) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(CONCAT(p.nombres, ' ', p.apellidos)) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Persona> buscarPorNombreOApellido(@Param("query") String query);

    List<Persona> findAllByOrderByNumeroGrupoAscNombresAsc();

    List<Persona> findByNumeroGrupoOrderByNombresAsc(Integer numeroGrupo);
}

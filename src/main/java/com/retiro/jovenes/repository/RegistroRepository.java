package com.retiro.jovenes.repository;

import com.retiro.jovenes.model.Registro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistroRepository extends JpaRepository<Registro, Long> {

    @Query("SELECT r FROM Registro r JOIN FETCH r.persona WHERE r.eliminado = false ORDER BY r.fechaRegistro DESC")
    List<Registro> findAllWithPersona();

    @Query("SELECT r FROM Registro r JOIN FETCH r.persona WHERE r.eliminado = true ORDER BY r.fechaEliminacion DESC")
    List<Registro> findEliminadosWithPersona();

    @Query("SELECT r FROM Registro r JOIN FETCH r.persona p WHERE r.eliminado = false AND p.numeroGrupo = :numeroGrupo ORDER BY r.fechaRegistro DESC")
    List<Registro> findByPersonaNumeroGrupo(@org.springframework.data.repository.query.Param("numeroGrupo") Integer numeroGrupo);

    @Query("SELECT COUNT(r) > 0 FROM Registro r WHERE r.persona.id = :personaId AND r.eliminado = false")
    boolean existsByPersonaIdAndEliminadoFalse(@org.springframework.data.repository.query.Param("personaId") Long personaId);

    boolean existsByPersonaId(Long personaId);
}

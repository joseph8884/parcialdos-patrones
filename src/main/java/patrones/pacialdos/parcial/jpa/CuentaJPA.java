package patrones.pacialdos.parcial.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import patrones.pacialdos.parcial.orm.CuentaORM;

import java.util.Optional;

@Repository
public interface CuentaJPA extends JpaRepository<CuentaORM, Long> {
    Optional<CuentaORM> findByNombre(String nombre);
}
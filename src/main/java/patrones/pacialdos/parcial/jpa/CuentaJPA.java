package patrones.pacialdos.parcial.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import patrones.pacialdos.parcial.orm.CuentaORM;
import jakarta.transaction.Transactional;


import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface CuentaJPA extends CrudRepository<CuentaORM, Long> {

    Optional<CuentaORM> findByNombre(String nombre);

    @Modifying
    @Transactional
    @Query("UPDATE CuentaORM c SET c.monto = c.monto - :monto WHERE c.nombre = :nombre AND c.monto >= :monto")
    int restarSaldo(@Param("nombre") String nombre, @Param("monto") BigDecimal monto);

    @Modifying
    @Transactional
    @Query("UPDATE CuentaORM c SET c.monto = c.monto + :monto WHERE c.nombre = :nombre")
    int sumarSaldo(@Param("nombre") String nombre, @Param("monto") BigDecimal monto);
}
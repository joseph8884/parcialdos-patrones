package patrones.pacialdos.parcial.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import patrones.pacialdos.parcial.orm.CuentaORM;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface CuentaJPA extends JpaRepository<CuentaORM, Long> {

    Optional<CuentaORM> findByNombre(String nombre);

    /**
     * Resta el monto especificado de la cuenta solo si tiene saldo suficiente.
     * Usa una query SQL directa para evitar condiciones de carrera.
     *
     * @param nombre Nombre de la cuenta
     * @param monto Monto a restar
     * @return Número de filas actualizadas (1 si tuvo éxito, 0 si no hay saldo suficiente)
     */
    @Modifying
    @Query(value = "UPDATE cuenta SET monto = monto - :monto WHERE nombre = :nombre AND monto >= :monto",
            nativeQuery = true)
    int restarSaldo(@Param("nombre") String nombre, @Param("monto") BigDecimal monto);

    /**
     * Suma el monto especificado a la cuenta.
     *
     * @param nombre Nombre de la cuenta
     * @param monto Monto a sumar
     * @return Número de filas actualizadas
     */
    @Modifying
    @Query(value = "UPDATE cuenta SET monto = monto + :monto WHERE nombre = :nombre",
            nativeQuery = true)
    int sumarSaldo(@Param("nombre") String nombre, @Param("monto") BigDecimal monto);

    /**
     * Obtiene el saldo actual de una cuenta (con bloqueo para lectura)
     *
     * @param nombre Nombre de la cuenta
     * @return El saldo actual
     */
    @Query(value = "SELECT monto FROM cuenta WHERE nombre = :nombre FOR UPDATE",
            nativeQuery = true)
    BigDecimal getSaldoWithLock(@Param("nombre") String nombre);
}
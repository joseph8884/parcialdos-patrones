package patrones.pacialdos.parcial.services;

import com.newrelic.api.agent.NewRelic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import patrones.pacialdos.parcial.jpa.CuentaJPA;
import patrones.pacialdos.parcial.jpa.TransaccionJPA;
import patrones.pacialdos.parcial.orm.CuentaORM;
import patrones.pacialdos.parcial.orm.TransaccionORM;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TransaccionService {
    private static final Logger logger = LoggerFactory.getLogger(TransaccionService.class);

    @Autowired
    private CuentaJPA cuentaJPA;

    @Autowired
    private TransaccionJPA transaccionJPA;

    // Crear un pool fijo de 30 hilos
    private final ExecutorService executorService = Executors.newFixedThreadPool(30);

    // Contador atómico para estadísticas
    private final AtomicInteger transaccionesExitosas = new AtomicInteger(0);

    @Transactional
    public void resetCuentas() {
        CuentaORM cuenta1 = cuentaJPA.findByNombre("abc").orElse(new CuentaORM(null, "abc", BigDecimal.ZERO));
        CuentaORM cuenta2 = cuentaJPA.findByNombre("cbd").orElse(new CuentaORM(null, "cbd", BigDecimal.ZERO));

        cuenta1.setMonto(new BigDecimal("10000"));
        cuenta2.setMonto(new BigDecimal("10000"));

        cuentaJPA.save(cuenta1);
        cuentaJPA.save(cuenta2);

        logger.info("Cuentas reseteadas: abc y cbd ahora tienen 10,000 cada una");
    }

    public void iniciarTransaccionesConcurrentes() {
        logger.info("Iniciando prueba de concurrencia con 30 hilos");

        // Reiniciar contador
        transaccionesExitosas.set(0);

        // Lanzar 30 hilos para realizar transferencias
        for (int i = 0; i < 30; i++) {
            executorService.execute(this::ejecutarTransferencias);
        }
    }

    private void ejecutarTransferencias() {
        try {
            while (true) {
                boolean resultado = transferirDinero();
                if (!resultado) {
                    // Si la transferencia falló porque ya no hay fondos, terminar
                    break;
                }

                // Pequeña pausa para reducir contención
                Thread.sleep(5);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Hilo interrumpido durante las transferencias");
        }
    }

    /**
     * Método que realiza la transferencia de dinero entre cuentas
     * @return true si la transferencia fue exitosa, false si no hay suficiente saldo
     */
    @Transactional
    public boolean transferirDinero() {
        try {
            // Obtener las cuentas
            Optional<CuentaORM> cuentaOrigenOpt = cuentaJPA.findByNombre("abc");
            Optional<CuentaORM> cuentaDestinoOpt = cuentaJPA.findByNombre("cbd");

            if (cuentaOrigenOpt.isEmpty() || cuentaDestinoOpt.isEmpty()) {
                logger.error("No se encontraron las cuentas");
                return false;
            }

            CuentaORM origen = cuentaOrigenOpt.get();
            CuentaORM destino = cuentaDestinoOpt.get();
            BigDecimal monto = new BigDecimal("5");

            // Verificar saldo suficiente
            if (origen.getMonto().compareTo(monto) < 0) {
                logger.info("La cuenta 'abc' se quedó sin saldo suficiente.");
                return false;
            }

            // Actualizar saldos
            origen.setMonto(origen.getMonto().subtract(monto));
            destino.setMonto(destino.getMonto().add(monto));

            // Guardar cuentas actualizadas
            cuentaJPA.save(origen);
            cuentaJPA.save(destino);

            // Crear transacción
            TransaccionORM transaccion = new TransaccionORM(
                    null,
                    origen.getId(),
                    destino.getId(),
                    monto,
                    LocalDateTime.now()
            );

            // Guardar la transacción
            transaccionJPA.save(transaccion);

            // Incrementar contador y enviar métricas a New Relic
            int totalExitosas = transaccionesExitosas.incrementAndGet();
            if (totalExitosas % 100 == 0) {
                logger.info("Se han completado {} transacciones exitosas", totalExitosas);
            }

            NewRelic.recordMetric("Custom/MontoTransferido", monto.floatValue());
            NewRelic.addCustomParameter("Origen", "abc");
            NewRelic.addCustomParameter("Destino", "cbd");
            NewRelic.addCustomParameter("Monto", 5);

            return true;

        } catch (Exception e) {
            logger.error("Error al realizar la transferencia: {}", e.getMessage(), e);
            return false;
        }
    }

    // Método para detener el servicio y liberar recursos
    public void detener() {
        executorService.shutdown();
    }
}
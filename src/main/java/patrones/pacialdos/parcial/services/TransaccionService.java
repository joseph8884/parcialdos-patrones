package patrones.pacialdos.parcial.services;

import com.newrelic.api.agent.NewRelic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import patrones.pacialdos.parcial.jpa.CuentaJPA;
import patrones.pacialdos.parcial.jpa.TransaccionJPA;
import patrones.pacialdos.parcial.orm.CuentaORM;
import patrones.pacialdos.parcial.orm.TransaccionORM;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class TransaccionService {
    private static final Logger logger = LoggerFactory.getLogger(TransaccionService.class);

    @Autowired
    private CuentaJPA cuentaJPA;

    @Autowired
    private TransaccionJPA transaccionJPA;

    @Autowired
    private DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    // Crear un pool fijo de 30 hilos
    private final ExecutorService executorService = Executors.newFixedThreadPool(30);

    // Control de finalización
    private final AtomicBoolean finTransferencias = new AtomicBoolean(false);

    // Contador atómico para estadísticas
    private final AtomicInteger transaccionesExitosas = new AtomicInteger(0);
    private final AtomicInteger transaccionesFallidas = new AtomicInteger(0);

    // Control global para la consistencia del saldo total
    private final ReentrantLock sumLock = new ReentrantLock();
    private BigDecimal totalSistema = new BigDecimal("20000"); // 10000 + 10000

    @Transactional
    public void resetCuentas() {
        // Reiniciar estado
        finTransferencias.set(false);
        transaccionesExitosas.set(0);
        transaccionesFallidas.set(0);

        // Resetear cuentas usando SQL directo para evitar problemas de concurrencia
        jdbcTemplate.update("UPDATE cuenta SET monto = ? WHERE nombre = ?",
                new BigDecimal("10000"), "abc");
        jdbcTemplate.update("UPDATE cuenta SET monto = ? WHERE nombre = ?",
                new BigDecimal("10000"), "cbd");

        // También actualizar cualquier entidad cacheada
        entityManager.clear();

        logger.info("Cuentas reseteadas: abc y cbd ahora tienen 10,000 cada una");
    }

    public void iniciarTransaccionesConcurrentes() {
        logger.info("Iniciando prueba de concurrencia con 30 hilos");

        // Reset inicial
        finTransferencias.set(false);
        transaccionesExitosas.set(0);
        transaccionesFallidas.set(0);

        // Lanzar 30 hilos para realizar transferencias
        for (int i = 0; i < 30; i++) {
            final int threadId = i;
            executorService.execute(() -> ejecutarTransferencias(threadId));
        }
    }

    private void ejecutarTransferencias(int threadId) {
        int intentos = 0;
        int exitosConsecutivos = 0;

        try {
            while (!finTransferencias.get()) {
                boolean resultado = transferirDinero();

                if (resultado) {
                    exitosConsecutivos++;
                    intentos = 0;

                    // Verificar periódicamente si ya no hay fondos en la cuenta origen
                    if (exitosConsecutivos % 10 == 0) {
                        BigDecimal saldoOrigen = getSaldoActual("abc");
                        if (saldoOrigen.compareTo(new BigDecimal("5")) < 0) {
                            logger.info("Hilo {} detectó que la cuenta abc está próxima a agotarse: {}",
                                    threadId, saldoOrigen);
                            finTransferencias.set(true);
                            break;
                        }
                    }
                } else {
                    intentos++;
                    // Si fallamos muchas veces consecutivas, verificamos si ya terminamos
                    if (intentos >= 5) {
                        BigDecimal saldoOrigen = getSaldoActual("abc");
                        if (saldoOrigen.compareTo(new BigDecimal("5")) < 0) {
                            logger.info("Hilo {} confirmó que no hay saldo suficiente después de {} intentos",
                                    threadId, intentos);
                            finTransferencias.set(true);
                            break;
                        }
                        // Reset intentos para no entrar constantemente aquí
                        intentos = 0;
                    }

                    // Pausa corta para reducir contención
                    Thread.sleep(10 + (long)(Math.random() * 20));
                }
            }

            logger.info("Hilo {} completado con {} transacciones exitosas",
                    threadId, exitosConsecutivos);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Hilo {} interrumpido: {}", threadId, e.getMessage());
        } catch (Exception e) {
            logger.error("Error en hilo {}: {}", threadId, e.getMessage(), e);
        }
    }

    /**
     * Método que realiza la transferencia de dinero entre cuentas
     * usando SQL directo para garantizar atomicidad
     *
     * @return true si la transferencia fue exitosa, false si no hay suficiente saldo
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean transferirDinero() {
        BigDecimal monto = new BigDecimal("5");

        try {
            // 1. Intentar realizar la transferencia con SQL directo
            int filasActualizadas = jdbcTemplate.update(
                    "UPDATE cuenta SET monto = monto - ? WHERE nombre = ? AND monto >= ?",
                    monto, "abc", monto
            );

            // Si no se actualizó ninguna fila, significa que no hay saldo suficiente
            if (filasActualizadas == 0) {
                return false;
            }

            // 2. Aumentar el saldo de la cuenta destino
            jdbcTemplate.update(
                    "UPDATE cuenta SET monto = monto + ? WHERE nombre = ?",
                    monto, "cbd"
            );

            // 3. Registrar la transacción
            insertarTransaccion(monto);

            // 4. Incrementar contador y enviar métricas a New Relic
            int totalExitosas = transaccionesExitosas.incrementAndGet();

            // Log cada 100 transacciones
            if (totalExitosas % 100 == 0) {
                logger.info("Completadas {} transacciones exitosas", totalExitosas);
                // Verificar consistencia periódicamente
                verificarConsistenciaSaldos();
            }

            // Enviar métricas a New Relic
            NewRelic.recordMetric("Custom/MontoTransferido", monto.floatValue());
            NewRelic.incrementCounter("Custom/TransaccionesCompletadas");

            return true;

        } catch (DataAccessException e) {
            transaccionesFallidas.incrementAndGet();
            logger.warn("Error de acceso a datos: {}", e.getMessage());
            NewRelic.noticeError(e);
            return false;
        } catch (Exception e) {
            transaccionesFallidas.incrementAndGet();
            logger.error("Error al realizar transferencia: {}", e.getMessage(), e);
            NewRelic.noticeError(e);
            return false;
        }
    }

    /**
     * Inserta el registro de la transacción
     */
    private void insertarTransaccion(BigDecimal monto) {
        // Obtener IDs de las cuentas de manera segura
        Long origenId = jdbcTemplate.queryForObject(
                "SELECT id FROM cuenta WHERE nombre = ?",
                Long.class, "abc");

        Long destinoId = jdbcTemplate.queryForObject(
                "SELECT id FROM cuenta WHERE nombre = ?",
                Long.class, "cbd");

        // Insertar la transacción con SQL directo para mayor rendimiento
        jdbcTemplate.update(
                "INSERT INTO transaccion (origen, destino, monto, timestamp) VALUES (?, ?, ?, ?)",
                origenId, destinoId, monto, LocalDateTime.now()
        );
    }


    private BigDecimal getSaldoActual(String nombreCuenta) {
        return jdbcTemplate.queryForObject(
                "SELECT monto FROM cuenta WHERE nombre = ?",
                BigDecimal.class, nombreCuenta
        );
    }

    /**
     * Verifica que la suma de los saldos de ambas cuentas sea consistente
     */
    private void verificarConsistenciaSaldos() {
        // Adquirir lock para asegurar que la verificación es atómica
        sumLock.lock();
        try {
            BigDecimal saldoABC = getSaldoActual("abc");
            BigDecimal saldoCBD = getSaldoActual("cbd");
            BigDecimal sumaActual = saldoABC.add(saldoCBD);

            // Comprobar que el total del sistema se mantiene constante
            if (!sumaActual.equals(totalSistema)) {
                logger.error("INCONSISTENCIA DETECTADA: Suma actual ({}) != Total esperado ({})",
                        sumaActual, totalSistema);
                NewRelic.noticeError("Inconsistencia de saldos detectada");
            }
        } finally {
            sumLock.unlock();
        }
    }

    /**
     * Obtiene el estado actual de las cuentas
     */
    public String obtenerEstadoCuentas() {
        try {
            BigDecimal saldoABC = getSaldoActual("abc");
            BigDecimal saldoCBD = getSaldoActual("cbd");
            int txExitosas = transaccionesExitosas.get();
            int txFallidas = transaccionesFallidas.get();

            return String.format(
                    "Saldo ABC: %s, Saldo CBD: %s, Total: %s, Transacciones exitosas: %d, fallidas: %d",
                    saldoABC, saldoCBD, saldoABC.add(saldoCBD), txExitosas, txFallidas
            );
        } catch (Exception e) {
            return "Error al obtener estado: " + e.getMessage();
        }
    }

    /**
     * Método para detener el servicio y liberar recursos
     */
    public void detener() {
        finTransferencias.set(true);
        executorService.shutdown();
    }
}
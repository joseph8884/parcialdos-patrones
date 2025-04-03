package patrones.pacialdos.parcial.services;

import com.newrelic.api.agent.NewRelic;
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

@Service
public class TransaccionService {

    @Autowired
    private CuentaJPA cuentaJPA;

    @Autowired
    private TransaccionJPA transaccionJPA;

    private final ExecutorService executorService = Executors.newFixedThreadPool(30);

    @Transactional
    public void resetCuentas() {
        CuentaORM cuenta1 = cuentaJPA.findByNombre("abc").orElse(new CuentaORM(null, "abc", BigDecimal.ZERO));
        CuentaORM cuenta2 = cuentaJPA.findByNombre("cbd").orElse(new CuentaORM(null, "cbd", BigDecimal.ZERO));

        cuenta1.setMonto(new BigDecimal("1000"));
        cuenta2.setMonto(new BigDecimal("1000"));

        cuentaJPA.save(cuenta1);
        cuentaJPA.save(cuenta2);
    }

    public void iniciarTransaccionesConcurrentes() {
        for (int i = 0; i < 30; i++) {
            executorService.execute(this::moverDinero);
        }
        executorService.shutdown();
    }

    @Transactional
    public void moverDinero() {
        while (true) {
            BigDecimal monto = new BigDecimal("5");

            // Restar saldo de "abc" solo si tiene suficiente dinero
            int updatedRows = cuentaJPA.restarSaldo("abc", monto);
            if (updatedRows == 0) {
                System.out.println("La cuenta 'abc' se quedó sin saldo.");
                break; // Salimos del bucle si ya no hay dinero
            }

            // Sumar saldo a "cbd"
            cuentaJPA.sumarSaldo("cbd", monto);

            // Obtener IDs de las cuentas
            Optional<CuentaORM> cuentaOrigenOpt = cuentaJPA.findByNombre("abc");
            Optional<CuentaORM> cuentaDestinoOpt = cuentaJPA.findByNombre("cbd");

            if (cuentaOrigenOpt.isEmpty() || cuentaDestinoOpt.isEmpty()) {
                System.out.println("No se encontraron las cuentas después de la transacción.");
                break;
            }

            Long idOrigen = cuentaOrigenOpt.get().getId();
            Long idDestino = cuentaDestinoOpt.get().getId();

            // Crear y guardar la transacción
            TransaccionORM transaccion = new TransaccionORM(
                    null,
                    idOrigen,
                    idDestino,
                    monto,
                    LocalDateTime.now()
            );
            transaccionJPA.save(transaccion);

            // Enviar datos a NewRelic
            NewRelic.recordMetric("Custom/MontoTransferido", monto.floatValue());
            NewRelic.addCustomParameter("Origen", "abc");
            NewRelic.addCustomParameter("Destino", "cbd");
            NewRelic.addCustomParameter("Monto", monto.multiply(new BigDecimal("100")).intValue());
        }
    }

}
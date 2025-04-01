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

    public void iniciarTransaccionesConcurrentes() {
        for (int i = 0; i < 30; i++) {
            executorService.execute(this::moverDinero);
        }
        executorService.shutdown();
    }

    @Transactional
    public void moverDinero() {
        while (true) {
            // Buscar las cuentas
            Optional<CuentaORM> cuentaOrigenOpt = cuentaJPA.findByNombre("abc");
            Optional<CuentaORM> cuentaDestinoOpt = cuentaJPA.findByNombre("cbd");

            if (cuentaOrigenOpt.isEmpty() || cuentaDestinoOpt.isEmpty()) {
                System.out.println("No se encontraron las cuentas");
                break;
            }

            CuentaORM cuentaOrigen = cuentaOrigenOpt.get();
            CuentaORM cuentaDestino = cuentaDestinoOpt.get();

            // Verificamos si aún hay dinero para transferir
            if (cuentaOrigen.getMonto().compareTo(new BigDecimal("5")) < 0) {
                System.out.println("La cuenta 'abc' se quedó sin saldo.");
                break;
            }

            // Definimos el monto a transferir
            BigDecimal monto = new BigDecimal("5");

            // Transferir 5 pesos
            cuentaOrigen.setMonto(cuentaOrigen.getMonto().subtract(monto));
            cuentaDestino.setMonto(cuentaDestino.getMonto().add(monto));

            // Guardar cambios en la DB
            cuentaJPA.save(cuentaOrigen);
            cuentaJPA.save(cuentaDestino);

            // Crear y guardar la transacción
            TransaccionORM transaccion = new TransaccionORM(
                    null,
                    cuentaOrigen.getId(),
                    cuentaDestino.getId(),
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
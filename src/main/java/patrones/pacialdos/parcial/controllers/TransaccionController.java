package patrones.pacialdos.parcial.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import patrones.pacialdos.parcial.orm.CuentaORM;
import patrones.pacialdos.parcial.orm.TransaccionORM;
import patrones.pacialdos.parcial.jpa.CuentaJPA;
import patrones.pacialdos.parcial.jpa.TransaccionJPA;
import patrones.pacialdos.parcial.services.TransaccionService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/transacciones")
public class TransaccionController {

    @Autowired
    private TransaccionService transaccionService;

    @Autowired
    private CuentaJPA cuentaJPA;

    @Autowired
    private TransaccionJPA transaccionJPA;

    @PostMapping("/iniciar")
    public String iniciarTransacciones() {
        transaccionService.iniciarTransaccionesConcurrentes();
        return "Transacciones concurrentes iniciadas.";
    }

    @GetMapping("/estado")
    public Map<String, Object> obtenerEstadoCuentas() {
        List<CuentaORM> cuentas = cuentaJPA.findAll();
        List<TransaccionORM> transacciones = transaccionJPA.findAll();

        return Map.of(
                "cuentas", cuentas,
                "transacciones", transacciones.stream()
                        .map(t -> Map.of(
                                "origen", t.getOrigen(),
                                "destino", t.getDestino(),
                                "monto", t.getMonto(),
                                "fecha", t.getTimestamp()
                        ))
                        .collect(Collectors.toList())
        );
    }
}

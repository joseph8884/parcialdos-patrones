package patrones.pacialdos.parcial.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import patrones.pacialdos.parcial.jpa.CuentaJPA;
import patrones.pacialdos.parcial.orm.CuentaORM;
import patrones.pacialdos.parcial.services.TransaccionService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/transacciones")
public class TransaccionController {

    @Autowired
    private TransaccionService transaccionService;

    @Autowired
    private CuentaJPA cuentaJPA;

    @PostMapping("/reset")
    public ResponseEntity<String> resetCuentas() {
        transaccionService.resetCuentas();
        return ResponseEntity.ok("Cuentas reseteadas a 10,000 cada una");
    }

    @PostMapping("/iniciar")
    public ResponseEntity<String> iniciarTransacciones() {
        transaccionService.iniciarTransaccionesConcurrentes();
        return ResponseEntity.ok("Transacciones iniciadas con 30 hilos concurrentes");
    }

    @GetMapping("/estado")
    public ResponseEntity<Map<String, Object>> obtenerEstado() {
        Map<String, Object> estado = new HashMap<>();

        Optional<CuentaORM> cuentaABC = cuentaJPA.findByNombre("abc");
        Optional<CuentaORM> cuentaCBD = cuentaJPA.findByNombre("cbd");

        if (cuentaABC.isPresent()) {
            estado.put("saldoABC", cuentaABC.get().getMonto());
        } else {
            estado.put("saldoABC", "No encontrado");
        }

        if (cuentaCBD.isPresent()) {
            estado.put("saldoCBD", cuentaCBD.get().getMonto());
        } else {
            estado.put("saldoCBD", "No encontrado");
        }

        return ResponseEntity.ok(estado);
    }
}
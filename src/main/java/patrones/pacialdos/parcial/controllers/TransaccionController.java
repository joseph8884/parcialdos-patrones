package patrones.pacialdos.parcial.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import patrones.pacialdos.parcial.services.TransaccionService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/transacciones")
public class TransaccionController {

    @Autowired
    private TransaccionService transaccionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

        // Obtener saldos directamente con SQL para evitar caché
        BigDecimal saldoABC = jdbcTemplate.queryForObject(
                "SELECT monto FROM cuenta WHERE nombre = ?",
                BigDecimal.class, "abc");

        BigDecimal saldoCBD = jdbcTemplate.queryForObject(
                "SELECT monto FROM cuenta WHERE nombre = ?",
                BigDecimal.class, "cbd");

        // Contar transacciones
        Integer numTransacciones = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transaccion",
                Integer.class);

        // Calcular total (debe ser 20,000)
        BigDecimal totalSistema = saldoABC.add(saldoCBD);

        estado.put("saldoABC", saldoABC);
        estado.put("saldoCBD", saldoCBD);
        estado.put("totalSistema", totalSistema);
        estado.put("transaccionesRegistradas", numTransacciones);
        estado.put("consistente", totalSistema.compareTo(new BigDecimal("20000")) == 0);

        return ResponseEntity.ok(estado);
    }

    @PostMapping("/detener")
    public ResponseEntity<String> detenerProceso() {
        transaccionService.detener();
        return ResponseEntity.ok("Proceso de transferencias detenido");
    }

    @GetMapping("/resumen")
    public ResponseEntity<String> obtenerResumen() {
        return ResponseEntity.ok(transaccionService.obtenerEstadoCuentas());
    }
}